package com.dxlr8r.notifyme.ui.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.outlined.AlarmAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.data.toLocalDateTime
import com.dxlr8r.notifyme.ui.common.Format
import com.dxlr8r.notifyme.ui.common.TaskCheckbox
import com.dxlr8r.notifyme.ui.theme.LocalPriority

@Composable
fun TasksScreen(
    tasks: List<Item>?,
    contentPadding: PaddingValues,
    completedExpanded: Boolean,
    onToggleCompleted: () -> Unit,
    onCheck: (Item, Boolean) -> Unit,
    onOpen: (Item) -> Unit,
    onReminder: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onClearCompleted: () -> Unit,
    banner: @Composable () -> Unit,
) {
    if (tasks == null) return
    val pending = tasks.filter { !it.done }
    val done = tasks.filter { it.done }.sortedByDescending { it.completedAt ?: 0 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "banner") { banner() }

        if (pending.isEmpty()) {
            item(key = "empty") { EmptyState(Modifier.animateItem()) }
        }

        items(pending, key = { it.id }) { task ->
            SwipeToDelete(onDelete = { onDelete(task) }, modifier = Modifier.animateItem()) {
                TaskRow(task, onCheck = { onCheck(task, true) }, onOpen = { onOpen(task) }, onReminder = { onReminder(task) })
            }
        }

        if (done.isNotEmpty()) {
            item(key = "completed-header") {
                CompletedHeader(done.size, completedExpanded, onToggleCompleted, onClearCompleted, Modifier.animateItem())
            }
            if (completedExpanded) {
                items(done, key = { it.id }) { task ->
                    SwipeToDelete(onDelete = { onDelete(task) }, modifier = Modifier.animateItem()) {
                        TaskRow(task, onCheck = { onCheck(task, false) }, onOpen = { onOpen(task) }, onReminder = null)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Item, onCheck: () -> Unit, onOpen: () -> Unit, onReminder: (() -> Unit)?) {
    val context = LocalContext.current
    val colors = LocalPriority.current.of(task.priority)
    val bg = if (task.done) MaterialTheme.colorScheme.surfaceContainer else colors.background
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = if (task.priority == Item.PRIORITY_NONE && !task.done) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TaskCheckbox(checked = task.done, onToggle = onCheck)
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                val next = task.snoozeAt ?: task.nextAt
                if (!task.done && task.hasReminder) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (task.alarm) Icons.Filled.Alarm else Icons.Outlined.NotificationsNone, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            next?.let { Format.dateTime(context, it.toLocalDateTime()) }
                                ?: task.anchorDateTime?.let { "Reminded ${Format.dateTime(context, it).replaceFirstChar { c -> c.lowercase() }}" }
                                ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (task.rule.isRepeating) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Outlined.Repeat, "Repeats", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (onReminder != null) {
                IconButton(onClick = onReminder) {
                    if (task.hasReminder && (task.nextAt != null || task.snoozeAt != null)) {
                        Icon(Icons.Filled.Alarm, "Edit reminder", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Outlined.AlarmAdd, "Add reminder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        // Returning false keeps no "dismissed" state around, so an undone delete comes back cleanly.
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) onDelete()
            false
        },
        positionalThreshold = { it * 0.4f },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        },
    ) { content() }
}

@Composable
private fun CompletedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit, onClear: () -> Unit, modifier: Modifier) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Completed · $count",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (expanded) TextButton(onClick = onClear) { Text("Clear all") }
        Icon(Icons.Outlined.KeyboardArrowDown, if (expanded) "Hide" else "Show", Modifier.rotate(rotation), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(top = 72.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.TaskAlt, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text("You're all clear", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap + to add a task. Or tap the mic and say\n“Pay electricity bill tomorrow at 6 pm”.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
