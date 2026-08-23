package com.ambrxsh.buzzbuddy.network

import com.ambrxsh.buzzbuddy.clients.AuthClientService
import com.ambrxsh.buzzbuddy.dtos.RefreshRequestDto
import com.ambrxsh.buzzbuddy.utils.SessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val session: SessionStore,
    private val refreshService: AuthClientService,
    private val onExpired: () -> Unit
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path == "/api/login" ||
            path == "/api/register" ||
            path == "/api/auth/refresh" ||
            path == "/api/account/password"
        ) {
            return null
        }
        if (responseCount(response) >= 2) {
            onExpired()
            return null
        }
        val refresh = session.getRefreshToken()
        if (refresh.isNullOrBlank()) {
            if (!session.getAccessToken().isNullOrBlank()) {
                onExpired()
            }
            return null
        }
        synchronized(lock) {
            val currentAccess = session.getAccessToken()
            val failedAuth = response.request.header("Authorization")
            if (!currentAccess.isNullOrBlank() && failedAuth != "Bearer $currentAccess") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .build()
            }
            return try {
                val tokens = runBlocking {
                    refreshService.refresh(RefreshRequestDto(refresh))
                }
                val access = tokens.accessToken.ifBlank { tokens.token.orEmpty() }
                if (access.isBlank()) {
                    onExpired()
                    return null
                }
                session.saveSession(access, tokens.refreshToken ?: refresh, session.getEmail().orEmpty())
                response.request.newBuilder()
                    .header("Authorization", "Bearer $access")
                    .build()
            } catch (_: Exception) {
                onExpired()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    companion object {
        private val lock = Any()
    }
}
