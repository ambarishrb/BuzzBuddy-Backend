package com.ambrxsh.buzzbuddy.utils

import android.content.Context
import com.ambrxsh.buzzbuddy.model.SettingsData
import com.ambrxsh.buzzbuddy.scheduler.AlarmRescheduler
import com.google.gson.Gson

class SettingsManager(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()

    fun saveSettings(settings: SettingsData) {
        val json = gson.toJson(settings)
        devicePrefs().edit().putString(KEY, json).apply()
        if (AlarmRescheduler.isUserUnlocked(appContext)) {
            try {
                credentialPrefs().edit().putString(KEY, json).apply()
            } catch (_: Exception) {
            }
        }
    }

    fun loadSettings(): SettingsData {
        migrateToDeviceProtected()
        val json = try {
            if (AlarmRescheduler.isUserUnlocked(appContext)) {
                credentialPrefs().getString(KEY, null) ?: devicePrefs().getString(KEY, null)
            } else {
                devicePrefs().getString(KEY, null)
            }
        } catch (_: Exception) {
            devicePrefs().getString(KEY, null)
        }
        return if (json != null) {
            gson.fromJson(json, SettingsData::class.java) ?: SettingsData()
        } else {
            SettingsData()
        }
    }

    private fun migrateToDeviceProtected() {
        if (!AlarmRescheduler.isUserUnlocked(appContext)) return
        try {
            val existing = devicePrefs().getString(KEY, null)
            if (existing != null) return
            val fromCredential = credentialPrefs().getString(KEY, null) ?: return
            devicePrefs().edit().putString(KEY, fromCredential).apply()
        } catch (_: Exception) {
        }
    }

    private fun devicePrefs() = deviceProtectedContext()
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun credentialPrefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun deviceProtectedContext(): Context {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            appContext.createDeviceProtectedStorageContext()
        } else {
            appContext
        }
    }

    companion object {
        private const val PREFS = "buzz_settings"
        private const val KEY = "settings_json"
    }
}
