package com.dxlr8r.notifyme.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class RepeatType { NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY, YEARLY, CUSTOM_DAYS, CUSTOM_WEEKS }

enum class EndType { NEVER, ON_DATE, AFTER_COUNT }

/**
 * How a reminder repeats.
 * [weekDays] is a bitmask: Monday = bit 0 … Sunday = bit 6 (used by WEEKLY and CUSTOM_WEEKS).
 * [interval] is "every N" for CUSTOM_DAYS / CUSTOM_WEEKS.
 */
data class RepeatRule(
    val type: RepeatType = RepeatType.NONE,
    val interval: Int = 1,
    val weekDays: Int = 0,
    val endType: EndType = EndType.NEVER,
    val endDate: LocalDate? = null,
    val count: Int = 0,
) {
    val isRepeating: Boolean get() = type != RepeatType.NONE

    companion object {
        val NONE = RepeatRule()
        const val WEEKDAY_MASK = 0b0011111 // Mon–Fri

        fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

        fun maskOf(vararg days: DayOfWeek): Int = days.fold(0) { acc, d -> acc or bit(d) }
    }
}

object Recurrence {
    private const val SAFETY_LIMIT = 100_000

    /** All occurrences, in order, starting at (or after) [anchor]. Lazy and possibly infinite. */
    fun occurrences(rule: RepeatRule, anchor: LocalDateTime): Sequence<LocalDateTime> {
        val n = rule.interval.coerceAtLeast(1).toLong()
        val base: Sequence<LocalDateTime> = when (rule.type) {
            RepeatType.NONE -> sequenceOf(anchor)
            RepeatType.DAILY -> counter().map { anchor.plusDays(it) }
            RepeatType.CUSTOM_DAYS -> counter().map { anchor.plusDays(it * n) }
            RepeatType.MONTHLY -> counter().map { anchor.plusMonths(it) }
            RepeatType.YEARLY -> counter().map { anchor.plusYears(it) }
            RepeatType.WEEKDAYS -> weekly(anchor, RepeatRule.WEEKDAY_MASK, 1)
            RepeatType.WEEKLY -> weekly(anchor, maskOrAnchor(rule.weekDays, anchor), 1)
            RepeatType.CUSTOM_WEEKS -> weekly(anchor, maskOrAnchor(rule.weekDays, anchor), n)
        }
        val limited = base.take(SAFETY_LIMIT)
        return when (rule.endType) {
            EndType.NEVER -> limited
            EndType.ON_DATE -> rule.endDate?.let { end -> limited.takeWhile { !it.toLocalDate().isAfter(end) } } ?: limited
            EndType.AFTER_COUNT -> limited.take(rule.count.coerceAtLeast(1))
        }
    }

    /** First occurrence strictly after [after], or null if the series has ended. */
    fun nextAfter(rule: RepeatRule, anchor: LocalDateTime, after: LocalDateTime): LocalDateTime? =
        occurrences(rule, anchor).firstOrNull { it.isAfter(after) }

    /** Dates (inclusive range) on which the series occurs. */
    fun datesBetween(rule: RepeatRule, anchor: LocalDateTime, from: LocalDate, to: LocalDate): List<LocalDate> =
        occurrences(rule, anchor)
            .takeWhile { !it.toLocalDate().isAfter(to) }
            .map { it.toLocalDate() }
            .filter { !it.isBefore(from) }
            .distinct()
            .toList()

    private fun counter(): Sequence<Long> = generateSequence(0L) { it + 1 }

    private fun maskOrAnchor(mask: Int, anchor: LocalDateTime): Int =
        if (mask and 0x7F != 0) mask and 0x7F else RepeatRule.bit(anchor.dayOfWeek)

    private fun weekly(anchor: LocalDateTime, mask: Int, everyWeeks: Long): Sequence<LocalDateTime> {
        val anchorDate = anchor.toLocalDate()
        val anchorWeek = startOfWeek(anchorDate)
        return counter()
            .map { anchorDate.plusDays(it) }
            .filter { date ->
                (mask and RepeatRule.bit(date.dayOfWeek)) != 0 &&
                    ChronoUnit.WEEKS.between(anchorWeek, startOfWeek(date)) % everyWeeks == 0L
            }
            .map { it.atTime(anchor.toLocalTime()) }
    }

    private fun startOfWeek(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())
}
