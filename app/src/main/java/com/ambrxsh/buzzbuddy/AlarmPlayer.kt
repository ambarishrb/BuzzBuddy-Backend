package com.ambrxsh.buzzbuddy

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ambrxsh.buzzbuddy.utils.AlarmTimeFormat
import com.ambrxsh.buzzbuddy.utils.SettingsManager
import timber.log.Timber
import java.util.Calendar

object AlarmPlayer {
    private var currentHour: Int = 0
    private var currentMinute: Int = 0

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousAlarmVolume: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null

    fun start(context: Context, hour: Int, minute: Int) {
        currentHour = hour
        currentMinute = minute
        start(context)
    }

    fun getAlarmTime(context: Context): String {
        val calendar = Calendar.getInstance()
        return AlarmTimeFormat.format12Hour(
            context,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        val settings = SettingsManager(appContext).loadSettings()
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (mediaPlayer == null) {
            applyAlarmStreamVolume(settings.volume)
            requestAlarmAudioFocus()
            startAlarmAudio(appContext, settings.alarmSound, settings.gradualVolume, settings.volume)
        }

        if (settings.vibrate && vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 500, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }

        if (settings.autoDismiss && autoDismissRunnable == null) {
            autoDismissRunnable = Runnable { stop() }
            handler.postDelayed(autoDismissRunnable!!, 120_000)
        }
    }

    private fun startAlarmAudio(
        appContext: Context,
        soundName: String,
        gradualVolume: Boolean,
        volumePercent: Int
    ) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val uris = soundUris(appContext, soundName)
        for (uri in uris) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(attributes)
                    setDataSource(appContext, uri)
                    isLooping = true
                    prepare()
                    if (gradualVolume) {
                        setVolume(0f, 0f)
                        start()
                        gradualVolumeIncrease(volumePercent)
                    } else {
                        start()
                    }
                }
                return
            } catch (e: Exception) {
                Timber.w(e, "Could not play alarm uri %s", uri)
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
        Timber.e("No alarm sound could be started")
    }

    private fun soundUris(context: Context, soundName: String): List<Uri> {
        val beep = context.getString(R.string.alarm_sound_beep)
        val wantsBeep = soundName.equals(beep, ignoreCase = true)
        val primary = if (wantsBeep) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
        } else {
            android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        val fallbacks = listOfNotNull(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            android.provider.Settings.System.DEFAULT_RINGTONE_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        return (listOfNotNull(primary) + fallbacks).distinct()
    }

    private fun applyAlarmStreamVolume(volumePercent: Int) {
        val manager = audioManager ?: return
        if (previousAlarmVolume == null) {
            previousAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val target = ((volumePercent / 100f) * maxVolume).toInt().coerceIn(0, maxVolume)
        try {
            manager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
        } catch (e: SecurityException) {
            Timber.w(e, "Could not set alarm stream volume")
        }
    }

    private fun requestAlarmAudioFocus() {
        val manager = audioManager ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
            manager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun gradualVolumeIncrease(targetPercent: Int) {
        val targetVolume = (targetPercent / 100f).coerceIn(0f, 1f)
        var currentVolume = 0f
        val runnable = object : Runnable {
            override fun run() {
                if (mediaPlayer != null && currentVolume < targetVolume) {
                    currentVolume = (currentVolume + 0.05f).coerceAtMost(targetVolume)
                    mediaPlayer?.setVolume(currentVolume, currentVolume)
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(runnable)
    }

    fun stop() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        autoDismissRunnable = null
        handler.removeCallbacksAndMessages(null)

        mediaPlayer?.run {
            try {
                if (isPlaying) stop()
            } catch (_: IllegalStateException) {
            }
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        previousAlarmVolume?.let { volume ->
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
            } catch (_: SecurityException) {
            }
        }
        previousAlarmVolume = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
        audioFocusRequest = null
        audioManager = null
    }
}
