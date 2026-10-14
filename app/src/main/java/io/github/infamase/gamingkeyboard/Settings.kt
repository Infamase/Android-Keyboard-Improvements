package io.github.infamase.gamingkeyboard

import android.content.Context
import android.content.SharedPreferences

/** User preferences shared by the settings screen and the keyboard service. */
class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var keyHeightDp: Int
        get() = prefs.getInt(KEY_HEIGHT, DEFAULT_KEY_HEIGHT_DP)
        set(value) = prefs.edit().putInt(KEY_HEIGHT, value.coerceIn(MIN_KEY_HEIGHT_DP, MAX_KEY_HEIGHT_DP)).apply()

    var haptics: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    var stickyModifiers: Boolean
        get() = prefs.getBoolean(KEY_STICKY, true)
        set(value) = prefs.edit().putBoolean(KEY_STICKY, value).apply()

    companion object {
        const val MIN_KEY_HEIGHT_DP = 24
        const val MAX_KEY_HEIGHT_DP = 64
        const val DEFAULT_KEY_HEIGHT_DP = 36

        private const val KEY_HEIGHT = "key_height_dp"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_STICKY = "sticky_modifiers"
    }
}
