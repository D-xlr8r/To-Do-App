package com.dxlr8r.notifyme.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.ui.calendar.CalendarScreen
import com.dxlr8r.notifyme.ui.common.SystemSettings
import com.dxlr8r.notifyme.ui.editor.EditorRequest
import com.dxlr8r.notifyme.ui.editor.ItemEditorSheet
import com.dxlr8r.notifyme.ui.focus.FocusActivity
import com.dxlr8r.notifyme.ui.settings.BatteryGuideDialog
import com.dxlr8r.notifyme.ui.settings.SettingsSheet
import com.dxlr8r.notifyme.ui.tasks.TasksScreen
import com.dxlr8r.notifyme.ui.theme.NotifyTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    /** Requests coming from notifications, the widget and launcher shortcuts. */
    private var launch by mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) launch = LaunchRequest.from(intent)
        setContent {
            NotifyTheme {
                NotifyApp(vm, launch, consumeLaunch = { launch = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launch = LaunchRequest.from(intent)
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val ACTION_ADD_TASK = "com.dxlr8r.notifyme.ADD_TASK"
        const val ACTION_ADD_REMINDER = "com.dxlr8r.notifyme.ADD_REMINDER"
        const val ACTION_VOICE_TASK = "com.dxlr8r.notifyme.VOICE_TASK"
    }
}

data class LaunchRequest(val tab: Int?, val editor: EditorRequest?) {
    companion object {
        fun from(intent: Intent?): LaunchRequest? {
            intent ?: return null
            return when (intent.action) {
                MainActivity.ACTION_ADD_TASK -> LaunchRequest(0, EditorRequest(Item.KIND_TASK))
                MainActivity.ACTION_VOICE_TASK -> LaunchRequest(0, EditorRequest(Item.KIND_TASK, startVoice = true))
                MainActivity.ACTION_ADD_REMINDER -> LaunchRequest(1, EditorRequest(Item.KIND_EVENT, date = LocalDate.now()))
                else -> if (intent.hasExtra(MainActivity.EXTRA_TAB)) LaunchRequest(intent.getIntExtra(MainActivity.EXTRA_TAB, 0), null) else null
            }
        }
    }
}

private enum class Prompt { EXACT, FULL_SCREEN, BATTERY }

@Composable
private fun NotifyApp(vm: MainViewModel, launch: LaunchRequest?, consumeLaunch: () -> Unit) {
    val context = LocalContext.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var editor by remember { mutableStateOf<EditorRequest?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var completedExpanded by remember { mutableStateOf(vm.prefs.completedExpanded) }
    var voiceLanguage by remember { mutableStateOf(vm.prefs.voiceLanguage) }
    var status by remember { mutableStateOf(SystemSettings.status(context)) }
    var prompt by remember { mutableStateOf<Prompt?>(null) }
    var pendingAlarmCheck by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        status = SystemSettings.status(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(launch) {
        launch ?: return@LaunchedEffect
        launch.tab?.let { tab = it }
        launch.editor?.let { editor = it }
        consumeLaunch()
    }

    fun followUpChecks(alarm: Boolean) {
        status = SystemSettings.status(context)
        prompt = when {
            !status.exactAlarms -> Prompt.EXACT
            alarm && !status.fullScreen -> Prompt.FULL_SCREEN
            SystemSettings.isAggressiveOem && !vm.prefs.batteryGuideShown -> {
                vm.prefs.batteryGuideShown = true
                Prompt.BATTERY
            }
            else -> null
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        followUpChecks(pendingAlarmCheck)
    }

    fun afterReminderSaved(alarm: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 && !status.notifications) {
            pendingAlarmCheck = alarm
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            followUpChecks(alarm)
        }
    }

    fun deleteWithUndo(item: Item) {
        vm.delete(item.id) { removed ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val r = snackbar.showSnackbar("Deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) vm.restore(removed)
            }
        }
    }

    fun openFocus(task: Item?) {
        context.startActivity(
            Intent(context, FocusActivity::class.java).apply {
                task?.let { putExtra(FocusActivity.EXTRA_TASK_ID, it.id) }
            },
        )
    }

    val hasAnyReminder = (tasks?.any { it.hasReminder && !it.done } == true) || events.isNotEmpty()
    val banner: @Composable () -> Unit = {
        if (hasAnyReminder && !status.allGood) {
            AttentionBanner(
                text = if (!status.notifications) "Notifications are off, so reminders can't alert you."
                else "Allow on-time alarms so reminders ring exactly when set.",
                onFix = {
                    if (!status.notifications) {
                        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else SystemSettings.openNotificationSettings(context)
                    } else {
                        SystemSettings.openExactAlarmSettings(context)
                    }
                },
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Header(
                title = if (tab == 0) "My Tasks" else "Calendar",
                subtitle = when {
                    tab == 0 && tasks != null -> {
                        val n = tasks!!.count { !it.done }
                        "${today()} · " + if (n == 0) "nothing pending" else "$n to do"
                    }
                    else -> today()
                },
                onFocus = { openFocus(null) },
                onSettings = { settingsOpen = true },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                val colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                )
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(if (tab == 0) Icons.Filled.TaskAlt else Icons.Outlined.TaskAlt, null) },
                    label = { Text("Tasks") }, colors = colors,
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(if (tab == 1) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth, null) },
                    label = { Text("Calendar") }, colors = colors,
                )
                NavigationBarItem(
                    selected = false, onClick = { openFocus(null) },
                    icon = { Icon(Icons.Outlined.HourglassTop, null) },
                    label = { Text("Focus") }, colors = colors,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editor = if (tab == 0) EditorRequest(Item.KIND_TASK) else EditorRequest(Item.KIND_EVENT, date = selectedDate)
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
            ) {
                Icon(Icons.Filled.Add, if (tab == 0) "Add task" else "Add reminder")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        AnimatedContent(targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
            if (t == 0) {
                TasksScreen(
                    tasks = tasks,
                    contentPadding = padding,
                    completedExpanded = completedExpanded,
                    onToggleCompleted = {
                        completedExpanded = !completedExpanded
                        vm.prefs.completedExpanded = completedExpanded
                    },
                    onCheck = { item, done ->
                        vm.setDone(item.id, done)
                        if (done) scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val r = snackbar.showSnackbar("Marked as done", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) vm.setDone(item.id, false)
                        }
                    },
                    onOpen = { editor = EditorRequest(Item.KIND_TASK, item = it) },
                    onReminder = { editor = EditorRequest(Item.KIND_TASK, item = it, openReminder = true) },
                    onDelete = { deleteWithUndo(it) },
                    onClearCompleted = { vm.clearCompleted() },
                    banner = banner,
                )
            } else {
                CalendarScreen(
                    events = events,
                    selected = selectedDate,
                    onSelect = { selectedDate = it },
                    contentPadding = padding,
                    onOpen = { editor = EditorRequest(Item.KIND_EVENT, item = it) },
                    onDelete = { deleteWithUndo(it) },
                    banner = banner,
                )
            }
        }
    }

    editor?.let { req ->
        ItemEditorSheet(
            request = req,
            voiceLanguage = voiceLanguage,
            onVoiceLanguageChange = { voiceLanguage = it; vm.prefs.voiceLanguage = it },
            onDismiss = { editor = null },
            onSave = { item ->
                vm.save(item)
                if (item.kind == Item.KIND_EVENT) item.anchorDateTime?.let { selectedDate = it.toLocalDate() }
                if (item.hasReminder) afterReminderSaved(item.alarm)
            },
            onDelete = { deleteWithUndo(it) },
            onFocus = { openFocus(it) },
        )
    }

    if (settingsOpen) {
        SettingsSheet(
            status = status,
            voiceLanguage = voiceLanguage,
            onVoiceLanguageChange = { voiceLanguage = it; vm.prefs.voiceLanguage = it },
            onDismiss = { settingsOpen = false },
        )
    }

    when (prompt) {
        Prompt.EXACT -> PermissionPrompt(
            title = "Ring exactly on time",
            text = "Android needs your OK for NotifyMe to ring at the exact minute you choose. Turn on “Alarms & reminders” on the next screen.",
            onAllow = { SystemSettings.openExactAlarmSettings(context); prompt = null },
            onLater = { prompt = null },
        )
        Prompt.FULL_SCREEN -> PermissionPrompt(
            title = "Show alarms on full screen",
            text = "So alarms can light up your screen like a clock alarm, allow full-screen notifications for NotifyMe.",
            onAllow = { SystemSettings.openFullScreenSettings(context); prompt = null },
            onLater = { prompt = null },
        )
        Prompt.BATTERY -> BatteryGuideDialog(onDismiss = { prompt = null })
        null -> Unit
    }
}

private fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

@Composable
private fun Header(title: String, subtitle: String, onFocus: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
    }
}

@Composable
private fun AttentionBanner(text: String, onFix: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsOff, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}

@Composable
private fun PermissionPrompt(title: String, text: String, onAllow: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onAllow) { Text("Allow") } },
        dismissButton = { TextButton(onClick = onLater) { Text("Not now") } },
    )
}
