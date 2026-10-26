package com.dxlr8r.notifyme.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.ui.MainActivity

object ReminderScheduler {

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun schedule(context: Context, item: Item) {
        cancel(context, item.id)
        if (item.done) return
        item.nextAt?.let { set(context, item, it, snooze = false) }
        item.snoozeAt?.let { set(context, item, it, snooze = true) }
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (snooze in listOf(false, true)) {
            pending(context, id, snooze, PendingIntent.FLAG_NO_CREATE)?.let {
                am.cancel(it)
                it.cancel()
            }
        }
    }

    private fun set(context: Context, item: Item, at: Long, snooze: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, item.id, snooze, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            if (canScheduleExact(context)) {
                if (item.alarm) {
                    val show = PendingIntent.getActivity(
                        context, item.id.toInt(),
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                }
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun pending(context: Context, id: Long, snooze: Boolean, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_ID, id)
            .putExtra(ReminderReceiver.EXTRA_SNOOZE, snooze)
        val code = (id * 2 + if (snooze) 1 else 0).toInt()
        return PendingIntent.getBroadcast(context, code, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }
}
