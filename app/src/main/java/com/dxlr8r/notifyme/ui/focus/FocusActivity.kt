package com.dxlr8r.notifyme.ui.focus

import android.app.NotificationManager
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DoNotDisturbOff
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.data.Prefs
import com.dxlr8r.notifyme.data.Repository
import com.dxlr8r.notifyme.ui.theme.ClockStyle
import com.dxlr8r.notifyme.ui.theme.NotifyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Calm full-screen clock with an optional focus timer. */
class FocusActivity : ComponentActivity() {

    private var previousFilter: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val repo = Repository.get(this)
        val prefs = Prefs(this)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val autoStart = intent.getIntExtra(EXTRA_START_MINUTES, 0)

        setContent {
            NotifyTheme {
                var tasks by remember { mutableStateOf<List<Item>>(emptyList()) }
                var task by remember { mutableStateOf<Item?>(null) }
                LaunchedEffect(Unit) {
                    tasks = repo.pendingTasks()
                    if (taskId > 0) task = repo.get(taskId)
                }
                FocusScreen(
                    task = task,
                    tasks = tasks,
                    onPickTask = { task = it },
                    defaultMinutes = prefs.focusMinutes,
                    autoStartMinutes = autoStart,
                    onMinutesChosen = { prefs.focusMinutes = it },
                    dndOn = { isDndOn() },
                    onToggleDnd = { toggleDnd() },
                    onFinished = { chime() },
                    onMarkDone = { t -> CoroutineScope(Dispatchers.IO).launch { repo.setDone(t.id, true) }; finish() },
                    onClose = { finish() },
                )
            }
        }
    }

    private fun isDndOn(): Boolean = previousFilter != null

    private fun toggleDnd(): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Allow NotifyMe to use Do Not Disturb, then tap again", Toast.LENGTH_LONG).show()
            runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
            return false
        }
        return if (previousFilter == null) {
            previousFilter = nm.currentInterruptionFilter
            // Priority mode: calls/messages filtered, alarms still ring.
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            true
        } else {
            restoreDnd(); false
        }
    }

    private fun restoreDnd() {
        val prev = previousFilter ?: return
        runCatching { getSystemService(NotificationManager::class.java).setInterruptionFilter(prev) }
        previousFilter = null
    }

    private fun chime() {
        runCatching {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
        }
        runCatching {
            getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1))
        }
    }

    override fun onDestroy() {
        if (isFinishing) restoreDnd()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_START_MINUTES = "start_minutes"
    }
}

@Composable
private fun FocusScreen(
    task: Item?,
    tasks: List<Item>,
    onPickTask: (Item?) -> Unit,
    defaultMinutes: Int,
    autoStartMinutes: Int,
    onMinutesChosen: (Int) -> Unit,
    dndOn: () -> Boolean,
    onToggleDnd: () -> Boolean,
    onFinished: () -> Unit,
    onMarkDone: (Item) -> Unit,
    onClose: () -> Unit,
) {
    var night by rememberSaveable { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(dndOn()) }
    var minutes by rememberSaveable { mutableIntStateOf(defaultMinutes) }
    var endAt by rememberSaveable { mutableLongStateOf(0L) }          // running when > 0
    var pausedLeft by rememberSaveable { mutableLongStateOf(0L) }     // paused when > 0
    var finished by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var customOpen by remember { mutableStateOf(false) }
    var taskMenu by remember { mutableStateOf(false) }
    val flip = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (autoStartMinutes > 0 && endAt == 0L && pausedLeft == 0L && !finished) {
            minutes = autoStartMinutes
            endAt = System.currentTimeMillis() + autoStartMinutes * 60_000L
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            nowMs = System.currentTimeMillis()
            if (endAt > 0 && nowMs >= endAt) {
                endAt = 0; finished = true; onFinished()
            }
            delay(1000 - (System.currentTimeMillis() % 1000))
        }
    }

    val bg by animateColorAsState(if (night) Color.Black else MaterialTheme.colorScheme.background, label = "bg")
    val fg by animateColorAsState(if (night) Color(0xFF9AA39D) else MaterialTheme.colorScheme.onBackground, label = "fg")
    val sub = if (night) Color(0xFF5E6661) else MaterialTheme.colorScheme.onSurfaceVariant
    val accent = if (night) Color(0xFF6FAE8A) else MaterialTheme.colorScheme.primary

    val running = endAt > 0
    val paused = pausedLeft > 0
    val totalMs = minutes * 60_000L
    val leftMs = when {
        running -> (endAt - nowMs).coerceAtLeast(0)
        paused -> pausedLeft
        else -> 0
    }

    Box(Modifier.fillMaxSize().background(bg).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Close", tint = sub) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { dnd = onToggleDnd() }) {
                Icon(if (dnd) Icons.Outlined.DoNotDisturbOn else Icons.Outlined.DoNotDisturbOff, "Do not disturb", tint = if (dnd) accent else sub)
            }
            IconButton(onClick = { night = !night }) {
                Icon(if (night) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, "Night clock", tint = sub)
            }
        }

        val hourglassColors = if (night) {
            HourglassColors(frame = Color(0xFF3A3530), glass = Color(0xFF6E7570), sand = Color(0xFFB8924A), sandDark = Color(0xFF8E6E33))
        } else {
            HourglassColors(frame = Color(0xFF5B4636), glass = Color(0xFF9DA8A1), sand = Color(0xFFEBC56E), sandDark = Color(0xFFD29E3E))
        }

        val clockBlock: @Composable (TextUnit) -> Unit = { size ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    now.format(DateTimeFormatter.ofPattern(if (android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current)) "HH:mm" else "h:mm", Locale.getDefault())),
                    style = ClockStyle.copy(fontSize = size),
                    color = fg,
                    modifier = Modifier.clickable(interactionSource = null, indication = null) { night = !night },
                )
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    color = sub,
                )
            }
        }

        val hourglass: @Composable (Modifier) -> Unit = { m ->
            Hourglass(
                endAt = endAt, pausedLeft = pausedLeft, totalMs = totalMs, finished = finished,
                colors = hourglassColors, flip = flip.value,
                modifier = m.semantics { contentDescription = "Hourglass timer" },
            )
        }

        val controlsBlock: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Time left (or the planned length before starting)
                val shown = if (running || paused) leftMs else if (finished) 0L else totalMs
                Text(
                    "%02d:%02d".format(shown / 60_000, (shown / 1000) % 60),
                    style = ClockStyle.copy(fontSize = 44.sp, letterSpacing = 0.sp),
                    color = if (running || paused || finished) fg else sub,
                )
                // Task
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(enabled = !running && !paused) { taskMenu = true }
                            .padding(6.dp),
                    ) {
                        Text(if (task != null) "Focusing on" else "Choose a task (optional)", style = MaterialTheme.typography.labelMedium, color = sub)
                        if (task != null) Text(
                            task.title, style = MaterialTheme.typography.titleMedium, color = fg,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                    }
                    DropdownMenu(expanded = taskMenu, onDismissRequest = { taskMenu = false }) {
                        DropdownMenuItem(text = { Text("No task") }, onClick = { onPickTask(null); taskMenu = false })
                        tasks.take(20).forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { onPickTask(t); taskMenu = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    running || paused -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = {
                            if (running) { pausedLeft = leftMs; endAt = 0 } else { endAt = System.currentTimeMillis() + pausedLeft; pausedLeft = 0 }
                        }) {
                            Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, Modifier.size(18.dp), tint = fg)
                            Spacer(Modifier.width(6.dp))
                            Text(if (running) "Pause" else "Resume", color = fg)
                        }
                        OutlinedButton(onClick = { endAt = 0; pausedLeft = 0 }) {
                            Icon(Icons.Outlined.Stop, null, Modifier.size(18.dp), tint = fg)
                            Spacer(Modifier.width(6.dp))
                            Text("Stop", color = fg)
                        }
                    }
                    finished -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Time's up — nice work.", style = MaterialTheme.typography.titleMedium, color = fg)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (task != null) Button(
                                onClick = { onMarkDone(task) },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                            ) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mark task done")
                            }
                            OutlinedButton(onClick = {
                                // Turn the hourglass over, then it is ready again.
                                scope.launch {
                                    flip.animateTo(1f, tween(800))
                                    finished = false
                                    flip.snapTo(0f)
                                }
                            }) { Text("Turn over", color = fg) }
                        }
                    }
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(25, 50).forEach { m ->
                                FilterChip(
                                    selected = minutes == m, onClick = { minutes = m; onMinutesChosen(m) },
                                    label = { Text("$m min") },
                                    colors = FilterChipDefaults.filterChipColors(labelColor = fg, selectedContainerColor = accent.copy(alpha = 0.2f), selectedLabelColor = fg),
                                )
                            }
                            FilterChip(
                                selected = minutes != 25 && minutes != 50, onClick = { customOpen = true },
                                label = { Text(if (minutes != 25 && minutes != 50) "$minutes min" else "Custom") },
                                colors = FilterChipDefaults.filterChipColors(labelColor = fg, selectedContainerColor = accent.copy(alpha = 0.2f), selectedLabelColor = fg),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { finished = false; endAt = System.currentTimeMillis() + minutes * 60_000L },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start focus")
                        }
                    }
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 16.dp)) {
            val w = maxWidth
            val h = maxHeight
            if (w > h) {
                // Landscape: clock and controls on the left, hourglass on the right
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        clockBlock((h.value / 4.2f).coerceIn(44f, 110f).sp)
                        Spacer(Modifier.height(12.dp))
                        controlsBlock()
                    }
                    hourglass(Modifier.weight(0.8f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    clockBlock((w.value / 4.6f).coerceIn(44f, 110f).sp)
                    hourglass(Modifier.weight(1f).fillMaxWidth().padding(vertical = 14.dp))
                    controlsBlock()
                }
            }
        }
    }

    if (customOpen) {
        var value by remember { mutableFloatStateOf(minutes.toFloat()) }
        AlertDialog(
            onDismissRequest = { customOpen = false },
            title = { Text("Focus length") },
            text = {
                Column {
                    Text("${value.toInt()} minutes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                    Slider(value = value, onValueChange = { value = it }, valueRange = 5f..180f, steps = 34)
                }
            },
            confirmButton = {
                TextButton(onClick = { minutes = value.toInt(); onMinutesChosen(minutes); customOpen = false }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { customOpen = false }) { Text("Cancel") } },
        )
    }
}
