package com.dxlr8r.notifyme.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class SmartParserTest {
    // Monday 26 Oct 2026, 10:00
    private val now = LocalDateTime.of(2026, 10, 26, 10, 0)
    private fun p(s: String) = SmartParser.parse(s, now)
    private fun at(d: Int, h: Int, m: Int = 0, month: Int = 10, y: Int = 2026) = LocalDateTime.of(y, month, d, h, m)

    @Test fun englishTomorrowPm() {
        val r = p("remind me to pay electricity bill tomorrow at 6 pm")
        assertEquals("Pay electricity bill", r.title)
        assertEquals(at(27, 18), r.dateTime)
    }

    @Test fun bareHourPicksEvening() = assertEquals(at(26, 21), p("Call mom at 9").dateTime)
    @Test fun earlyHourMeansPm() = assertEquals(at(26, 18), p("call papa at 6").dateTime)
    @Test fun plainTextHasNoReminder() = assertNull(p("buy milk").dateTime)
    @Test fun weekdayAndMinutes() = assertEquals(at(30, 15, 30), p("meeting with Rahul on friday at 3:30pm").dateTime)
    @Test fun relativeHours() = assertEquals(at(26, 12), p("submit report in 2 hours").dateTime)
    @Test fun noon() = assertEquals(at(26, 12), p("lunch at noon").dateTime)
    @Test fun explicitDate() = assertEquals(at(5, 16, month = 11), p("doctor appointment 5 November 4pm").dateTime)
    @Test fun indianNumericDate() = assertEquals(at(25, 11, month = 12), p("dentist 25/12 at 11").dateTime)

    @Test fun dailyRepeat() {
        val r = p("take medicine every day at 8 am")
        assertEquals("Take medicine", r.title)
        assertEquals(RepeatType.DAILY, r.repeat.type)
        assertEquals(at(27, 8), r.dateTime)
    }

    @Test fun weeklyOnTwoDays() {
        val r = p("every monday and thursday 7am gym")
        assertEquals("Gym", r.title)
        assertEquals(RepeatType.WEEKLY, r.repeat.type)
        assertEquals(RepeatRule.maskOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), r.repeat.weekDays)
        assertEquals(at(29, 7), r.dateTime)
    }

    @Test fun customDays() {
        val r = p("water plants every 3 days")
        assertEquals(RepeatType.CUSTOM_DAYS, r.repeat.type)
        assertEquals(3, r.repeat.interval)
    }

    @Test fun hindiEvening() {
        val r = p("कल शाम 6 बजे दवाई लेनी है")
        assertEquals("दवाई लेनी", r.title)
        assertEquals(at(27, 18), r.dateTime)
    }

    @Test fun hindiSaadhe() {
        val r = p("मुझे परसों सुबह साढ़े 9 बजे डॉक्टर के पास जाना है याद दिलाना")
        assertEquals("डॉक्टर के पास जाना", r.title)
        assertEquals(at(28, 9, 30), r.dateTime)
    }

    @Test fun hinglishWeekly() {
        val r = p("har somvar subah 9 baje team meeting")
        assertEquals("Team meeting", r.title)
        assertEquals(RepeatType.WEEKLY, r.repeat.type)
        assertEquals(at(2, 9, month = 11), r.dateTime)
    }

    @Test fun hinglishNight() = assertEquals(at(27, 22), p("kal raat 10 baje mummy ko call karna").dateTime)
    @Test fun hindiRelative() = assertEquals(at(26, 10, 10), p("10 मिनट बाद चाय").dateTime)

    @Test fun hindiDailyNight() {
        val r = p("रोज़ रात 10 बजे किताब पढ़ना")
        assertEquals(RepeatType.DAILY, r.repeat.type)
        assertEquals(at(26, 22), r.dateTime)
    }
}
