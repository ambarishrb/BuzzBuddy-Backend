package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("confirm_password") val confirmPassword: String? = null
)
