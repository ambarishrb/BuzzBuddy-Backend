package com.ambrxsh.buzzbuddy.scheduler

import android.content.Context
import android.os.Build
import com.ambrxsh.buzzbuddy.model.SmartAlarm

/**
 * Enabled alarm times in device-protected storage so LOCKED_BOOT_COMPLETED
 * can re-arm alarms before the user unlocks (Room lives in credential storage).
 */
object AlarmScheduleCache {
    private const val PREFS = "buzz_alarm_cache"
    private const val KEY = "enabled_alarms"

    data class CachedAlarm(val alarmId: Int, val hour: Int, val minute: Int)

    fun save(context: Context, enabledAlarms: List<SmartAlarm>) {
        val encoded = enabledAlarms.joinToString(";") { alarm ->
            "${alarm.alarmId},${alarm.alarmTime_hour},${alarm.alarmTime_minute}"
        }
        devicePrefs(context).edit().putString(KEY, encoded).apply()
    }

    fun load(context: Context): List<CachedAlarm> {
        val raw = devicePrefs(context).getString(KEY, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(';').mapNotNull { token ->
            val parts = token.split(',')
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].toIntOrNull() ?: return@mapNotNull null
            val hour = parts[1].toIntOrNull() ?: return@mapNotNull null
            val minute = parts[2].toIntOrNull() ?: return@mapNotNull null
            CachedAlarm(id, hour, minute)
        }
    }

    private fun devicePrefs(context: Context) = deviceProtectedContext(context)
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun deviceProtectedContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.applicationContext.createDeviceProtectedStorageContext()
        } else {
            context.applicationContext
        }
    }
}
