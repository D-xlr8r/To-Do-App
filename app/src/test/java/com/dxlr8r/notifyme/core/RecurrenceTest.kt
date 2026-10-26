package com.dxlr8r.notifyme.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class RecurrenceTest {
    @Test fun monthlyKeepsEndOfMonth() {
        val got = Recurrence.occurrences(RepeatRule(RepeatType.MONTHLY), LocalDateTime.of(2026, 1, 31, 9, 0)).take(3).toList()
        assertEquals(listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)), got.map { it.toLocalDate() })
    }

    @Test fun weekdaysSkipWeekend() {
        val got = Recurrence.occurrences(RepeatRule(RepeatType.WEEKDAYS), LocalDateTime.of(2026, 10, 24, 9, 0)).take(6).toList()
        assertEquals(DayOfWeek.MONDAY, got.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 2), got.last().toLocalDate())
    }

    @Test fun everyTwoWeeks() {
        val rule = RepeatRule(RepeatType.CUSTOM_WEEKS, interval = 2, weekDays = RepeatRule.maskOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val got = Recurrence.occurrences(rule, LocalDateTime.of(2026, 10, 26, 9, 0)).take(4).map { it.toLocalDate() }.toList()
        assertEquals(listOf(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13)), got)
    }

    @Test fun countEnds() {
        val rule = RepeatRule(RepeatType.DAILY, endType = EndType.AFTER_COUNT, count = 3)
        assertNull(Recurrence.nextAfter(rule, LocalDateTime.of(2026, 10, 1, 9, 0), LocalDateTime.of(2026, 10, 26, 10, 0)))
    }

    @Test fun endDateInclusive() {
        val rule = RepeatRule(RepeatType.DAILY, endType = EndType.ON_DATE, endDate = LocalDate.of(2026, 10, 28))
        val got = Recurrence.datesBetween(rule, LocalDateTime.of(2026, 10, 26, 9, 0), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertEquals(3, got.size)
    }

    @Test fun oldWeeklyStillFindsThisMonth() {
        val got = Recurrence.datesBetween(RepeatRule(RepeatType.WEEKLY), LocalDateTime.of(2020, 1, 6, 9, 0), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertEquals(listOf(5, 12, 19, 26), got.map { it.dayOfMonth })
    }
}
