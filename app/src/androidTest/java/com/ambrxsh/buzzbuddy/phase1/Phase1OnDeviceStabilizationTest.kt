package com.ambrxsh.buzzbuddy.phase1

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ambrxsh.buzzbuddy.R
import com.ambrxsh.buzzbuddy.model.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase1OnDeviceStabilizationTest {

    @Test
    fun mainActivityLaunchesAlarmList() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById(R.id.fragment_container))
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertNotNull("SetAlarmPage should be shown on launch", fragment)
                assertNotNull(fragment!!.requireView().findViewById(R.id.recyclerViewAlarms))
                assertNotNull(fragment.requireView().findViewById(R.id.add_alarm_button))
            }
        }
    }

    @Test
    fun exactAlarmPermissionIsUsableOnThisDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        assertTrue(
            "Enable Alarms & reminders for Buzz Buddy before Phase 4",
            alarmManager.canScheduleExactAlarms()
        )
    }
}
