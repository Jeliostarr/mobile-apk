package com.example.data.api

import com.example.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = tokenManager.getApiKey()

        val requestBuilder = originalRequest.newBuilder()
        if (!apiKey.isNullOrEmpty()) {
            requestBuilder.header("X-API-Key", apiKey)
        }

        val response = chain.proceed(requestBuilder.build())

        // Only classify failures for requests that actually carried a key —
        // a call made while genuinely logged out isn't "your key has a
        // problem", it's just not authenticated, and shouldn't pop the
        // switch-or-purchase modal. ApiIssueReporter is a singleton (see
        // ApiKeyIssue.kt) specifically so this works the same way whether
        // this interceptor instance belongs to YocinemaRepository's client
        // or the separate one YoApplication builds for Coil's image loader —
        // both report into the exact same signal.
        if (!apiKey.isNullOrEmpty() && response.code in AUTH_FAILURE_CODES) {
            try {
                // peekBody (not body()!) reads a copy without consuming the
                // real stream — Retrofit/the caller still needs to read the
                // actual body afterwards (e.g. loginWithKey reads errorBody()
                // itself), and a response body can only be consumed once.
                val peeked = response.peekBody(4096).string()
                ApiIssueReporter.report(response.code, peeked)
            } catch (e: Exception) {
                // Non-fatal — worst case the global modal just doesn't fire
                // for this particular failure.
            }
        }

        // NOTE: this used to silently clear the saved key on every 401,
        // logging the user out with zero explanation. Now that failures are
        // surfaced through the global warning modal (see ApiKeyIssueDialog),
        // the key is left in place so the user can see what's wrong and
        // choose to switch keys or log out deliberately, rather than being
        // signed out invisibly mid-session.

        return response
    }

    companion object {
        private val AUTH_FAILURE_CODES = intArrayOf(401, 403, 429)
    }
}
