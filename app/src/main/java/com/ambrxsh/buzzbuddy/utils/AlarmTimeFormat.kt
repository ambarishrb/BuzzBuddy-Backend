package com.ambrxsh.buzzbuddy.utils

import android.content.Context
import com.ambrxsh.buzzbuddy.R
import java.util.Calendar

object AlarmTimeFormat {
    fun amPm(context: Context, hour24: Int): String {
        return context.getString(if (hour24 >= 12) R.string.pm else R.string.am)
    }

    fun format12Hour(context: Context, hour24: Int, minute: Int): String {
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        return context.getString(R.string.alarm_time_format, hour12, minute, amPm(context, hour24))
    }

    fun format12HourClock(context: Context, hour24: Int, minute: Int): String {
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        return context.getString(R.string.alarm_time_hhmm, hour12, minute)
    }

    fun formatMillis(context: Context, triggerAt: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = triggerAt }
        return format12Hour(
            context,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }
}
