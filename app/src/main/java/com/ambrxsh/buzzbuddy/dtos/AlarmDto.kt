package com.ambrxsh.buzzbuddy.dtos

data class AlarmDto(
    val id: Int? = null,
    val title: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean
)
