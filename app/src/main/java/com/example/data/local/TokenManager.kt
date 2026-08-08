package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "yocinema_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("yocinema_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val _apiKeyFlow = MutableStateFlow(getApiKey())
    val apiKeyFlow: StateFlow<String?> = _apiKeyFlow.asStateFlow()

    fun getApiKey(): String? {
        val key = prefs.getString(KEY_API_KEY, null)
        return if (key.isNull_orBlank()) null else key
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        _apiKeyFlow.value = key.trim()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = null
    }

    fun getVjHistory(): List<String> {
        val historyStr = prefs.getString(KEY_VJ_HISTORY, null) ?: return emptyList()
        return historyStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun saveVjHistory(vjName: String) {
        if (vjName.isBlank()) return
        val current = getVjHistory().toMutableList()
        current.remove(vjName.trim())
        current.add(0, vjName.trim())
        val updated = current.take(10).joinToString(",")
        prefs.edit().putString(KEY_VJ_HISTORY, updated).apply()
    }

    fun getOrCreateViewerId(): String {
        var id = prefs.getString(KEY_VIEWER_ID, null)
        if (id.isNull_orBlank()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_VIEWER_ID, id).apply()
        }
        return id!!
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_VJ_HISTORY = "vj_history"
        private const val KEY_VIEWER_ID = "viewer_id"

        private fun String?.isNull_orBlank(): Boolean {
            return this == null || this.trim().isEmpty()
        }
    }
}
