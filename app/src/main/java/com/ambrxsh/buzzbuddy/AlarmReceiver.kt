package com.ambrxsh.buzzbuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ambrxsh.buzzbuddy.scheduler.AlarmRescheduler
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler
import com.ambrxsh.buzzbuddy.services.BuzzBuddyAlarmForegroundService
import com.ambrxsh.buzzbuddy.utils.SnoozeManager
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP_ALARM = "com.ambrxsh.buzzbuddy.STOP_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, -1)
        Timber.d("AlarmReceiver action=%s alarmId=%s", intent.action, alarmId)

        when (intent.action) {
            ACTION_STOP_ALARM -> {
                AlarmPlayer.stop()
                BuzzBuddyAlarmForegroundService.stop(context)
            }

            SnoozeManager.ACTION_CANCEL_SNOOZE -> {
                SnoozeManager.get(context).cancelSnooze(alarmId)
            }

            else -> {
                val isSnooze = intent.getBooleanExtra(BuzzBuddyAlarmScheduler.EXTRA_IS_SNOOZE, false)
                if (!isSnooze) {
                    val hour = intent.getIntExtra(BuzzBuddyAlarmScheduler.EXTRA_HOUR, -1)
                    val minute = intent.getIntExtra(BuzzBuddyAlarmScheduler.EXTRA_MINUTE, -1)
                    if (hour >= 0 && minute >= 0) {
                        BuzzBuddyAlarmScheduler(context).schedule(alarmId, hour, minute)
                    }
                }
                if (AlarmRescheduler.isUserUnlocked(context)) {
                    SnoozeManager.get(context).clearSnooze(alarmId)
                }
                BuzzBuddyAlarmForegroundService.start(context, alarmId)
            }
        }
    }
}
