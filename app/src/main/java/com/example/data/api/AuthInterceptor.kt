package com.example.data.api

import com.example.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = tokenManager.getApiKey()

        val requestBuilder = originalRequest.newBuilder()
        if (!apiKey.isNullOrEmpty()) {
            requestBuilder.header("X-API-Key", apiKey)
        }

        val response = chain.proceed(requestBuilder.build())

        // Clear API key ONLY on HTTP 401 Unauthorized
        if (response.code == 401) {
            tokenManager.clearApiKey()
        }

        return response
    }
}
