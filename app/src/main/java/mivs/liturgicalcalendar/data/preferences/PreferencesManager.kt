package mivs.liturgicalcalendar.data.preferences

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var areNotificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", false)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var notificationHour: Int
        get() = prefs.getInt("notification_hour", 7) // Domyślnie 7:00
        set(value) = prefs.edit().putInt("notification_hour", value).apply()

    var notificationMinute: Int
        get() = prefs.getInt("notification_minute", 0) // Domyślnie :00
        set(value) = prefs.edit().putInt("notification_minute", value).apply()
}