package com.dxlr8r.notifyme.ui.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.dxlr8r.notifyme.reminders.Notifications
import com.dxlr8r.notifyme.reminders.ReminderScheduler

/** Everything that depends on phone settings, gathered in one place. */
object SystemSettings {

    data class Status(
        val notifications: Boolean,
        val exactAlarms: Boolean,
        val fullScreen: Boolean,
    ) {
        val allGood get() = notifications && exactAlarms
    }

    fun status(context: Context) = Status(
        notifications = Notifications.canPost(context),
        exactAlarms = ReminderScheduler.canScheduleExact(context),
        fullScreen = Notifications.canUseFullScreen(context),
    )

    fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: Exception) {
        false
    }

    private fun pkg(context: Context) = Uri.parse("package:${context.packageName}")

    fun openNotificationSettings(context: Context) {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        if (!open(context, i)) openAppDetails(context)
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (open(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg(context)))) return
        }
        openAppDetails(context)
    }

    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            if (open(context, Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg(context)))) return
        }
        openNotificationSettings(context)
    }

    fun openChannelSound(context: Context, channel: String) {
        if (!open(context, Notifications.channelSettingsIntent(context, channel))) openNotificationSettings(context)
    }

    fun openAppDetails(context: Context) {
        open(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg(context)))
    }

    fun openBatteryOptimization(context: Context) {
        if (!open(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) openAppDetails(context)
    }

    // ------------------------------------------------------------ phone makers that stop background apps

    private val manufacturer get() = Build.MANUFACTURER.lowercase()

    val isAggressiveOem: Boolean
        get() = listOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "iqoo", "oneplus", "huawei", "honor", "samsung", "tecno", "infinix", "itel")
            .any { manufacturer.contains(it) }

    val oemName: String
        get() = Build.MANUFACTURER.replaceFirstChar { it.titlecase() }

    /** Short, human steps for the user's phone brand. */
    fun batterySteps(): List<String> = when {
        listOf("xiaomi", "redmi", "poco").any { manufacturer.contains(it) } -> listOf(
            "Turn on Autostart for NotifyMe",
            "Battery saver → NotifyMe → No restrictions",
            "In Recent apps, pull NotifyMe down to lock it",
        )
        listOf("oppo", "realme", "oneplus").any { manufacturer.contains(it) } -> listOf(
            "Allow Auto launch / Allow background activity",
            "Battery → NotifyMe → Allow background activity",
            "In Recent apps, lock NotifyMe",
        )
        listOf("vivo", "iqoo").any { manufacturer.contains(it) } -> listOf(
            "Allow Background power consumption / Autostart",
            "Battery → High background power usage → NotifyMe on",
            "In Recent apps, lock NotifyMe",
        )
        listOf("huawei", "honor").any { manufacturer.contains(it) } -> listOf(
            "App launch → NotifyMe → Manage manually, all switches on",
            "Battery → turn off power-intensive prompt for NotifyMe",
        )
        manufacturer.contains("samsung") -> listOf(
            "Battery → Background usage limits → remove NotifyMe from Sleeping apps",
            "App battery usage → Unrestricted",
        )
        else -> listOf(
            "Battery → NotifyMe → Unrestricted / Don't optimise",
            "Allow the app to run in the background",
        )
    }

    /** Tries the brand's own auto-start screen, then falls back to the app's settings page. */
    fun openAutostart(context: Context) {
        val candidates = listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oplus.safecenter" to "com.oplus.safecenter.permission.startup.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
        )
        for ((p, c) in candidates) {
            if (open(context, Intent().setComponent(ComponentName(p, c)))) return
        }
        openAppDetails(context)
    }
}
