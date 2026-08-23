package com.ambrxsh.buzzbuddy.scheduler

import java.util.Calendar

object AlarmScheduleMath {
    const val SNOOZE_REQUEST_OFFSET = 1_000_000

    fun requestCode(alarmId: Int, isSnooze: Boolean): Int {
        return if (isSnooze) alarmId + SNOOZE_REQUEST_OFFSET else alarmId
    }

    fun snoozeTriggerMillis(snoozeMinutes: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        return nowMillis + snoozeMinutes.coerceAtLeast(1) * 60_000L
    }

    fun nextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
    }

    fun nextDueMillis(
        hour: Int,
        minute: Int,
        snoozeUntil: Long?,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val daily = nextTriggerMillis(hour, minute, nowMillis)
        if (snoozeUntil != null && snoozeUntil > nowMillis) {
            return minOf(daily, snoozeUntil)
        }
        return daily
    }
}
