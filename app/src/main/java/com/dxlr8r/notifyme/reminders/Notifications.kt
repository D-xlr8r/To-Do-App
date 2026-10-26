package com.dxlr8r.notifyme.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dxlr8r.notifyme.R
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.data.toLocalDateTime
import com.dxlr8r.notifyme.ui.MainActivity
import com.dxlr8r.notifyme.ui.common.Format

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_ALARMS = "alarms_v1"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val reminders = NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Task and calendar reminders"
            enableVibration(true)
            enableLights(true)
        }
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarms = NotificationChannel(CHANNEL_ALARMS, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders set to ring like an alarm"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAttrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 600, 800, 600, 800)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(reminders, alarms))
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    /** Intent that opens the system sound settings for a channel, so users pick any ringtone. */
    fun channelSettingsIntent(context: Context, channel: String): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channel)

    fun show(context: Context, item: Item, missed: Boolean) {
        if (!canPost(context)) return
        val id = item.id
        val code = id.toInt()
        val open = PendingIntent.getActivity(
            context, code,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TAB, if (item.isTask) 0 else 1)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(action: String, minutes: Long, req: Int) = PendingIntent.getBroadcast(
            context, code * 8 + req,
            ReminderReceiver.intent(context, action, id, minutes),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val shownAt = if (missed) item.nextAt ?: System.currentTimeMillis() else System.currentTimeMillis()
        val whenText = Format.time(context, shownAt.toLocalDateTime().toLocalTime())
        val subtitle = buildString {
            if (missed) append("Missed reminder") else append(if (item.isTask) "Task reminder" else "Reminder")
            append(" · ").append(whenText)
            if (item.rule.isRepeating) append(" · ").append(Format.repeat(item.rule))
        }

        val alarm = item.alarm && !missed
        val builder = NotificationCompat.Builder(context, if (alarm) CHANNEL_ALARMS else CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(ContextCompat.getColor(context, R.color.brand_sage))
            .setContentTitle(item.title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setContentIntent(open)
            .setAutoCancel(!alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Done", action(ReminderReceiver.ACTION_DONE, 0, 1))
            .addAction(0, "Snooze 10 min", action(ReminderReceiver.ACTION_SNOOZE, 10, 2))
            .addAction(0, "1 hour", action(ReminderReceiver.ACTION_SNOOZE, 60, 3))

        if (alarm) {
            val full = PendingIntent.getActivity(
                context, code,
                Intent(context, AlarmActivity::class.java)
                    .putExtra(ReminderReceiver.EXTRA_ID, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.setFullScreenIntent(full, true)
                .setOngoing(true)
                .setDeleteIntent(action(ReminderReceiver.ACTION_DISMISS, 0, 4))
        }

        val notification = builder.build()
        if (alarm) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        try {
            NotificationManagerCompat.from(context).notify(code, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context, id: Long) {
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }
}
