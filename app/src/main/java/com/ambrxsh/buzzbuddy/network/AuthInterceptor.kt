package com.ambrxsh.buzzbuddy.network

import com.ambrxsh.buzzbuddy.utils.SessionStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val session: SessionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        val builder = chain.request().newBuilder()
        if (path !in PUBLIC_PATHS) {
            val token = session.getAccessToken()
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
        }
        return chain.proceed(builder.build())
    }

    companion object {
        private val PUBLIC_PATHS = setOf(
            "/api/login",
            "/api/register",
            "/api/auth/refresh",
            "/api/auth/password-reset/request",
            "/api/auth/password-reset/confirm"
        )
    }
}
