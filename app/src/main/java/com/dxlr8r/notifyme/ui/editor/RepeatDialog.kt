package com.dxlr8r.notifyme.ui.editor

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dxlr8r.notifyme.core.EndType
import com.dxlr8r.notifyme.core.RepeatRule
import com.dxlr8r.notifyme.core.RepeatType
import com.dxlr8r.notifyme.ui.common.DatePickDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class Choice(val label: String) {
    NONE("Does not repeat"), DAILY("Every day"), WEEKDAYS("Weekdays (Mon–Fri)"),
    WEEKLY("Every week"), MONTHLY("Every month"), YEARLY("Every year"), CUSTOM("Custom…"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatDialog(
    initial: RepeatRule,
    anchorDate: LocalDate,
    onDismiss: () -> Unit,
    onDone: (RepeatRule) -> Unit,
) {
    val anchorBit = RepeatRule.bit(anchorDate.dayOfWeek)
    var choice by remember {
        mutableStateOf(
            when (initial.type) {
                RepeatType.NONE -> Choice.NONE
                RepeatType.DAILY -> Choice.DAILY
                RepeatType.WEEKDAYS -> Choice.WEEKDAYS
                RepeatType.WEEKLY -> Choice.WEEKLY
                RepeatType.MONTHLY -> Choice.MONTHLY
                RepeatType.YEARLY -> Choice.YEARLY
                RepeatType.CUSTOM_DAYS, RepeatType.CUSTOM_WEEKS -> Choice.CUSTOM
            },
        )
    }
    var days by remember { mutableIntStateOf(if (initial.weekDays != 0) initial.weekDays else anchorBit) }
    var interval by remember { mutableIntStateOf(initial.interval.coerceAtLeast(if (initial.type == RepeatType.CUSTOM_DAYS || initial.type == RepeatType.CUSTOM_WEEKS) 1 else 2)) }
    var customWeeks by remember { mutableStateOf(initial.type == RepeatType.CUSTOM_WEEKS) }
    var endType by remember { mutableStateOf(initial.endType) }
    var endDate by remember { mutableStateOf(initial.endDate ?: anchorDate.plusMonths(1)) }
    var count by remember { mutableIntStateOf(if (initial.count > 0) initial.count else 10) }
    var pickEnd by remember { mutableStateOf(false) }

    fun build(): RepeatRule {
        val type = when (choice) {
            Choice.NONE -> return RepeatRule.NONE
            Choice.DAILY -> RepeatType.DAILY
            Choice.WEEKDAYS -> RepeatType.WEEKDAYS
            Choice.WEEKLY -> RepeatType.WEEKLY
            Choice.MONTHLY -> RepeatType.MONTHLY
            Choice.YEARLY -> RepeatType.YEARLY
            Choice.CUSTOM -> if (customWeeks) RepeatType.CUSTOM_WEEKS else RepeatType.CUSTOM_DAYS
        }
        val mask = if (type == RepeatType.WEEKLY || type == RepeatType.CUSTOM_WEEKS) (if (days == 0) anchorBit else days) else 0
        return RepeatRule(
            type = type,
            interval = if (choice == Choice.CUSTOM) interval else 1,
            weekDays = mask,
            endType = endType,
            endDate = if (endType == EndType.ON_DATE) endDate else null,
            count = if (endType == EndType.AFTER_COUNT) count else 0,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onDone(build()); onDismiss() }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Repeat") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Choice.values().forEach { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = choice == c, onClick = { choice = c }, role = Role.RadioButton)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == c, onClick = null, modifier = Modifier.padding(8.dp))
                        Text(c.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                if (choice == Choice.CUSTOM) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Every", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(8.dp))
                        Stepper(interval, onChange = { interval = it.coerceIn(1, 99) })
                    }
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !customWeeks, onClick = { customWeeks = false },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text(if (interval == 1) "day" else "days") }
                        SegmentedButton(
                            selected = customWeeks, onClick = { customWeeks = true },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text(if (interval == 1) "week" else "weeks") }
                    }
                }

                if (choice == Choice.WEEKLY || (choice == Choice.CUSTOM && customWeeks)) {
                    Spacer(Modifier.height(12.dp))
                    Text("On", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    WeekdayChips(days, onChange = { days = it })
                }

                if (choice != Choice.NONE) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Ends", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EndRow("Never", endType == EndType.NEVER) { endType = EndType.NEVER }
                    EndRow(
                        "On ${endDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))}",
                        endType == EndType.ON_DATE,
                    ) { endType = EndType.ON_DATE; pickEnd = true }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = endType == EndType.AFTER_COUNT, onClick = { endType = EndType.AFTER_COUNT })
                        Text("After", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(8.dp))
                        Stepper(count, onChange = { count = it.coerceIn(1, 999); endType = EndType.AFTER_COUNT })
                        Spacer(Modifier.width(6.dp))
                        Text("times", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
    )

    if (pickEnd) {
        DatePickDialog(endDate, onDismiss = { pickEnd = false }) { endDate = if (it.isBefore(anchorDate)) anchorDate else it }
    }
}

@Composable
private fun EndRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(value - 1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Remove, "Less", Modifier.size(18.dp))
        }
        Text("$value", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton(onClick = { onChange(value + 1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Add, "More", Modifier.size(18.dp))
        }
    }
}

@Composable
fun WeekdayChips(mask: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.values().forEach { d ->
            val bit = RepeatRule.bit(d)
            val on = mask and bit != 0
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { val next = mask xor bit; if (next != 0) onChange(next) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
