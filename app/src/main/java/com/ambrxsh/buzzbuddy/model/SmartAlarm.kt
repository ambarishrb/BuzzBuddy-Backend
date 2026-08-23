package com.ambrxsh.buzzbuddy.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_alarms")
data class SmartAlarm(
    var alarmTitle: String,
    var alarmTime_hour: Int,
    var alarmTime_minute: Int,
    var isEnabled: Boolean = true,
    @PrimaryKey(autoGenerate = true)
    var alarmId: Int = 0,
    var serverId: Int? = null
)
