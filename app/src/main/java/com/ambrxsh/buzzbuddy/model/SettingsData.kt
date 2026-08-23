package com.ambrxsh.buzzbuddy.model

data class SettingsData(
    var snoozeDuration: Int = 10,
    var alarmSound: String = "Sunrise",
    var volume: Int = 50,
    var gradualVolume: Boolean = false,
    var vibrate: Boolean = true,
    var autoDismiss: Boolean = false
)