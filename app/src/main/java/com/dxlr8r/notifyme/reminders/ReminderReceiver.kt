package com.dxlr8r.notifyme.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dxlr8r.notifyme.data.Repository
import com.dxlr8r.notifyme.data.toEpochMillis
import com.dxlr8r.notifyme.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        val pending = goAsync()
        scope.launch {
            try {
                val repo = Repository.get(context)
                when (intent.action) {
                    ACTION_FIRE -> {
                        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
                        val item = repo.get(id)
                        if (item != null && !item.done) {
                            Notifications.show(context, item, missed = false)
                            repo.onFired(id, snooze)
                        }
                    }
                    ACTION_DONE -> {
                        repo.acknowledge(id)
                        AlarmBus.dismiss(id)
                    }
                    ACTION_SNOOZE -> {
                        val minutes = intent.getLongExtra(EXTRA_MINUTES, 10)
                        repo.snooze(id, snoozeUntil(minutes))
                        AlarmBus.dismiss(id)
                    }
                    ACTION_MIDNIGHT -> {
                        WidgetUpdater.update(context)
                        WidgetUpdater.scheduleMidnight(context)
                    }
                    ACTION_DISMISS -> {
                        Notifications.cancel(context, id)
                        AlarmBus.dismiss(id)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.dxlr8r.notifyme.FIRE"
        const val ACTION_DONE = "com.dxlr8r.notifyme.DONE"
        const val ACTION_SNOOZE = "com.dxlr8r.notifyme.SNOOZE"
        const val ACTION_DISMISS = "com.dxlr8r.notifyme.DISMISS"
        const val ACTION_MIDNIGHT = "com.dxlr8r.notifyme.MIDNIGHT"
        const val EXTRA_ID = "id"
        const val EXTRA_SNOOZE = "snooze"
        const val EXTRA_MINUTES = "minutes"

        /** Special value for "same time tomorrow". */
        const val SNOOZE_TOMORROW = -1L

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun snoozeUntil(minutes: Long): Long =
            if (minutes == SNOOZE_TOMORROW) LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toEpochMillis()
            else System.currentTimeMillis() + minutes * 60_000

        fun intent(context: Context, action: String, id: Long, minutes: Long = 0): Intent =
            Intent(context, ReminderReceiver::class.java)
                .setAction(action)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_MINUTES, minutes)
    }
}

class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val showMissed = intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == "android.intent.action.QUICKBOOT_POWERON"
                Repository.get(context).rescheduleAll(showMissed)
            } finally {
                pending.finish()
            }
        }
    }
}
