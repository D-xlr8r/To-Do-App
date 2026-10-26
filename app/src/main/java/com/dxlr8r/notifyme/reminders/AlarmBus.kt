package com.dxlr8r.notifyme.reminders

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Lets the full-screen alarm close itself when the alarm is handled from the notification. */
object AlarmBus {
    private val _dismissed = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val dismissed: SharedFlow<Long> = _dismissed

    fun dismiss(id: Long) {
        _dismissed.tryEmit(id)
    }
}
