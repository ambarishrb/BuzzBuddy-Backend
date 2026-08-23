package com.ambrxsh.buzzbuddy.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.model.MainActivity
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler

class SnoozeManager(context: Context) {

    companion object {
        const val ACTION_CANCEL_SNOOZE = "com.ambrxsh.buzzbuddy.CANCEL_SNOOZE"
        private const val PREFS_NAME = "buzz_snooze"
        private const val KEY_PREFIX = "until_"
        private const val CHANNEL_ID = "snooze_status_channel"
        private const val NOTIFICATION_BASE_ID = 3000

        @Volatile
        private var instance: SnoozeManager? = null

        fun get(context: Context): SnoozeManager {
            return instance ?: synchronized(this) {
                instance ?: SnoozeManager(context.applicationContext).also { instance = it }
            }
        }

        fun notificationId(alarmId: Int) = NOTIFICATION_BASE_ID + alarmId
    }

    private val appContext = context.applicationContext
    private val prefs = deviceProtectedContext(appContext)
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _activeSnoozes = MutableLiveData(loadAll())
    val activeSnoozes: LiveData<Map<Int, Long>> = _activeSnoozes

    fun snoozeUntil(alarmId: Int): Long? = activeSnoozes.value?.get(alarmId)

    fun snapshot(): Map<Int, Long> = loadAll()

    fun markSnoozed(alarmId: Int, triggerAt: Long) {
        prefs.edit().putLong("$KEY_PREFIX$alarmId", triggerAt).apply()
        publish()
        showNotification(alarmId, triggerAt)
    }

    fun cancelSnooze(alarmId: Int) {
        BuzzBuddyAlarmScheduler(appContext).cancelSnooze(alarmId)
        clearSnooze(alarmId)
    }

    fun clearSnooze(alarmId: Int) {
        prefs.edit().remove("$KEY_PREFIX$alarmId").apply()
        notificationManager.cancel(notificationId(alarmId))
        publish()
    }

    private fun loadAll(): Map<Int, Long> {
        val now = System.currentTimeMillis()
        val result = mutableMapOf<Int, Long>()
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX) || value !is Long) return@forEach
            val alarmId = key.removePrefix(KEY_PREFIX).toIntOrNull() ?: return@forEach
            if (value > now) {
                result[alarmId] = value
            } else {
                prefs.edit().remove(key).apply()
            }
        }
        return result
    }

    private fun publish() {
        _activeSnoozes.postValue(loadAll())
    }

    private fun showNotification(alarmId: Int, triggerAt: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.snooze_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = appContext.getString(R.string.snooze_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            appContext,
            alarmId,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(appContext, com.ambrxsh.buzzbuddy.AlarmReceiver::class.java).apply {
            action = ACTION_CANCEL_SNOOZE
            putExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationId(alarmId),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appContext.getString(R.string.snooze_notification_title))
            .setContentText(appContext.getString(R.string.snooze_notification_text, formatTime(triggerAt)))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                appContext.getString(R.string.stop_snooze),
                cancelPendingIntent
            )
            .build()

        notificationManager.notify(notificationId(alarmId), notification)
    }

    private fun formatTime(triggerAt: Long): String {
        return AlarmTimeFormat.formatMillis(appContext, triggerAt)
    }

    private fun deviceProtectedContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
    }
}
