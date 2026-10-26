package com.dxlr8r.notifyme.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dxlr8r.notifyme.core.EndType
import com.dxlr8r.notifyme.core.RepeatRule
import com.dxlr8r.notifyme.core.RepeatType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One row for both kinds of entries:
 *  - [KIND_TASK]  lives on the Tasks tab; reminder optional.
 *  - [KIND_EVENT] lives on the Calendar tab; always has a date/time.
 */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: Int = KIND_TASK,
    val title: String,
    val priority: Int = PRIORITY_NONE,
    val done: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** First occurrence of the reminder as local date-time (ISO), null = no reminder. */
    val anchor: String? = null,
    /** Next pending trigger in epoch millis, null when nothing is scheduled. */
    val nextAt: Long? = null,
    /** Pending snooze trigger in epoch millis. */
    val snoozeAt: Long? = null,
    /** true = ringing full-screen alarm, false = normal notification. */
    val alarm: Boolean = false,
    val repeatType: String = RepeatType.NONE.name,
    val repeatInterval: Int = 1,
    val repeatDays: Int = 0,
    val endType: String = EndType.NEVER.name,
    val endDate: Long? = null,
    val endCount: Int = 0,
) {
    val isTask get() = kind == KIND_TASK
    val hasReminder get() = anchor != null

    val anchorDateTime: LocalDateTime?
        get() = anchor?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    val rule: RepeatRule
        get() = RepeatRule(
            type = runCatching { RepeatType.valueOf(repeatType) }.getOrDefault(RepeatType.NONE),
            interval = repeatInterval,
            weekDays = repeatDays,
            endType = runCatching { EndType.valueOf(endType) }.getOrDefault(EndType.NEVER),
            endDate = endDate?.let { LocalDate.ofEpochDay(it) },
            count = endCount,
        )

    fun withReminder(at: LocalDateTime?, rule: RepeatRule, alarm: Boolean): Item = copy(
        anchor = at?.withSecond(0)?.withNano(0)?.toString(),
        alarm = alarm,
        repeatType = rule.type.name,
        repeatInterval = rule.interval,
        repeatDays = rule.weekDays,
        endType = rule.endType.name,
        endDate = rule.endDate?.toEpochDay(),
        endCount = rule.count,
        snoozeAt = null,
    )

    fun withoutReminder(): Item = withReminder(null, RepeatRule.NONE, false).copy(nextAt = null)

    companion object {
        const val KIND_TASK = 0
        const val KIND_EVENT = 1

        const val PRIORITY_NONE = 0
        const val PRIORITY_LOW = 1
        const val PRIORITY_MEDIUM = 2
        const val PRIORITY_HIGH = 3
    }
}

fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), ZoneId.systemDefault())
