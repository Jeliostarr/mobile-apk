package com.example.data.player

import com.example.data.api.YocinemaApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

data class CachedStreamToken(
    val token: String,
    val expiresAtMs: Long
)

class StreamTokenManager(private val api: YocinemaApi) {
    private val tokenCache = ConcurrentHashMap<String, CachedStreamToken>()
    private val mutex = Mutex()

    suspend fun getOrMintStreamToken(movieId: String): String {
        val now = System.currentTimeMillis()
        val cached = tokenCache[movieId]

        // Refresh 60s (60_000 ms) before expiry
        if (cached != null && (cached.expiresAtMs - 60_000) > now) {
            return cached.token
        }

        return mutex.withLock {
            // Re-check inside lock
            val recheckCached = tokenCache[movieId]
            if (recheckCached != null && (recheckCached.expiresAtMs - 60_000) > System.currentTimeMillis()) {
                return@withLock recheckCached.token
            }

            try {
                val response = api.mintStreamToken(movieId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val token = body.streamToken
                    val ttlMs = body.expiresIn * 1000L
                    val expiresAtMs = System.currentTimeMillis() + ttlMs
                    val newToken = CachedStreamToken(token, expiresAtMs)
                    tokenCache[movieId] = newToken
                    return@withLock token
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fallback: Return cached if present, else empty
            tokenCache[movieId]?.token ?: ""
        }
    }

    suspend fun attachStreamToken(url: String, movieId: String): String {
        if (url.isBlank()) return url
        val token = getOrMintStreamToken(movieId)
        if (token.isBlank()) return url

        val delimiter = if (url.contains("?")) "&" else "?"
        return "$url${delimiter}streamToken=$token"
    }

    fun invalidateToken(movieId: String) {
        tokenCache.remove(movieId)
    }
}
