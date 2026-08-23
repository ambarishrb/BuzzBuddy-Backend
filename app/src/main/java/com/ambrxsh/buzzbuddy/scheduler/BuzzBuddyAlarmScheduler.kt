package com.ambrxsh.buzzbuddy.scheduler

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.ambrxsh.buzzbuddy.AlarmReceiver
import com.ambrxsh.buzzbuddy.model.MainActivity
import timber.log.Timber
import java.util.Calendar

class BuzzBuddyAlarmScheduler(private val context: Context) {

    companion object {
        const val EXTRA_ALARM_ID = "alarmId"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val EXTRA_IS_SNOOZE = "isSnooze"

        const val SNOOZE_REQUEST_OFFSET = AlarmScheduleMath.SNOOZE_REQUEST_OFFSET

        fun requestCode(alarmId: Int, isSnooze: Boolean) = AlarmScheduleMath.requestCode(alarmId, isSnooze)

        fun snoozeTriggerMillis(snoozeMinutes: Int, nowMillis: Long = System.currentTimeMillis()) =
            AlarmScheduleMath.snoozeTriggerMillis(snoozeMinutes, nowMillis)

        fun nextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()) =
            AlarmScheduleMath.nextTriggerMillis(hour, minute, nowMillis)
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(alarmId: Int, hour: Int, minute: Int): Boolean {
        return scheduleAt(alarmId, nextTriggerMillis(hour, minute), hour, minute, isSnooze = false)
    }

    fun scheduleSnooze(alarmId: Int, snoozeMinutes: Int): Long? {
        val triggerAt = snoozeTriggerMillis(snoozeMinutes)
        return triggerAt.takeIf { scheduleSnoozeAt(alarmId, triggerAt) }
    }

    fun scheduleSnoozeAt(alarmId: Int, triggerAt: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = triggerAt }
        return scheduleAt(
            alarmId,
            triggerAt,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            isSnooze = true
        )
    }

    fun cancelSnooze(alarmId: Int) {
        cancelPendingIntent(requestCode(alarmId, isSnooze = true))
    }

    fun cancel(alarmId: Int) {
        cancelPendingIntent(requestCode(alarmId, isSnooze = false))
        cancelPendingIntent(requestCode(alarmId, isSnooze = true))
    }

    fun openExactAlarmSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${activity.packageName}".toUri()
            }
            activity.startActivity(intent)
        }
    }

    private fun scheduleAt(
        alarmId: Int,
        triggerAt: Long,
        hour: Int,
        minute: Int,
        isSnooze: Boolean
    ): Boolean {
        if (!canScheduleExactAlarms()) {
            Timber.w("Cannot schedule exact alarms; permission missing")
            return false
        }

        val operation = pendingIntent(alarmId, hour, minute, isSnooze)
        return try {
            val showIntent = PendingIntent.getActivity(
                context,
                requestCode(alarmId, isSnooze),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                operation
            )
            Timber.d(
                "Scheduled alarmId=%s snooze=%s at %s",
                alarmId,
                isSnooze,
                triggerAt
            )
            true
        } catch (e: SecurityException) {
            Timber.e(e, "Exact alarm scheduling denied")
            false
        }
    }

    private fun pendingIntent(
        alarmId: Int,
        hour: Int,
        minute: Int,
        isSnooze: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(alarmId, isSnooze),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPendingIntent(requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
