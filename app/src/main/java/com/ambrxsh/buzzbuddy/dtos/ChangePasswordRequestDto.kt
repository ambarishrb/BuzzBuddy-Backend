package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequestDto(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)
