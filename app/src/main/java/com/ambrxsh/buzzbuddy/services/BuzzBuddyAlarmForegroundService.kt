package com.ambrxsh.buzzbuddy.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ambrxsh.buzzbuddy.AlarmActivity
import com.ambrxsh.buzzbuddy.AlarmPlayer
import com.ambrxsh.buzzbuddy.AlarmReceiver
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler
import timber.log.Timber

class BuzzBuddyAlarmForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "alarm_ringing_channel"
        const val FALLBACK_NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.ambrxsh.buzzbuddy.STOP_FOREGROUND_ALARM"

        fun notificationId(alarmId: Int): Int {
            return if (alarmId >= 0) 1000 + alarmId else FALLBACK_NOTIFICATION_ID
        }

        fun start(context: Context, alarmId: Int) {
            val intent = Intent(context, BuzzBuddyAlarmForegroundService::class.java).apply {
                putExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BuzzBuddyAlarmForegroundService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AlarmPlayer.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getIntExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        if (intent == null || alarmId < 0) {
            Timber.w("Ignoring ringing service restart without a valid alarm id")
            val notification = createFullScreenNotification(-1)
            startRingingForeground(FALLBACK_NOTIFICATION_ID, notification)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        Timber.d("Starting ringing foreground service for alarmId=%s", alarmId)
        val notification = createFullScreenNotification(alarmId)
        startRingingForeground(notificationId(alarmId), notification)
        AlarmPlayer.start(this)
        return START_STICKY
    }

    private fun startRingingForeground(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(id, notification)
        }
    }

    private fun createFullScreenNotification(alarmId: Int): Notification {
        createHighImportanceChannel()

        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("openAlarmFragment", true)
            putExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP_ALARM
            putExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId(alarmId) + 50_000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.alarm_ringing_title))
            .setContentText(getString(R.string.alarm_ringing_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(activityPendingIntent)
            .setFullScreenIntent(activityPendingIntent, true)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.alarm_stop), stopPendingIntent)
            .build()
    }

    private fun createHighImportanceChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.alarm_channel_description)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
