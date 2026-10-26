package com.dxlr8r.notifyme.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.dxlr8r.notifyme.R
import com.dxlr8r.notifyme.core.Recurrence
import com.dxlr8r.notifyme.data.Item
import com.dxlr8r.notifyme.data.Repository
import com.dxlr8r.notifyme.data.toLocalDateTime
import com.dxlr8r.notifyme.ui.MainActivity
import com.dxlr8r.notifyme.ui.common.Format
import java.time.LocalDate
import java.time.LocalDateTime

private val Ink = Color(0xFF1E211F)
private val InkSoft = Color(0xFF5B6150)
private val Line = Color(0x221E211F)
private val DotRed = Color(0xFFE0605A)
private val DotYellow = Color(0xFFE8A33A)
private val DotGreen = Color(0xFF4E9B6A)

private val MODE = intPreferencesKey("mode") // 0 = tasks, 1 = today
private val KEY_ID = ActionParameters.Key<Long>("id")
private val KEY_MODE = ActionParameters.Key<Int>("mode")

/** Sticky-note home-screen widget: pending tasks, or everything due today. */
class NotifyWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = Repository.get(context)
        val firstTasks = repo.pendingTasks()
        val firstEvents = repo.allEvents()
        provideContent {
            val tasks by repo.tasks.collectAsState(initial = firstTasks)
            val events by repo.events.collectAsState(initial = firstEvents)
            val mode = currentState<Preferences>()[MODE] ?: 0
            StickyNote(context, mode, tasks.filter { !it.done }, events)
        }
    }
}

class NotifyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotifyWidget()
}

object WidgetUpdater {
    suspend fun update(context: Context) {
        runCatching { NotifyWidget().updateAll(context) }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[KEY_ID] ?: return
        Repository.get(context).setDone(id, true)
    }
}

class SwitchModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val m = parameters[KEY_MODE] ?: 0
        updateAppWidgetState(context, glanceId) { it[MODE] = m }
        NotifyWidget().update(context, glanceId)
    }
}

private data class TodayEntry(val time: LocalDateTime, val title: String, val isTask: Boolean)

@Composable
private fun StickyNote(context: Context, mode: Int, tasks: List<Item>, events: List<Item>) {
    val addIntent = Intent(context, MainActivity::class.java)
        .setAction(if (mode == 0) MainActivity.ACTION_ADD_TASK else MainActivity.ACTION_ADD_REMINDER)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val openIntent = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_TAB, if (mode == 0) 0 else 1)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(22.dp)
            .background(ImageProvider(R.drawable.widget_bg)),
    ) {
        Column(GlanceModifier.fillMaxSize().padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 8.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Widget rows can hold at most 10 children, so the three dots share one inner row.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(DotRed); Spacer(GlanceModifier.width(4.dp)); Dot(DotYellow); Spacer(GlanceModifier.width(4.dp)); Dot(DotGreen)
                }
                Spacer(GlanceModifier.width(12.dp))
                Tab("Tasks", mode == 0, 0)
                Spacer(GlanceModifier.width(14.dp))
                Tab("Today", mode == 1, 1)
                Spacer(GlanceModifier.defaultWeight())
                Image(
                    ImageProvider(R.drawable.ic_w_add),
                    contentDescription = "Add",
                    modifier = GlanceModifier.size(32.dp).clickable(actionStartActivity(addIntent)),
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Line))) {}
            Spacer(GlanceModifier.height(4.dp))

            if (mode == 0) {
                if (tasks.isEmpty()) {
                    Empty("No pending tasks. Tap + to add one.", openIntent)
                } else {
                    LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                        items(tasks, itemId = { it.id }) { task -> TaskLine(context, task, openIntent) }
                    }
                }
            } else {
                val today = todayEntries(tasks, events)
                if (today.isEmpty()) {
                    Empty("Nothing scheduled for today.", openIntent)
                } else {
                    LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                        items(today) { e -> TodayLine(context, e, openIntent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Image(
        ImageProvider(R.drawable.widget_dot),
        contentDescription = null,
        modifier = GlanceModifier.size(9.dp),
        colorFilter = ColorFilter.tint(ColorProvider(color)),
    )
}

@Composable
private fun Tab(label: String, active: Boolean, mode: Int) {
    Text(
        label,
        style = TextStyle(
            color = ColorProvider(if (active) Ink else InkSoft),
            fontSize = 18.75.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            textDecoration = if (active) TextDecoration.Underline else TextDecoration.None,
        ),
        modifier = GlanceModifier.clickable(actionRunCallback<SwitchModeAction>(actionParametersOf(KEY_MODE to mode))),
    )
}

@Composable
private fun TaskLine(context: Context, task: Item, openIntent: Intent) {
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            ImageProvider(R.drawable.ic_w_box),
            contentDescription = "Mark done",
            modifier = GlanceModifier.size(30.dp).padding(5.dp)
                .clickable(actionRunCallback<ToggleTaskAction>(actionParametersOf(KEY_ID to task.id))),
        )
        Spacer(GlanceModifier.width(6.dp))
        if (task.priority != Item.PRIORITY_NONE) {
            Image(
                ImageProvider(R.drawable.widget_dot),
                contentDescription = null,
                modifier = GlanceModifier.size(8.dp),
                colorFilter = ColorFilter.tint(
                    ColorProvider(
                        when (task.priority) {
                            Item.PRIORITY_HIGH -> DotRed
                            Item.PRIORITY_MEDIUM -> DotYellow
                            else -> DotGreen
                        },
                    ),
                ),
            )
            Spacer(GlanceModifier.width(7.dp))
        }
        Text(
            task.title,
            maxLines = 2,
            style = TextStyle(color = ColorProvider(Ink), fontSize = 17.5.sp),
            modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openIntent)),
        )
        val next = task.snoozeAt ?: task.nextAt
        if (next != null) {
            Spacer(GlanceModifier.width(6.dp))
            Text(
                shortWhen(context, next.toLocalDateTime()),
                style = TextStyle(color = ColorProvider(InkSoft), fontSize = 13.75.sp),
            )
        }
    }
}

@Composable
private fun TodayLine(context: Context, e: TodayEntry, openIntent: Intent) {
    Row(
        GlanceModifier.fillMaxWidth().padding(vertical = 6.dp).clickable(actionStartActivity(openIntent)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Format.time(context, e.time.toLocalTime()),
            style = TextStyle(color = ColorProvider(Ink), fontSize = 16.25.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.width(88.dp),
        )
        Image(
            ImageProvider(R.drawable.widget_dot),
            contentDescription = null,
            modifier = GlanceModifier.size(7.dp),
            colorFilter = ColorFilter.tint(ColorProvider(if (e.isTask) DotGreen else DotRed)),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(e.title, maxLines = 2, style = TextStyle(color = ColorProvider(Ink), fontSize = 17.5.sp), modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun Empty(text: String, openIntent: Intent) {
    Box(
        GlanceModifier.fillMaxSize().padding(8.dp).clickable(actionStartActivity(openIntent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(color = ColorProvider(InkSoft), fontSize = 16.25.sp))
    }
}

private fun shortWhen(context: Context, dt: LocalDateTime): String {
    val today = LocalDate.now()
    val time = Format.time(context, dt.toLocalTime())
    return when (dt.toLocalDate()) {
        today -> time
        today.plusDays(1) -> "Tmrw $time"
        else -> dt.toLocalDate().let { "${it.dayOfMonth}/${it.monthValue}" }
    }
}

private fun todayEntries(tasks: List<Item>, events: List<Item>): List<TodayEntry> {
    val today = LocalDate.now()
    val out = ArrayList<TodayEntry>()
    for (e in events) {
        val anchor = e.anchorDateTime ?: continue
        if (anchor.toLocalDate().isAfter(today)) continue
        if (Recurrence.datesBetween(e.rule, anchor, today, today).isNotEmpty()) {
            out += TodayEntry(today.atTime(anchor.toLocalTime()), e.title, isTask = false)
        }
    }
    for (t in tasks) {
        val next = (t.snoozeAt ?: t.nextAt)?.toLocalDateTime() ?: continue
        if (next.toLocalDate() == today) out += TodayEntry(next, t.title, isTask = true)
    }
    return out.sortedBy { it.time }
}
