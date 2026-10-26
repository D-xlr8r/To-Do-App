package com.dxlr8r.notifyme.ui.onboarding

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.AlarmAdd
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dxlr8r.notifyme.R
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.data.toEpochMillis
import com.dxlr8r.notifyme.ui.focus.Hourglass
import com.dxlr8r.notifyme.ui.focus.HourglassColors
import com.dxlr8r.notifyme.ui.tasks.TaskRow
import com.dxlr8r.notifyme.ui.theme.Brand
import com.dxlr8r.notifyme.widget.NotifyWidgetReceiver
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

private data class Page(
    val title: String,
    val body: String,
    val art: @Composable BoxScope.() -> Unit,
)

/** First-run tour of the main buttons, ending with an offer to add the home-screen widget. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val pages = remember {
        listOf(
            Page(
                "Welcome to NotifyMe",
                "A calm place for your tasks and reminders. Here is a quick 30-second tour of the buttons you'll use.",
            ) { WelcomeArt() },
            Page(
                "Add a task in seconds",
                "Tap the + button to add a task. Tap the mic and just say it — \"Pay electricity bill tomorrow at 6 pm\" — in English or Hindi, and the date and time fill in by themselves.",
            ) { AddArt() },
            Page(
                "Priority, reminders and done",
                "Pick a colour for priority when adding a task. Tap the clock on any task to set a reminder or alarm. Tick the box when it's done.",
            ) { TasksArt() },
            Page(
                "Your calendar",
                "Open the Calendar tab and tap any date to add a reminder there. Red dots show the days that have reminders.",
            ) { CalendarArt() },
            Page(
                "Focus with the hourglass",
                "The Focus tab gives you a calm full-screen clock. Start a timer and watch the sand fall while you work.",
            ) { FocusArt() },
            Page(
                "Put it on your home screen",
                "Add the sticky-note widget to see pending tasks and today's reminders without opening the app. Tick tasks right from the widget.",
            ) { WidgetPreview(Modifier.widthIn(max = 330.dp).fillMaxWidth()) },
        )
    }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    var widgetRequested by remember { mutableStateOf(false) }
    val canPin = remember { canPinWidget(context) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${pager.currentPage + 1} of ${pages.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (!last) TextButton(onClick = onDone) { Text("Skip") }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 300.dp),
                    contentAlignment = Alignment.Center,
                    content = page.art,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (index == pages.lastIndex) {
                    Spacer(Modifier.height(20.dp))
                    if (canPin) {
                        Button(
                            onClick = { widgetRequested = requestPinWidget(context) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(50.dp),
                        ) {
                            Icon(Icons.Outlined.AddToHomeScreen, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add widget to home screen")
                        }
                        if (widgetRequested) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Confirm on the pop-up, then drop it where you like.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        ManualWidgetSteps()
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // Page dots
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val active = i == pager.currentPage
                val w by animateDpAsState(if (active) 22.dp else 8.dp, label = "dot")
                val c by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    label = "dotColor",
                )
                Box(Modifier.padding(horizontal = 3.dp).height(8.dp).width(w).clip(CircleShape).background(c))
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) {
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(50.dp),
                ) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(50.dp).widthIn(min = 120.dp),
            ) { Text(if (last) "Get started" else "Next") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun canPinWidget(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

private fun requestPinWidget(context: Context): Boolean = runCatching {
    AppWidgetManager.getInstance(context)
        .requestPinAppWidget(ComponentName(context, NotifyWidgetReceiver::class.java), null, null)
}.getOrDefault(false)

@Composable
private fun ManualWidgetSteps() {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp)) {
            listOf(
                "Touch and hold an empty spot on your home screen",
                "Tap Widgets",
                "Find NotifyMe, then drag the sticky note onto your screen",
            ).forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("${i + 1}.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(22.dp), fontWeight = FontWeight.SemiBold)
                    Text(step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ illustrations

@Composable
private fun ArtCard(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .widthIn(max = 330.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun WelcomeArt() {
    Image(
        painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = "NotifyMe",
        modifier = Modifier.size(240.dp).clip(RoundedCornerShape(56.dp)),
    )
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, size: Dp, square: Boolean = false) {
    Box(
        Modifier
            .size(size)
            .clip(if (square) RoundedCornerShape(size * 0.3f) else CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun AddArt() {
    ArtCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pay electricity bill tomorrow at 6 pm",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    RoundIcon(Icons.Outlined.Mic, 40.dp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "✨ Set reminder: Tomorrow, 6:00 PM",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tap", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(12.dp))
                RoundIcon(Icons.Filled.Add, 60.dp, square = true)
                Spacer(Modifier.width(12.dp))
                Text("to add", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TasksArt() {
    val now = remember { LocalDateTime.now().withMinute(0).withSecond(0).withNano(0) }
    val samples = remember {
        listOf(
            Item(id = -1, title = "Pay electricity bill", priority = Item.PRIORITY_HIGH, anchor = now.plusHours(3).toString(), nextAt = now.plusHours(3).toEpochMillis()),
            Item(id = -2, title = "Buy milk and bread", priority = Item.PRIORITY_MEDIUM),
            Item(id = -3, title = "Call the bank", priority = Item.PRIORITY_LOW),
        )
    }
    ArtCard {
        Column {
            samples.forEach { item ->
                TaskRow(item, onCheck = {}, onOpen = {}, onReminder = {})
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Legend(Icons.Outlined.CheckCircle, "Tick = done")
                Legend(Icons.Outlined.AlarmAdd, "Clock = reminder")
            }
        }
    }
}

@Composable
private fun Legend(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalendarArt() {
    val today = remember { LocalDate.now() }
    val dots = remember { setOf(3, 8, 14, 21, 27) }
    ArtCard {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    today.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            for (r in 0 until 4) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c + 1
                        val selected = day == 14
                        Column(Modifier.weight(1f).padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Box(
                                Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape)
                                    .background(if (day in dots) Brand.CalendarDot else Color.Transparent),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusArt() {
    Box(
        Modifier
            .widthIn(max = 330.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("24:10", color = Color(0xFF9AA39D), fontSize = 34.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(6.dp))
            Hourglass(
                endAt = 0L,
                pausedLeft = 58_000L,
                totalMs = 100_000L,
                finished = false,
                colors = HourglassColors(
                    woodLight = Color(0xFF6B5340), woodDark = Color(0xFF3E2D20), glass = Color(0xFF6A6383),
                    sandLight = Color(0xFFFFE08A), sand = Color(0xFFF2C14E), sandDark = Color(0xFFC9952E), shadow = Color(0x66000000),
                ),
                modifier = Modifier.size(width = 150.dp, height = 220.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.HourglassTop, null, tint = Color(0xFFA78BFA), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Focus tab", color = Color(0xFFA78BFA), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ------------------------------------------------------------------ widget preview

private val WInk = Color(0xFF1E211F)
private val WInkSoft = Color(0xFF5B6150)
private val WRed = Color(0xFFE0605A)
private val WYellow = Color(0xFFE8A33A)
private val WGreen = Color(0xFF4E9B6A)

private data class SampleTask(val title: String, val dot: Color?, val done: Boolean, val time: String? = null)

private val sampleTasks = listOf(
    SampleTask("Pay electricity bill", WRed, done = false, time = "6:00 PM"),
    SampleTask("Submit project report", WRed, done = true),
    SampleTask("Call the bank", WYellow, done = false, time = "Tmrw 11:00 AM"),
    SampleTask("Buy milk & vegetables", WYellow, done = true),
    SampleTask("Morning walk", WGreen, done = true),
    SampleTask("Book train tickets", null, done = false),
)

/** A faithful picture of the home-screen sticky note, drawn with the widget's own artwork. */
@Composable
fun WidgetPreview(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF5F8C9), Color(0xFFE4EC97))))
            .border(1.dp, Color(0x1A1E211F), RoundedCornerShape(22.dp)),
    ) {
        // tape strip
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(width = 64.dp, height = 9.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(Color(0x40FFFFFF)),
        )
        Column(Modifier.padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(WRed, WYellow, WGreen).forEach {
                    Box(Modifier.padding(end = 4.dp).size(9.dp).clip(CircleShape).background(it))
                }
                Spacer(Modifier.width(8.dp))
                Text("Tasks", color = WInk, fontSize = 16.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                Spacer(Modifier.width(14.dp))
                Text("Today", color = WInkSoft, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Image(painterResource(R.drawable.ic_w_add), contentDescription = null, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x221E211F)))
            Spacer(Modifier.height(4.dp))
            sampleTasks.forEach { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource(if (t.done) R.drawable.ic_w_box_checked else R.drawable.ic_w_box),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).padding(4.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    if (t.dot != null) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (t.done) t.dot.copy(alpha = 0.4f) else t.dot))
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        t.title,
                        color = if (t.done) WInkSoft else WInk,
                        fontSize = 15.sp,
                        textDecoration = if (t.done) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (t.time != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(t.time, color = WInkSoft, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
