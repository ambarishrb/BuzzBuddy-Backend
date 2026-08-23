package com.ambrxsh.buzzbuddy.phase2

import com.ambrxsh.buzzbuddy.model.SettingsData
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Phase2GsonSettingsTest {

    @Test
    fun settingsDataSurvivesGsonRoundTrip() {
        val original = SettingsData(
            snoozeDuration = 7,
            alarmSound = "Beep",
            volume = 80,
            gradualVolume = true,
            vibrate = false,
            autoDismiss = true
        )
        val restored = Gson().fromJson(Gson().toJson(original), SettingsData::class.java)
        assertEquals(original.snoozeDuration, restored.snoozeDuration)
        assertEquals(original.alarmSound, restored.alarmSound)
        assertEquals(original.volume, restored.volume)
        assertEquals(original.gradualVolume, restored.gradualVolume)
        assertFalse(restored.vibrate)
        assertEquals(original.autoDismiss, restored.autoDismiss)
    }
}
