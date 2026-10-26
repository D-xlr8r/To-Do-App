package com.dxlr8r.notifyme.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dxlr8r.notifyme.BuildConfig
import com.dxlr8r.notifyme.reminders.Notifications
import com.dxlr8r.notifyme.ui.common.FieldRow
import com.dxlr8r.notifyme.ui.common.SystemSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    status: SystemSettings.Status,
    voiceLanguage: String,
    onVoiceLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var battery by remember { mutableStateOf(false) }
    val ok = MaterialTheme.colorScheme.primary
    val bad = MaterialTheme.colorScheme.error

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(16.dp))

            Section("Reminders")
            Group {
                StatusRow(Icons.Outlined.Notifications, "Notifications", status.notifications, ok, bad) {
                    SystemSettings.openNotificationSettings(context)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    StatusRow(Icons.Outlined.Schedule, "On-time alarms", status.exactAlarms, ok, bad) {
                        SystemSettings.openExactAlarmSettings(context)
                    }
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    StatusRow(Icons.Outlined.Fullscreen, "Full-screen alarms", status.fullScreen, ok, bad) {
                        SystemSettings.openFullScreenSettings(context)
                    }
                }
                FieldRow(Icons.Outlined.BatteryChargingFull, "Background", "Keep reminders working", onClick = { battery = true })
            }

            Spacer(Modifier.height(18.dp))
            Section("Sounds")
            Group {
                FieldRow(Icons.Outlined.MusicNote, "Notification tone", "Choose from your phone's sounds", onClick = {
                    SystemSettings.openChannelSound(context, Notifications.CHANNEL_REMINDERS)
                })
                FieldRow(Icons.Outlined.Alarm, "Alarm tone", "Choose from your phone's ringtones", onClick = {
                    SystemSettings.openChannelSound(context, Notifications.CHANNEL_ALARMS)
                })
            }

            Spacer(Modifier.height(18.dp))
            Section("Voice")
            Group {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Translate, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = !voiceLanguage.startsWith("hi"), onClick = { onVoiceLanguageChange("en-IN") },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("English") }
                        SegmentedButton(
                            selected = voiceLanguage.startsWith("hi"), onClick = { onVoiceLanguageChange("hi-IN") },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("हिंदी") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "NotifyMe ${BuildConfig.VERSION_NAME} · Ad-free\nYour tasks stay on this phone. No account, no tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (battery) BatteryGuideDialog(onDismiss = { battery = false })
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(4.dp)) { content() }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    on: Boolean,
    ok: androidx.compose.ui.graphics.Color,
    bad: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    FieldRow(
        icon, label,
        if (on) "Allowed" else "Not allowed — tap to fix",
        onClick = onClick,
        valueColor = if (on) MaterialTheme.colorScheme.onSurface else bad,
        trailing = {
            Icon(
                if (on) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null,
                tint = if (on) ok else bad,
            )
        },
    )
}

@Composable
fun BatteryGuideDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.BatteryChargingFull, null) },
        title = { Text("Keep reminders on time") },
        text = {
            Column {
                Text(
                    "${SystemSettings.oemName} phones can stop apps in the background to save battery. " +
                        "A quick one-time setup makes sure NotifyMe always rings:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                SystemSettings.batterySteps().forEachIndexed { i, step ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(22.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { SystemSettings.openBatteryOptimization(context) }) { Text("Open battery settings") }
            }
        },
        confirmButton = { TextButton(onClick = { SystemSettings.openAutostart(context) }) { Text("Open app settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
