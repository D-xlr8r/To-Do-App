package com.dxlr8r.notifyme

import android.app.Application
import com.dxlr8r.notifyme.data.Repository
import com.dxlr8r.notifyme.reminders.Notifications
import com.dxlr8r.notifyme.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotifyApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        WidgetUpdater.scheduleMidnight(this)
        // Cheap safety net: make sure every reminder is registered with the system.
        scope.launch { runCatching { Repository.get(this@NotifyApp).rescheduleAll(showMissed = false) } }
    }
}
