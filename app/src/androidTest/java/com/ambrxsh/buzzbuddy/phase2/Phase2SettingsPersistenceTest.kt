package com.ambrxsh.buzzbuddy.phase2

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ambrxsh.buzzbuddy.model.SettingsData
import com.ambrxsh.buzzbuddy.utils.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase2SettingsPersistenceTest {

    @Test
    fun settingsSurviveSaveAndLoad() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = SettingsManager(context)
        val original = manager.loadSettings()
        try {
            val updated = SettingsData(
                snoozeDuration = 4,
                alarmSound = "Beep",
                volume = 77,
                gradualVolume = true,
                vibrate = false,
                autoDismiss = true
            )
            manager.saveSettings(updated)
            val loaded = manager.loadSettings()
            assertEquals(4, loaded.snoozeDuration)
            assertEquals("Beep", loaded.alarmSound)
            assertEquals(77, loaded.volume)
            assertEquals(true, loaded.gradualVolume)
            assertEquals(false, loaded.vibrate)
            assertEquals(true, loaded.autoDismiss)
        } finally {
            manager.saveSettings(original)
        }
    }
}
