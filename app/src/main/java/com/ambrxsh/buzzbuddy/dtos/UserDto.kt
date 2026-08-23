package com.ambrxsh.buzzbuddy.dtos

import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: Int? = null,
    val name: String = "",
    val email: String = "",
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
