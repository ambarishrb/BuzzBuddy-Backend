package com.ambrxsh.buzzbuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ambrxsh.buzzbuddy.scheduler.AlarmRescheduler
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isRestoreAction(intent.action)) return

        Timber.d("Restore trigger action=%s", intent.action)
        val pendingResult = goAsync()
        AlarmRescheduler.restoreAsync(context, pendingResult)
    }

    private fun isRestoreAction(action: String?): Boolean {
        return action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_UNLOCKED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
    }
}
