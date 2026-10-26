package com.dxlr8r.notifyme.data

import android.content.Context
import com.dxlr8r.notifyme.core.Recurrence
import com.dxlr8r.notifyme.reminders.Notifications
import com.dxlr8r.notifyme.reminders.ReminderScheduler
import com.dxlr8r.notifyme.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/** Single entry point for every change, so alarms and the home-screen widget never go out of sync. */
class Repository(private val context: Context) {
    private val dao = AppDatabase.get(context).items()

    val tasks: Flow<List<Item>> = dao.observeTasks()
    val events: Flow<List<Item>> = dao.observeEvents()

    suspend fun get(id: Long): Item? = dao.get(id)

    suspend fun pendingTasks(): List<Item> = dao.pendingTasks()

    suspend fun allEvents(): List<Item> = dao.allEvents()

    /** Inserts or updates; recalculates the next trigger and (re)schedules it. Returns the id. */
    suspend fun save(item: Item): Long {
        val prepared = item.copy(nextAt = computeNext(item, LocalDateTime.now()), title = item.title.trim())
        val id = dao.upsert(prepared)
        val saved = prepared.copy(id = id)
        ReminderScheduler.schedule(context, saved)
        changed()
        return id
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val item = dao.get(id) ?: return
        val updated = if (done) {
            item.copy(done = true, completedAt = System.currentTimeMillis(), nextAt = null, snoozeAt = null)
        } else {
            item.copy(done = false, completedAt = null).let { it.copy(nextAt = computeNext(it, LocalDateTime.now())) }
        }
        dao.update(updated)
        ReminderScheduler.schedule(context, updated)
        Notifications.cancel(context, id)
        changed()
    }

    suspend fun delete(id: Long): Item? {
        val item = dao.get(id) ?: return null
        ReminderScheduler.cancel(context, id)
        Notifications.cancel(context, id)
        dao.delete(item)
        changed()
        return item
    }

    /** Puts back an item removed by [delete] (Undo). */
    suspend fun restore(item: Item) {
        dao.upsert(item.copy(nextAt = computeNext(item, LocalDateTime.now())))
        dao.get(item.id)?.let { ReminderScheduler.schedule(context, it) }
        changed()
    }

    suspend fun clearCompleted() {
        dao.completedTasks().forEach { ReminderScheduler.cancel(context, it.id) }
        dao.clearCompleted()
        changed()
    }

    /** Called when an alarm goes off. Advances repeating reminders to their next occurrence. */
    suspend fun onFired(id: Long, wasSnooze: Boolean): Item? {
        val item = dao.get(id) ?: return null
        val now = LocalDateTime.now()
        val updated = if (wasSnooze) {
            item.copy(snoozeAt = null)
        } else {
            // If the alarm arrived a little early, step past the occurrence it was meant for.
            val after = maxOf(item.nextAt?.toLocalDateTime() ?: now, now)
            item.copy(nextAt = item.anchorDateTime?.let { Recurrence.nextAfter(item.rule, it, after) }?.toEpochMillis())
        }
        dao.update(updated)
        ReminderScheduler.schedule(context, updated)
        changed()
        return item
    }

    suspend fun snooze(id: Long, untilMillis: Long) {
        val item = dao.get(id) ?: return
        val updated = item.copy(snoozeAt = untilMillis)
        dao.update(updated)
        ReminderScheduler.schedule(context, updated)
        Notifications.cancel(context, id)
        changed()
    }

    /** Acknowledges a reminder from the notification: completes one-off tasks, just dismisses others. */
    suspend fun acknowledge(id: Long) {
        val item = dao.get(id) ?: return
        Notifications.cancel(context, id)
        if (item.isTask && !item.rule.isRepeating) setDone(id, true)
    }

    /**
     * After boot / time-zone change / app update: recompute every trigger.
     * One-off reminders that were missed while the phone was off are shown right away.
     */
    suspend fun rescheduleAll(showMissed: Boolean) {
        val now = LocalDateTime.now()
        val nowMs = System.currentTimeMillis()
        for (item in dao.withReminders()) {
            val missed = item.nextAt != null && item.nextAt < nowMs - 60_000 && !item.rule.isRepeating
            if (missed && showMissed) {
                Notifications.show(context, item, missed = true)
            }
            val snooze = item.snoozeAt?.takeIf { it > nowMs }
            val updated = item.copy(nextAt = computeNext(item, now), snoozeAt = snooze)
            if (updated != item) dao.update(updated)
            ReminderScheduler.schedule(context, updated)
        }
        changed()
    }

    private fun computeNext(item: Item, now: LocalDateTime): Long? {
        if (item.done) return null
        val anchor = item.anchorDateTime ?: return null
        return Recurrence.nextAfter(item.rule, anchor, now)?.toEpochMillis()
    }

    private suspend fun changed() {
        WidgetUpdater.update(context)
    }

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: Repository(context.applicationContext).also { instance = it }
        }
    }
}
