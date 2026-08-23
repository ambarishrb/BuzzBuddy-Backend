package com.ambrxsh.buzzbuddy.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler

object AlarmPermissionHelper {

    const val REQUEST_POST_NOTIFICATIONS = 4101

    fun requestStartupPermissions(activity: Activity) {
        requestNotificationPermission(activity)
        requestExactAlarmPermission(activity)
        requestFullScreenIntentPermission(activity)
    }

    fun hasReliabilityGap(context: Context): Boolean {
        if (!BuzzBuddyAlarmScheduler(context).canScheduleExactAlarms()) return true
        if (!hasNotifications(context)) return true
        if (!canUseFullScreenIntent(context)) return true
        if (!isIgnoringBatteryOptimizations(context)) return true
        return false
    }

    fun repairNextGap(activity: Activity) {
        when {
            !BuzzBuddyAlarmScheduler(activity).canScheduleExactAlarms() ->
                requestExactAlarmPermission(activity)
            !hasNotifications(activity) ->
                requestNotificationPermission(activity)
            !canUseFullScreenIntent(activity) ->
                requestFullScreenIntentPermission(activity)
            !isIgnoringBatteryOptimizations(activity) ->
                requestBatteryOptimizationExemption(activity)
            else ->
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                )
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
        }
    }

    fun requestExactAlarmPermission(activity: Activity) {
        val scheduler = BuzzBuddyAlarmScheduler(activity)
        if (scheduler.canScheduleExactAlarms()) return

        AlertDialog.Builder(activity)
            .setTitle(R.string.exact_alarm_permission_title)
            .setMessage(R.string.exact_alarm_permission_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                scheduler.openExactAlarmSettings(activity)
            }
            .setNegativeButton(R.string.permission_not_now, null)
            .show()
    }

    fun requestFullScreenIntentPermission(activity: Activity) {
        if (canUseFullScreenIntent(activity)) return

        AlertDialog.Builder(activity)
            .setTitle(R.string.full_screen_permission_title)
            .setMessage(R.string.full_screen_permission_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = "package:${activity.packageName}".toUri()
                    }
                    activity.startActivity(intent)
                }
            }
            .setNegativeButton(R.string.permission_not_now, null)
            .show()
    }

    fun requestBatteryOptimizationExemption(activity: Activity) {
        if (isIgnoringBatteryOptimizations(activity)) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.battery_permission_title)
            .setMessage(R.string.battery_permission_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        activity.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:${activity.packageName}".toUri()
                            }
                        )
                    } catch (_: Exception) {
                        activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            }
            .setNegativeButton(R.string.permission_not_now, null)
            .show()
    }

    fun requestDndAccess(activity: Activity) {
        val manager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (manager.isNotificationPolicyAccessGranted) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.dnd_permission_title)
            .setMessage(R.string.dnd_permission_message)
            .setPositiveButton(R.string.permission_open_settings) { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            .setNegativeButton(R.string.permission_not_now, null)
            .show()
    }

    private fun hasNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return NotificationManagerCompat.from(context).canUseFullScreenIntent()
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
