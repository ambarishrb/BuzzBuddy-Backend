package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class PasswordResetConfirmDto(
    val email: String,
    val code: String,
    @SerializedName("new_password") val newPassword: String
)
