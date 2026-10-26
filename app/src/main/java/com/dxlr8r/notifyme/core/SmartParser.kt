package com.dxlr8r.notifyme.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Understands everyday reminder phrases in English, Hindi (Devanagari) and Hinglish, e.g.
 *  "remind me to pay electricity bill tomorrow at 6 pm"
 *  "कल शाम 6 बजे दवाई लेनी है"
 *  "har somvar subah 9 baje team meeting"
 * and splits them into a clean title + date/time + repeat rule. Works fully offline.
 */
object SmartParser {

    data class Result(
        val title: String,
        val dateTime: LocalDateTime?,
        val repeat: RepeatRule,
        /** True when a date, time or repeat was recognised in the text. */
        val found: Boolean,
    )

    // Letter/number boundaries that also work for Devanagari (plain \b is ASCII-only).
    private const val L = "(?<![\\p{L}\\p{M}\\p{N}])"
    private const val R = "(?![\\p{L}\\p{M}\\p{N}])"

    private val devanagariDigits = mapOf(
        '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
        '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9',
    )

    private val hourWords = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
        "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4, "पांच" to 5, "पाँच" to 5, "छह" to 6, "छः" to 6, "छे" to 6,
        "सात" to 7, "आठ" to 8, "नौ" to 9, "दस" to 10, "ग्यारह" to 11, "बारह" to 12,
        "ek" to 1, "do" to 2, "teen" to 3, "tin" to 3, "char" to 4, "chaar" to 4, "paanch" to 5, "panch" to 5,
        "chhe" to 6, "che" to 6, "chhah" to 6, "saat" to 7, "sat" to 7, "aath" to 8, "ath" to 8, "nau" to 9,
        "das" to 10, "gyarah" to 11, "gyara" to 11, "barah" to 12, "bara" to 12,
    )

    private val weekdayNames: List<Pair<DayOfWeek, String>> = listOf(
        DayOfWeek.MONDAY to "monday|mon|सोमवार|somvar|somwar|somvaar",
        DayOfWeek.TUESDAY to "tuesday|tues|tue|मंगलवार|mangalvar|mangalwar|mangalvaar",
        DayOfWeek.WEDNESDAY to "wednesday|wed|बुधवार|budhvar|budhwar|budhvaar",
        DayOfWeek.THURSDAY to "thursday|thurs|thu|गुरुवार|बृहस्पतिवार|guruvar|guruwar|guruvaar|brihaspativar",
        DayOfWeek.FRIDAY to "friday|fri|शुक्रवार|shukravar|shukrawar|shukravaar",
        DayOfWeek.SATURDAY to "saturday|sat|शनिवार|shanivar|shaniwar|shanivaar",
        DayOfWeek.SUNDAY to "sunday|sun|रविवार|इतवार|ravivar|raviwar|itvaar|itwar",
    )
    private val fullWeekdayNames = setOf(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
    )
    private val weekdayAlt = weekdayNames.joinToString("|") { it.second }

    private val monthNames: List<Pair<Int, String>> = listOf(
        1 to "january|jan|जनवरी", 2 to "february|feb|फ़रवरी|फरवरी", 3 to "march|mar|मार्च",
        4 to "april|apr|अप्रैल", 5 to "may|मई", 6 to "june|jun|जून", 7 to "july|jul|जुलाई",
        8 to "august|aug|अगस्त", 9 to "september|sept|sep|सितंबर|सितम्बर",
        10 to "october|oct|अक्टूबर|अक्तूबर", 11 to "november|nov|नवंबर|नवम्बर",
        12 to "december|dec|दिसंबर|दिसम्बर",
    )
    private val monthAlt = monthNames.joinToString("|") { it.second }

    private enum class Period { MORNING, AFTERNOON, EVENING, NIGHT }

    private class Ctx(val text: String, val now: LocalDateTime) {
        val lower: String = text.lowercase().let { if (it.length == text.length) it else text }
        val removed = BooleanArray(text.length)
        var date: LocalDate? = null
        var time: LocalTime? = null
        var explicitAmPm = false
        var period: Period? = null
        var absolute: LocalDateTime? = null
        var repeat: RepeatRule = RepeatRule.NONE
        var weekdayHint: DayOfWeek? = null
        var nextWeekdayExplicit = false

        fun free(range: IntRange): Boolean = range.none { removed[it] }

        fun cut(range: IntRange) {
            for (i in range) if (i in removed.indices) removed[i] = true
        }

        /** Finds every unclaimed match of [regex]; [onMatch] returns true to claim (remove) it. */
        fun scan(regex: Regex, onMatch: (MatchResult) -> Boolean) {
            for (m in regex.findAll(lower)) {
                if (m.range.isEmpty() || !free(m.range)) continue
                if (onMatch(m)) cut(m.range)
            }
        }
    }

    private fun rx(p: String) = Regex(p)

    fun parse(input: String, now: LocalDateTime = LocalDateTime.now()): Result {
        val normalized = normalize(input)
        val c = Ctx(normalized, now)

        parseRelative(c)
        parseRepeat(c)
        parseDates(c)
        parseTimes(c)
        parsePeriods(c)

        val dateTime = resolve(c)
        val title = cleanTitle(c)
        val found = dateTime != null || c.repeat.isRepeating
        return Result(
            title = title.ifBlank { input.trim() },
            dateTime = dateTime,
            repeat = c.repeat,
            found = found,
        )
    }

    // ---------------------------------------------------------------- normalisation

    private fun normalize(s: String): String {
        var t = s.map { devanagariDigits[it] ?: it }.joinToString("")
        t = t.replace(Regex("(?i)a\\.\\s?m\\.?"), "am ").replace(Regex("(?i)p\\.\\s?m\\.?"), "pm ")
        t = t.replace('’', '\'').replace(Regex("\\s+"), " ").trim()
        // "six pm" / "at seven" / "सात बजे" → digits
        t = Regex("(?i)$L(${hourWords.keys.joinToString("|")})(?=\\s*(am|pm|o'?clock|बजे|baje|bje)$R)")
            .replace(t) { hourWords[it.value.lowercase()]?.toString() ?: it.value }
        t = Regex("(?i)(${L}at\\s+)(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)$R")
            .replace(t) { it.groupValues[1] + (hourWords[it.groupValues[2].lowercase()] ?: 0) }
        return t
    }

    // ---------------------------------------------------------------- relative ("in 10 minutes")

    private fun parseRelative(c: Ctx) {
        val en = rx("$L(in|after)\\s+(\\d+|a|an|one|half an|half a)\\s*(minutes|minute|mins|min|hours|hour|hrs|hr|days|day)$R")
        c.scan(en) { m ->
            val amt = m.groupValues[2]
            val unit = m.groupValues[3]
            val minutes = when {
                amt.startsWith("half") -> 30L
                unit.startsWith("min") -> (amt.toLongOrNull() ?: 1L)
                unit.startsWith("h") -> (amt.toLongOrNull() ?: 1L) * 60
                else -> (amt.toLongOrNull() ?: 1L) * 1440
            }
            c.absolute = c.now.plusMinutes(minutes).plusSeconds(59).withSecond(0).withNano(0)
            true
        }
        val hi = rx("$L(\\d+|आधे|aadhe|adhe|aadha|ek)\\s*(मिनट|minute|minutes|min|घंटे|घंटा|ghante|ghanta|दिन|din)\\s*(में|मे|बाद|mein|main|me|baad|bad)$R")
        c.scan(hi) { m ->
            val amtRaw = m.groupValues[1]
            val unit = m.groupValues[2]
            val half = amtRaw in setOf("आधे", "aadhe", "adhe", "aadha")
            val amt = if (amtRaw == "ek") 1L else amtRaw.toLongOrNull() ?: 1L
            val minutes = when {
                unit in setOf("मिनट", "minute", "minutes", "min") -> amt
                unit in setOf("घंटे", "घंटा", "ghante", "ghanta") -> if (half) 30 else amt * 60
                else -> amt * 1440
            }
            c.absolute = c.now.plusMinutes(minutes).plusSeconds(59).withSecond(0).withNano(0)
            true
        }
    }

    // ---------------------------------------------------------------- repeat

    private fun parseRepeat(c: Ctx) {
        // every 3 days / every 2 weeks / हर 2 दिन
        c.scan(rx("$L(every|हर|har)\\s+(\\d+)\\s*(days|day|दिन|din|weeks|week|हफ्ते|हफ़्ते|hafte)$R")) { m ->
            val n = m.groupValues[2].toInt().coerceAtLeast(1)
            val isWeek = m.groupValues[3].let { it.startsWith("week") || it.startsWith("हफ") || it == "hafte" }
            c.repeat = if (isWeek) RepeatRule(RepeatType.CUSTOM_WEEKS, interval = n)
            else RepeatRule(RepeatType.CUSTOM_DAYS, interval = n)
            true
        }
        // every monday and thursday / हर सोमवार
        c.scan(rx("$L(every|हर|har)\\s+(($weekdayAlt)(\\s*(,|and|&|और|aur)\\s*($weekdayAlt))*)\\s*(को|ko)?$R")) { m ->
            val days = Regex("$L($weekdayAlt)$R").findAll(m.groupValues[2]).mapNotNull { weekdayOf(it.value) }.toList()
            if (days.isEmpty()) return@scan false
            c.repeat = RepeatRule(RepeatType.WEEKLY, weekDays = RepeatRule.maskOf(*days.toTypedArray()))
            true
        }
        c.scan(rx("$L(every\\s+weekdays?|on\\s+weekdays|weekdays|सोमवार से शुक्रवार|monday to friday)$R")) {
            c.repeat = RepeatRule(RepeatType.WEEKDAYS); true
        }
        c.scan(rx("$L(every\\s*day|everyday|daily|each day|every single day|रोज़ाना|रोजाना|रोज़|रोज|हर\\s*दिन|हर\\s*रोज़?|har\\s*din|har\\s*roz|roz(ana)?|roj(ana)?|daily)$R")) {
            c.repeat = RepeatRule(RepeatType.DAILY); true
        }
        c.scan(rx("$L(every\\s*week|weekly|each week|हर\\s*(हफ्ते|हफ़्ते|सप्ताह)|har\\s*(hafte|hafta))$R")) {
            c.repeat = RepeatRule(RepeatType.WEEKLY); true
        }
        c.scan(rx("$L(every\\s*month|monthly|each month|हर\\s*(महीने|महीना)|har\\s*(mahine|mahina))$R")) {
            c.repeat = RepeatRule(RepeatType.MONTHLY); true
        }
        c.scan(rx("$L(every\\s*year|yearly|annually|each year|हर\\s*(साल|वर्ष)|har\\s*(saal|sal))$R")) {
            c.repeat = RepeatRule(RepeatType.YEARLY); true
        }
        // "every morning" / "har subah" → daily, leave the period word for time parsing
        c.scan(rx("$L(every|हर|har)(?=\\s+(morning|evening|night|afternoon|सुबह|शाम|रात|दोपहर|subah|shaam|sham|raat|dopahar)$R)")) {
            if (c.repeat.isRepeating) return@scan false
            c.repeat = RepeatRule(RepeatType.DAILY); true
        }
    }

    private fun weekdayOf(word: String): DayOfWeek? {
        val w = word.lowercase()
        return weekdayNames.firstOrNull { (_, alt) -> alt.split("|").contains(w) }?.first
    }

    // ---------------------------------------------------------------- dates

    private fun parseDates(c: Ctx) {
        val today = c.now.toLocalDate()
        c.scan(rx("$L(day after tomorrow|परसों|परसो|parso|parson|parsoon)$R")) { c.date = today.plusDays(2); true }
        c.scan(rx("$L(tomorrow|tmrw|tmr|कल|kal)$R")) { c.date = today.plusDays(1); true }
        c.scan(rx("$L(tonight)$R")) { c.date = today; c.period = Period.NIGHT; true }
        c.scan(rx("$L(today|आज|aaj)$R")) { c.date = today; true }
        c.scan(rx("$L(next week|अगले हफ्ते|अगले हफ़्ते|agle hafte)$R")) {
            c.date = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)); true
        }

        // 5th november, 5 nov 2027, 5 नवंबर
        c.scan(rx("$L(\\d{1,2})(st|nd|rd|th)?\\s*(of\\s+)?($monthAlt)$R(\\s*,?\\s*(\\d{4}))?")) { m ->
            setDate(c, m.groupValues[1].toInt(), monthOf(m.groupValues[4]), m.groupValues[6].toIntOrNull())
        }
        // november 5, nov 5th 2027
        c.scan(rx("$L($monthAlt)\\s+(\\d{1,2})(st|nd|rd|th)?$R(\\s*,?\\s*(\\d{4}))?")) { m ->
            setDate(c, m.groupValues[2].toInt(), monthOf(m.groupValues[1]), m.groupValues[5].toIntOrNull())
        }
        // 25/12, 25-12-2026 (day first, as used in India)
        c.scan(rx("$L(\\d{1,2})[/-](\\d{1,2})([/-](\\d{2,4}))?$R")) { m ->
            val y = m.groupValues[4].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
            setDate(c, m.groupValues[1].toInt(), m.groupValues[2].toInt(), y)
        }
        // on the 5th / 5 तारीख / 5 tarikh
        c.scan(rx("$L(on\\s+(the\\s+)?(\\d{1,2})(st|nd|rd|th)|(\\d{1,2})\\s*(तारीख|tarikh|tareekh|tarik)(\\s*(को|ko))?)$R")) { m ->
            val d = (m.groupValues[3].ifEmpty { m.groupValues[5] }).toIntOrNull() ?: return@scan false
            if (d !in 1..31) return@scan false
            var date = safeDate(today.year, today.monthValue, d) ?: return@scan false
            if (date.isBefore(today)) date = safeDate(today.plusMonths(1).year, today.plusMonths(1).monthValue, d) ?: return@scan false
            c.date = date
            true
        }

        // weekdays: "on monday", "next friday", "सोमवार को", "agle somvar"
        c.scan(rx("$L((on|next|this|coming|अगले|agle|is)\\s+)?($weekdayAlt)(\\s+(को|ko))?$R")) { m ->
            val word = m.groupValues[3]
            val prefix = m.groupValues[2]
            val isAbbrev = word in setOf("mon", "tue", "tues", "wed", "thu", "thurs", "fri", "sat", "sun")
            if (isAbbrev && prefix.isEmpty()) return@scan false
            if (word == "sat" && prefix.isEmpty()) return@scan false
            val day = weekdayOf(word) ?: return@scan false
            if (c.repeat.isRepeating && c.repeat.type == RepeatType.WEEKLY) {
                c.weekdayHint = day; return@scan true
            }
            c.weekdayHint = day
            c.nextWeekdayExplicit = prefix in setOf("next", "अगले", "agle")
            true
        }
        // Full weekday names with no prefix are handled above; abbreviations need a prefix.
    }

    private fun setDate(c: Ctx, day: Int, month: Int?, year: Int?): Boolean {
        if (month == null || month !in 1..12 || day !in 1..31) return false
        val today = c.now.toLocalDate()
        var date = safeDate(year ?: today.year, month, day) ?: return false
        if (year == null && date.isBefore(today)) date = safeDate(today.year + 1, month, day) ?: return false
        c.date = date
        return true
    }

    private fun safeDate(y: Int, m: Int, d: Int): LocalDate? = runCatching { LocalDate.of(y, m, d) }.getOrNull()

    private fun monthOf(word: String): Int? {
        val w = word.lowercase()
        return monthNames.firstOrNull { (_, alt) -> alt.split("|").contains(w) }?.first
    }

    // ---------------------------------------------------------------- times

    private fun parseTimes(c: Ctx) {
        // साढ़े 6 / sadhe 6 → 6:30 ; सवा 6 → 6:15 ; पौने 6 → 5:45 ; डेढ़ → 1:30 ; ढाई → 2:30
        c.scan(rx("$L(साढ़े|साढे|sadhe|saadhe|sade|saade)\\s*(\\d{1,2})(\\s*(बजे|baje|bje))?$R")) { m ->
            setTime(c, m.groupValues[2].toInt(), 30, null)
        }
        c.scan(rx("$L(सवा|sava|savva|sawa)\\s*(\\d{1,2})(\\s*(बजे|baje|bje))?$R")) { m ->
            setTime(c, m.groupValues[2].toInt(), 15, null)
        }
        c.scan(rx("$L(पौने|paune|pone|paune)\\s*(\\d{1,2})(\\s*(बजे|baje|bje))?$R")) { m ->
            val h = m.groupValues[2].toInt()
            setTime(c, if (h == 1) 12 else h - 1, 45, null)
        }
        c.scan(rx("$L(डेढ़|डेढ|dedh|dhedh)(\\s*(बजे|baje|bje))$R")) { setTime(c, 1, 30, null) }
        c.scan(rx("$L(ढाई|dhai|dhaai)(\\s*(बजे|baje|bje))$R")) { setTime(c, 2, 30, null) }

        // 6 बजकर 30 मिनट
        c.scan(rx("$L(\\d{1,2})\\s*(बजकर|bajkar|baj kar)\\s*(\\d{1,2})\\s*(मिनट|minute|min)?(\\s*(पर|par))?$R")) { m ->
            setTime(c, m.groupValues[1].toInt(), m.groupValues[3].toInt(), null)
        }
        // 6:30 pm, at 18.45, @ 7:05am
        c.scan(rx("$L((at|by|@)\\s*)?(\\d{1,2})[:.](\\d{2})\\s*(am|pm)?(\\s*(बजे|baje|bje))?$R")) { m ->
            setTime(c, m.groupValues[3].toInt(), m.groupValues[4].toInt(), m.groupValues[5].ifEmpty { null })
        }
        // 6 pm, at 7am
        c.scan(rx("$L((at|by|@)\\s*)?(\\d{1,2})\\s*(am|pm)$R")) { m ->
            setTime(c, m.groupValues[3].toInt(), 0, m.groupValues[4])
        }
        // 6 बजे, 6 baje, 7 o'clock
        c.scan(rx("$L((at|by|@)\\s*)?(\\d{1,2})\\s*(बजे|baje|bje|o'?clock)(\\s*(तक|tak))?$R")) { m ->
            setTime(c, m.groupValues[3].toInt(), 0, null)
        }
        // at 6
        c.scan(rx("$L(at|@)\\s*(\\d{1,2})$R(?!\\s*[/-])")) { m ->
            setTime(c, m.groupValues[2].toInt(), 0, null)
        }
        c.scan(rx("$L(noon|midday|दोपहर 12 बजे)$R")) { setTime(c, 12, 0, "pm") }
        c.scan(rx("$L(midnight|आधी रात|aadhi raat)$R")) { setTime(c, 0, 0, "am") }
    }

    private fun setTime(c: Ctx, hour: Int, minute: Int, ampm: String?): Boolean {
        if (c.time != null || c.absolute != null) return false
        if (minute !in 0..59) return false
        var h = hour
        when (ampm) {
            "am" -> { if (h !in 1..12) return false; if (h == 12) h = 0; c.explicitAmPm = true }
            "pm" -> { if (h !in 1..12) return false; if (h != 12) h += 12; c.explicitAmPm = true }
            else -> if (h !in 0..23) return false else if (h >= 13 || h == 0) c.explicitAmPm = true
        }
        c.time = LocalTime.of(h, minute)
        return true
    }

    private fun parsePeriods(c: Ctx) {
        val p = listOf(
            Period.MORNING to "(in the |this |tomorrow )?(morning)|सुबह|सवेरे|subah|subha|savere",
            Period.AFTERNOON to "(in the |this )?(afternoon)|दोपहर|dopahar|dopehar|dophar",
            Period.EVENING to "(in the |this )?(evening)|शाम|shaam|sham",
            Period.NIGHT to "(at |in the )?(night)|रात|raat|rat",
        )
        for ((period, alt) in p) {
            c.scan(rx("$L($alt)(\\s+(को|ko|में|mein|me))?$R")) {
                if (c.period == null) c.period = period
                true
            }
        }
    }

    // ---------------------------------------------------------------- resolution

    private fun resolve(c: Ctx): LocalDateTime? {
        c.absolute?.let { return it }
        val now = c.now
        val today = now.toLocalDate()
        val period = c.period

        var time = c.time
        if (time != null && !c.explicitAmPm && time.hour in 1..12) {
            time = applyPeriod(time, period)
        } else if (time == null && period != null) {
            time = when (period) {
                Period.MORNING -> LocalTime.of(9, 0)
                Period.AFTERNOON -> LocalTime.of(14, 0)
                Period.EVENING -> LocalTime.of(18, 0)
                Period.NIGHT -> LocalTime.of(21, 0)
            }
        }

        var date = c.date
        if (date == null && c.weekdayHint != null) {
            val wd = c.weekdayHint!!
            date = if (wd == today.dayOfWeek && !c.nextWeekdayExplicit) {
                val t = time ?: LocalTime.of(9, 0)
                if (today.atTime(t).isAfter(now)) today else today.plusWeeks(1)
            } else {
                today.with(TemporalAdjusters.next(wd))
            }
        }
        if (c.repeat.type == RepeatType.WEEKLY && c.repeat.weekDays != 0 && date == null) {
            val t = time ?: LocalTime.of(9, 0)
            date = (0L..7L).map { today.plusDays(it) }.first {
                (c.repeat.weekDays and RepeatRule.bit(it.dayOfWeek)) != 0 && it.atTime(t).isAfter(now)
            }
        }
        if (c.repeat.type == RepeatType.WEEKDAYS && date == null) {
            val t = time ?: LocalTime.of(9, 0)
            date = (0L..7L).map { today.plusDays(it) }.first {
                it.dayOfWeek.value <= 5 && it.atTime(t).isAfter(now)
            }
        }

        if (time == null && date == null && !c.repeat.isRepeating) return null

        if (time != null && !c.explicitAmPm && period == null && time.hour in 1..11) {
            // "at 6" — choose the sensible half of the day.
            val am = time
            val pm = time.plusHours(12)
            val d = date ?: today
            time = when {
                am.hour <= 6 -> pm // nobody means 3 am unless they say so
                d != today -> am
                today.atTime(am).isAfter(now) -> am
                today.atTime(pm).isAfter(now) -> pm
                else -> am // tomorrow morning
            }
        }

        val t = time ?: LocalTime.of(9, 0)
        if (date == null) {
            date = if (today.atTime(t).isAfter(now)) today else today.plusDays(1)
        }
        return date.atTime(t)
    }

    private fun applyPeriod(t: LocalTime, period: Period?): LocalTime = when (period) {
        null -> t
        Period.MORNING -> if (t.hour == 12) t.withHour(0) else t
        Period.AFTERNOON -> if (t.hour in 1..6) t.plusHours(12) else t
        Period.EVENING -> if (t.hour in 1..11) t.plusHours(12) else t
        Period.NIGHT -> when (t.hour) {
            12 -> t.withHour(0)
            in 5..11 -> t.plusHours(12)
            else -> t
        }
    }

    // ---------------------------------------------------------------- title

    private val leadingFiller = Regex(
        "(?i)^\\s*(please\\s+|pls\\s+|plz\\s+|hey\\s+|ok\\s+)*" +
            "(remind me (to|that|about|of)?|reminder (to|for|of)?|set (a |an )?(reminder|alarm) (to|for)?|" +
            "remember to|don'?t forget to|i need to|i have to|i've to|need to|have to)\\s+",
    )
    private val hindiFiller = Regex(
        "$L(मुझे|mujhe|muje|याद दिलाना|याद दिला देना|याद दिलाना है|याद दिला दो|याद दिलाएं|याद दिलाइए|" +
            "yaad dilana|yaad dila dena|yaad dila do|yad dilana|का रिमाइंडर|की रिमाइंडर|रिमाइंडर|reminder lagao|" +
            "reminder set karo|reminder|lagana|लगाना|लगा दो|ka|का|ki|की|है|hai)$R",
    )
    private val danglingWords = setOf(
        "at", "on", "by", "for", "to", "the", "in", "and", "of", "is", "से", "को", "ko", "se", "पर", "par", "on the",
        "this", "me", "में", "mein", "around", "about", "from",
    )

    private fun cleanTitle(c: Ctx): String {
        val sb = StringBuilder()
        for (i in c.text.indices) sb.append(if (c.removed[i]) ' ' else c.text[i])
        var t = sb.toString().replace(Regex("\\s+"), " ").trim()
        t = leadingFiller.replace(t, "")
        if (Regex("[\\u0900-\\u097F]|(?i)${L}(mujhe|yaad|reminder|hai|ka|ki)$R").containsMatchIn(t)) {
            // only strip Hindi fillers when there is something else left
            val stripped = hindiFiller.replace(t, " ").replace(Regex("\\s+"), " ").trim()
            if (stripped.isNotBlank()) t = stripped
        }
        // trim dangling connector words and punctuation at both ends
        var changed = true
        while (changed && t.isNotEmpty()) {
            changed = false
            val words = t.split(" ")
            if (words.size > 1 && words.last().lowercase().trim(',', '.', '!', '?') in danglingWords) {
                t = words.dropLast(1).joinToString(" "); changed = true
            }
            val w2 = t.split(" ")
            if (w2.size > 1 && w2.first().lowercase() in danglingWords) {
                t = w2.drop(1).joinToString(" "); changed = true
            }
            val trimmed = t.trim(' ', ',', '.', '!', '?', '-', ':', '।')
            if (trimmed != t) { t = trimmed; changed = true }
        }
        return t.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
