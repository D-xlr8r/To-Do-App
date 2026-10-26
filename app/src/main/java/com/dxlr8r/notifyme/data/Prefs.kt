package com.dxlr8r.notifyme.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** BCP-47 tag used for voice input: "en-IN" or "hi-IN". */
    var voiceLanguage: String
        get() = sp.getString("voice_lang", "en-IN") ?: "en-IN"
        set(v) = sp.edit().putString("voice_lang", v).apply()

    var batteryGuideShown: Boolean
        get() = sp.getBoolean("battery_guide_shown", false)
        set(v) = sp.edit().putBoolean("battery_guide_shown", v).apply()

    var completedExpanded: Boolean
        get() = sp.getBoolean("completed_expanded", false)
        set(v) = sp.edit().putBoolean("completed_expanded", v).apply()

    var focusMinutes: Int
        get() = sp.getInt("focus_minutes", 25)
        set(v) = sp.edit().putInt("focus_minutes", v).apply()
}
