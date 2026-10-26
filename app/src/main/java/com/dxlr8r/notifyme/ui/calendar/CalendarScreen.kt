package com.dxlr8r.notifyme.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dxlr8r.notifyme.core.Recurrence
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.ui.common.Format
import com.dxlr8r.notifyme.ui.tasks.SwipeToDelete
import com.dxlr8r.notifyme.ui.theme.Brand
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private const val PAGES = 2401
private const val CENTER = 1200

@Composable
fun CalendarScreen(
    events: List<Item>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    contentPadding: PaddingValues,
    onOpen: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    banner: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val baseMonth = remember { YearMonth.from(today) }
    val pager = rememberPagerState(initialPage = CENTER + monthsBetween(baseMonth, YearMonth.from(selected))) { PAGES }
    val scope = rememberCoroutineScope()
    val visibleMonth = baseMonth.plusMonths((pager.currentPage - CENTER).toLong())
    val firstDay = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

    // Keep the pager in step when the selected date jumps to another month (e.g. "Today").
    LaunchedEffect(selected) {
        val target = CENTER + monthsBetween(baseMonth, YearMonth.from(selected))
        if (target != pager.currentPage) pager.animateScrollToPage(target)
    }
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val m = baseMonth.plusMonths((page - CENTER).toLong())
            if (YearMonth.from(currentSelected) != m) currentOnSelect(if (m == YearMonth.from(today)) today else m.atDay(1))
        }
    }

    val dayItems = remember(events, selected) { itemsOn(events, selected) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
    ) {
        item(key = "banner") { Box(Modifier.padding(horizontal = 16.dp)) { banner() } }
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (selected != today) {
                    TextButton(onClick = { onSelect(today) }) { Text("Today") }
                }
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous month")
                }
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Icon(Icons.Outlined.ChevronRight, "Next month")
                }
            }
        }
        item(key = "weekdays") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                (0 until 7).forEach { i ->
                    val d = firstDay.plus(i.toLong())
                    Text(
                        d.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        item(key = "grid") {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), beyondViewportPageCount = 1) { page ->
                val month = baseMonth.plusMonths((page - CENTER).toLong())
                val dots = remember(events, month) { datesWithReminders(events, month) }
                MonthGrid(month, firstDay, today, selected, dots, onSelect)
            }
        }
        item(key = "day-header") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (selected == today) "Today" else selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (dayItems.isNotEmpty()) {
                    Text(
                        "${dayItems.size} reminder${if (dayItems.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (dayItems.isEmpty()) {
            item(key = "empty") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.EventAvailable, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No reminders on this day.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        items(dayItems, key = { it.first.id }) { (item, at) ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 5.dp).animateItem()) {
                SwipeToDelete(onDelete = { onDelete(item) }) {
                    EventRow(item, at, onClick = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDay: DayOfWeek,
    today: LocalDate,
    selected: LocalDate,
    dots: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val lead = ((first.dayOfWeek.value - firstDay.value) + 7) % 7
    val cells = lead + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val index = r * 7 + c - lead
                    Box(Modifier.weight(1f).height(52.dp), contentAlignment = Alignment.Center) {
                        if (index in 0 until month.lengthOfMonth()) {
                            val date = month.atDay(index + 1)
                            DayCell(date, date == today, date == selected, date in dots, onClick = { onSelect(date) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, hasDot: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) primary else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !hasDot -> androidx.compose.ui.graphics.Color.Transparent
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        else -> Brand.CalendarDot
                    },
                ),
        )
    }
}

@Composable
private fun EventRow(item: Item, at: LocalDateTime, onClick: () -> Unit) {
    val context = LocalContext.current
    val past = at.isBefore(LocalDateTime.now())
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(if (past) MaterialTheme.colorScheme.outline else Brand.CalendarDot))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (past) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Format.time(context, at.toLocalTime()),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (past) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                    if (item.rule.isRepeating) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Repeat, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(3.dp))
                        Text(Format.repeat(item.rule), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Icon(
                if (item.alarm) Icons.Filled.Alarm else Icons.Outlined.NotificationsNone,
                if (item.alarm) "Alarm" else "Notification",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun monthsBetween(a: YearMonth, b: YearMonth): Int = ((b.year - a.year) * 12 + (b.monthValue - a.monthValue))

private fun datesWithReminders(events: List<Item>, month: YearMonth): Set<LocalDate> {
    val from = month.atDay(1)
    val to = month.atEndOfMonth()
    val out = HashSet<LocalDate>()
    for (e in events) {
        val anchor = e.anchorDateTime ?: continue
        if (anchor.toLocalDate().isAfter(to)) continue
        out += Recurrence.datesBetween(e.rule, anchor, from, to)
    }
    return out
}

private fun itemsOn(events: List<Item>, date: LocalDate): List<Pair<Item, LocalDateTime>> =
    events.mapNotNull { e ->
        val anchor = e.anchorDateTime ?: return@mapNotNull null
        if (anchor.toLocalDate().isAfter(date)) return@mapNotNull null
        if (Recurrence.datesBetween(e.rule, anchor, date, date).isEmpty()) null
        else e to date.atTime(anchor.toLocalTime())
    }.sortedBy { it.second }
