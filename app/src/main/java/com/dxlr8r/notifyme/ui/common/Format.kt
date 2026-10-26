package com.dxlr8r.notifyme.ui.common

import android.content.Context
import android.text.format.DateFormat
import com.dxlr8r.notifyme.core.EndType
import com.dxlr8r.notifyme.core.RepeatRule
import com.dxlr8r.notifyme.core.RepeatType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Format {
    private val dayMonth = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val dayMonthYear = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    fun time(context: Context, t: LocalTime): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return t.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun date(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> if (d.year == today.year) d.format(dayMonth) else d.format(dayMonthYear)
    }

    fun dateTime(context: Context, dt: LocalDateTime): String = "${date(dt.toLocalDate())}, ${time(context, dt.toLocalTime())}"

    fun weekdayShort(d: DayOfWeek): String = d.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun repeat(rule: RepeatRule): String {
        val base = when (rule.type) {
            RepeatType.NONE -> return "Does not repeat"
            RepeatType.DAILY -> "Daily"
            RepeatType.WEEKDAYS -> "Weekdays"
            RepeatType.WEEKLY -> if (rule.weekDays == 0) "Weekly" else "Every ${days(rule.weekDays)}"
            RepeatType.MONTHLY -> "Monthly"
            RepeatType.YEARLY -> "Yearly"
            RepeatType.CUSTOM_DAYS -> if (rule.interval == 1) "Daily" else "Every ${rule.interval} days"
            RepeatType.CUSTOM_WEEKS -> {
                val every = if (rule.interval == 1) "Weekly" else "Every ${rule.interval} weeks"
                if (rule.weekDays == 0) every else "$every on ${days(rule.weekDays)}"
            }
        }
        val end = when (rule.endType) {
            EndType.NEVER -> ""
            EndType.ON_DATE -> rule.endDate?.let { " until ${it.format(shortDate)}" } ?: ""
            EndType.AFTER_COUNT -> " · ${rule.count} times"
        }
        return base + end
    }

    private fun days(mask: Int): String =
        DayOfWeek.values().filter { mask and RepeatRule.bit(it) != 0 }.joinToString(", ") { weekdayShort(it) }
}
