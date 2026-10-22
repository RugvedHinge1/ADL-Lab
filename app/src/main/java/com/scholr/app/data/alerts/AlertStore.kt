package com.scholr.app.data.alerts

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * On-device alert state: the current alert list, which ones were read, which
 * ones already produced a system notification, and the user's on/off switch.
 * Shared by the app UI and the background worker, hence plain SharedPreferences.
 */
class AlertStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("scholr_alerts", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(AppAlert.serializer())

    var alerts: List<AppAlert>
        get() = prefs.getString(KEY_ALERTS, null)
            ?.let { runCatching { json.decodeFromString(listSerializer, it) }.getOrNull() }
            .orEmpty()
        set(value) = prefs.edit().putString(KEY_ALERTS, json.encodeToString(listSerializer, value)).apply()

    var generatedAt: Long
        get() = prefs.getLong(KEY_GENERATED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_GENERATED_AT, value).apply()

    var readIds: Set<String>
        get() = prefs.getStringSet(KEY_READ, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_READ, value).apply()

    var notifiedIds: Set<String>
        get() = prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().toSet()
        set(value) = prefs.edit().putStringSet(KEY_NOTIFIED, value).apply()

    /** Master switch for system notifications (the in-app list always works). */
    var pushEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUSH, true)
        set(value) = prefs.edit().putBoolean(KEY_PUSH, value).apply()

    var permissionAsked: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    private companion object {
        const val KEY_ALERTS = "alerts"
        const val KEY_GENERATED_AT = "generated_at"
        const val KEY_READ = "read_ids"
        const val KEY_NOTIFIED = "notified_ids"
        const val KEY_PUSH = "push_enabled"
        const val KEY_ASKED = "permission_asked"
    }
}
