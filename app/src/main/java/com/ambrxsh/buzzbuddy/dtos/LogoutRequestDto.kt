package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class LogoutRequestDto(
    @SerializedName("refresh_token") val refreshToken: String? = null
)
