package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class RefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)
