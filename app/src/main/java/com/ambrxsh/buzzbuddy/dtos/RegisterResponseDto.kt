package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class RegisterResponseDto(
    val id: Int,
    val name: String,
    val email: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
