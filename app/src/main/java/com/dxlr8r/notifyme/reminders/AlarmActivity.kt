package com.dxlr8r.notifyme.reminders

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.data.Repository
import com.dxlr8r.notifyme.ui.common.Format
import com.dxlr8r.notifyme.ui.theme.ClockStyle
import com.dxlr8r.notifyme.ui.theme.NotifyTheme
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Full-screen alarm shown over the lock screen. The ringing itself comes from the alarm notification. */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val id = intent.getLongExtra(ReminderReceiver.EXTRA_ID, -1)
        val repo = Repository.get(this)

        lifecycleScope.launch {
            AlarmBus.dismissed.collect { if (it == id) finish() }
        }

        setContent {
            NotifyTheme {
                var item by remember { mutableStateOf<Item?>(null) }
                LaunchedEffect(id) { item = repo.get(id) }
                val context = this
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(28.dp),
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(Format.time(context, LocalTime.now()), style = ClockStyle.copy(fontSize = 64.sp), color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            item?.title ?: "Reminder",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(48.dp))
                        Button(
                            onClick = { act(ReminderReceiver.ACTION_DONE, id, 0) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                        ) {
                            Icon(Icons.Outlined.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (item?.isTask == true && item?.rule?.isRepeating == false) "Mark done" else "Dismiss", fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Snooze", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { act(ReminderReceiver.ACTION_SNOOZE, id, 10) }) { Text("10 min") }
                            OutlinedButton(onClick = { act(ReminderReceiver.ACTION_SNOOZE, id, 60) }) { Text("1 hour") }
                            OutlinedButton(onClick = { act(ReminderReceiver.ACTION_SNOOZE, id, ReminderReceiver.SNOOZE_TOMORROW) }) { Text("Tomorrow") }
                        }
                        if (item?.isTask == true && item?.rule?.isRepeating == false) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { act(ReminderReceiver.ACTION_DISMISS, id, 0) }) { Text("Stop ringing, keep task open") }
                        }
                    }
                }
            }
        }
    }

    private fun act(action: String, id: Long, minutes: Long) {
        Notifications.cancel(this, id)
        sendBroadcast(ReminderReceiver.intent(this, action, id, minutes))
        finish()
    }
}
