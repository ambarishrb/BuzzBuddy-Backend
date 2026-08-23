package com.ambrxsh.buzzbuddy.scheduler

import android.content.Context
import android.os.Build
import android.os.UserManager
import com.ambrxsh.buzzbuddy.room.SmartAlarmsDatabase
import com.ambrxsh.buzzbuddy.utils.SnoozeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

object AlarmRescheduler {

    fun restoreAsync(
        context: Context,
        pendingResult: android.content.BroadcastReceiver.PendingResult? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAll(context)
            } catch (e: Exception) {
                Timber.e(e, "Failed to reschedule alarms")
            } finally {
                pendingResult?.finish()
            }
        }
    }

    fun rescheduleAll(context: Context) {
        val app = context.applicationContext
        val scheduler = BuzzBuddyAlarmScheduler(app)
        val cached = AlarmScheduleCache.load(app)
        cached.forEach { entry ->
            scheduler.schedule(entry.alarmId, entry.hour, entry.minute)
        }
        Timber.d("Rescheduled %s cached alarms (direct boot safe)", cached.size)

        if (!isUserUnlocked(app)) return

        try {
            val alarms = SmartAlarmsDatabase.getDatabase(app).smartAlarmDao().getAllAlarmsSync()
            AlarmScheduleCache.save(app, alarms.filter { it.isEnabled })

            val snoozes = SnoozeManager.get(app).snapshot()
            val now = System.currentTimeMillis()

            for (alarm in alarms) {
                if (!alarm.isEnabled) {
                    scheduler.cancel(alarm.alarmId)
                    continue
                }
                scheduler.schedule(alarm.alarmId, alarm.alarmTime_hour, alarm.alarmTime_minute)
                val snoozeUntil = snoozes[alarm.alarmId]
                if (snoozeUntil != null && snoozeUntil > now) {
                    scheduler.scheduleSnoozeAt(alarm.alarmId, snoozeUntil)
                }
            }
            Timber.d("Rescheduled %s alarms from Room", alarms.size)
        } catch (e: Exception) {
            Timber.e(e, "Credential storage not ready; kept device-protected cache schedules")
        }
    }

    fun isUserUnlocked(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        return userManager?.isUserUnlocked ?: true
    }
}
