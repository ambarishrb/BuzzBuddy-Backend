package com.ambrxsh.buzzbuddy

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.ambrxsh.buzzbuddy.fragments.ActivityAlarmFragment
import com.ambrxsh.buzzbuddy.scheduler.BuzzBuddyAlarmScheduler
import java.util.Calendar

class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContentView(R.layout.activity_alarm)

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val minute = Calendar.getInstance().get(Calendar.MINUTE)
        AlarmPlayer.start(this, hour, minute)

        if (savedInstanceState == null || intent.getBooleanExtra("openAlarmFragment", false)) {
            showAlarmFragment(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showOverLockScreen()
        if (intent.getBooleanExtra("openAlarmFragment", false)) {
            showAlarmFragment(intent)
        }
    }

    private fun showAlarmFragment(source: Intent) {
        val alarmId = source.getIntExtra(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, -1)
        val fragment = ActivityAlarmFragment().apply {
            arguments = Bundle().apply { putInt(BuzzBuddyAlarmScheduler.EXTRA_ALARM_ID, alarmId) }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.alarm_fragment_container, fragment)
            .commit()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
