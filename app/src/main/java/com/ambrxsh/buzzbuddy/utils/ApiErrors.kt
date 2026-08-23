package com.ambrxsh.buzzbuddy.utils

import org.json.JSONObject
import retrofit2.HttpException

fun HttpException.apiErrorMessage(fallback: String): String {
    val raw = response()?.errorBody()?.string().orEmpty()
    if (raw.isBlank()) return fallback
    return try {
        val error = JSONObject(raw).optString("error")
        error.ifBlank { fallback }
    } catch (_: Exception) {
        fallback
    }
}
