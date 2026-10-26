package com.dxlr8r.notifyme.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmAdd
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.dxlr8r.notifyme.core.RepeatRule
import com.dxlr8r.notifyme.core.SmartParser
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.reminders.Notifications
import com.dxlr8r.notifyme.ui.common.DatePickDialog
import com.dxlr8r.notifyme.ui.common.FieldRow
import com.dxlr8r.notifyme.ui.common.Format
import com.dxlr8r.notifyme.ui.common.SystemSettings
import com.dxlr8r.notifyme.ui.common.TimePickDialog
import com.dxlr8r.notifyme.ui.common.rememberVoiceInput
import com.dxlr8r.notifyme.ui.theme.LocalPriority
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** What the editor should open with. */
data class EditorRequest(
    val kind: Int,
    val item: Item? = null,
    val date: LocalDate? = null,
    val openReminder: Boolean = false,
    val startVoice: Boolean = false,
)

private fun defaultDateTime(date: LocalDate?): LocalDateTime {
    val now = LocalDateTime.now()
    val nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
    return when {
        date == null || date == now.toLocalDate() -> nextHour
        else -> date.atTime(9, 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(
    request: EditorRequest,
    voiceLanguage: String,
    onVoiceLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onFocus: (Item) -> Unit,
) {
    val context = LocalContext.current
    val existing = request.item
    val isTask = request.kind == Item.KIND_TASK
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(TextFieldValue(existing?.title ?: "", TextRange((existing?.title ?: "").length))) }
    var priority by remember { mutableIntStateOf(existing?.priority ?: Item.PRIORITY_NONE) }
    var hasReminder by remember { mutableStateOf(existing?.hasReminder == true || !isTask || request.openReminder) }
    val start = remember { existing?.anchorDateTime ?: defaultDateTime(request.date) }
    var date by remember { mutableStateOf(start.toLocalDate()) }
    var time by remember { mutableStateOf(start.toLocalTime()) }
    var rule by remember { mutableStateOf(existing?.rule ?: RepeatRule.NONE) }
    var alarm by remember { mutableStateOf(existing?.alarm ?: false) }
    var dismissedSuggestionFor by remember { mutableStateOf<String?>(null) }

    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var pickRepeat by remember { mutableStateOf(false) }

    fun apply(r: SmartParser.Result) {
        r.dateTime?.let { date = it.toLocalDate(); time = it.toLocalTime() }
        if (r.repeat.isRepeating) rule = r.repeat
        if (r.dateTime != null || r.repeat.isRepeating) hasReminder = true
    }

    val voice = rememberVoiceInput(voiceLanguage) { spoken ->
        val r = SmartParser.parse(spoken)
        title = TextFieldValue(r.title, TextRange(r.title.length))
        if (r.found) apply(r)
        dismissedSuggestionFor = r.title
    }

    val suggestion = remember(title.text) {
        val text = title.text
        if (text.isBlank() || text == dismissedSuggestionFor) null
        else SmartParser.parse(text).takeIf { it.found && it.title.isNotBlank() && it.title != text.trim() }
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (request.startVoice) voice()
        else if (existing == null) runCatching { focus.requestFocus() }
    }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    fun save() {
        val base = existing ?: Item(kind = request.kind, title = "")
        var item = base.copy(title = title.text.trim(), priority = if (isTask) priority else Item.PRIORITY_NONE)
        item = if (hasReminder) item.withReminder(LocalDateTime.of(date, time), rule, alarm) else item.withoutReminder()
        onSave(item)
        close()
    }

    val palette = LocalPriority.current
    val fieldColor by animateColorAsState(
        if (isTask) palette.of(priority).background else MaterialTheme.colorScheme.surfaceContainer,
        label = "field",
    )
    val past = hasReminder && !rule.isRepeating && LocalDateTime.of(date, time).isBefore(LocalDateTime.now())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        // The form scrolls; the Cancel / Save row stays pinned above the keyboard.
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        existing == null && isTask -> "New task"
                        existing == null -> "New reminder"
                        isTask -> "Edit task"
                        else -> "Edit reminder"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (existing != null && isTask) {
                    IconButton(onClick = { onFocus(existing); close() }) {
                        Icon(Icons.Outlined.HourglassTop, "Focus on this task")
                    }
                }
                if (existing != null) {
                    IconButton(onClick = { onDelete(existing); close() }) {
                        Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Title + voice
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                placeholder = { Text(if (isTask) "What do you need to do?" else "Remind me to…") },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(18.dp),
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (title.text.isNotBlank()) save() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val hindi = voiceLanguage.startsWith("hi")
                        Text(
                            if (hindi) "हिं" else "EN",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onVoiceLanguageChange(if (hindi) "en-IN" else "hi-IN") }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        IconButton(onClick = voice) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Mic, "Speak", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                },
            )

            AnimatedVisibility(visible = suggestion != null) {
                val s = suggestion
                if (s != null) {
                    val label = buildString {
                        s.dateTime?.let { append(Format.dateTime(context, it)) }
                        if (s.repeat.isRepeating) {
                            if (isNotEmpty()) append(" · ")
                            append(Format.repeat(s.repeat))
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = {
                                title = TextFieldValue(s.title, TextRange(s.title.length))
                                apply(s)
                                dismissedSuggestionFor = s.title
                            },
                            label = { Text("Set reminder: $label") },
                            leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = null,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        IconButton(onClick = { dismissedSuggestionFor = title.text }) {
                            Icon(Icons.Outlined.Close, "Ignore", Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (isTask) {
                Spacer(Modifier.height(18.dp))
                Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                PriorityPicker(priority) { priority = it }
            }

            Spacer(Modifier.height(18.dp))
            if (isTask && !hasReminder) {
                Surface(
                    onClick = { hasReminder = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AlarmAdd, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Add reminder", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Reminder",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (isTask) TextButton(onClick = { hasReminder = false; rule = RepeatRule.NONE }) { Text("Remove") }
                }
                if (isTask) {
                    QuickPicks { dt -> date = dt.toLocalDate(); time = dt.toLocalTime() }
                    Spacer(Modifier.height(6.dp))
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(4.dp)) {
                        FieldRow(Icons.Outlined.CalendarToday, "Date", Format.date(date), onClick = { pickDate = true })
                        FieldRow(
                            Icons.Outlined.Schedule, "Time", Format.time(context, time), onClick = { pickTime = true },
                            valueColor = if (past) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                        FieldRow(Icons.Outlined.Repeat, "Repeat", Format.repeat(rule), onClick = { pickRepeat = true })
                    }
                }
                if (past) {
                    Text(
                        "This time has already passed, so no alert will ring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("Alert", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !alarm, onClick = { alarm = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = { Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(18.dp)) },
                    ) { Text("Notification") }
                    SegmentedButton(
                        selected = alarm, onClick = { alarm = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = { Icon(Icons.Outlined.Alarm, null, Modifier.size(18.dp)) },
                    ) { Text("Alarm") }
                }
                Text(
                    if (alarm) "Rings full-screen with your alarm tone until you stop it. Change tone"
                    else "Uses your phone's notification sound. Change tone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            SystemSettings.openChannelSound(
                                context,
                                if (alarm) Notifications.CHANNEL_ALARMS else Notifications.CHANNEL_REMINDERS,
                            )
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
        }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { close() }) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { save() },
                    enabled = title.text.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (pickDate) DatePickDialog(date, onDismiss = { pickDate = false }) { date = it }
    if (pickTime) TimePickDialog(time, onDismiss = { pickTime = false }) { time = it }
    if (pickRepeat) RepeatDialog(rule, date, onDismiss = { pickRepeat = false }) { rule = it }
}

@Composable
private fun PriorityPicker(selected: Int, onSelect: (Int) -> Unit) {
    val palette = LocalPriority.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(Item.PRIORITY_NONE, Item.PRIORITY_LOW, Item.PRIORITY_MEDIUM, Item.PRIORITY_HIGH).forEach { p ->
            val colors = palette.of(p)
            val isSel = p == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(p) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .border(2.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (p == Item.PRIORITY_NONE) colors.background else colors.dot)
                        .border(1.dp, if (p == Item.PRIORITY_NONE) MaterialTheme.colorScheme.outline else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSel) Icon(
                        Icons.Filled.Check, null,
                        tint = if (p == Item.PRIORITY_NONE) MaterialTheme.colorScheme.onSurface else Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    colors.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun QuickPicks(onPick: (LocalDateTime) -> Unit) {
    val now = LocalDateTime.now().withSecond(0).withNano(0)
    val picks = buildList {
        add("In 1 hour" to now.plusHours(1))
        if (now.toLocalTime().isBefore(LocalTime.of(19, 0))) add("This evening" to now.toLocalDate().atTime(19, 0))
        add("Tomorrow morning" to now.toLocalDate().plusDays(1).atTime(9, 0))
        add("Next week" to now.toLocalDate().plusWeeks(1).atTime(9, 0))
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        picks.forEach { (label, dt) ->
            SuggestionChip(onClick = { onPick(dt) }, label = { Text(label) }, shape = RoundedCornerShape(10.dp))
        }
    }
}
