package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String = "",
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String = "bearer",
    val token: String? = null
)
