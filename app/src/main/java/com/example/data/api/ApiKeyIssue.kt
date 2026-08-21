package com.example.data.api

import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Every kind of "your key can't be used right now" state the backend can
 * hand back. [message] is what the warning modal shows — kept close to the
 * backend's own wording (see auth.js) so support conversations and in-app
 * copy don't drift apart. [canSwitchKey] / [canPurchase] control which
 * action buttons the modal offers for a given issue — e.g. a rate-limited
 * key can't be fixed by switching to a key you don't have, but a suspended
 * one might be resolved by using a different one.
 */
sealed class ApiKeyIssue(val message: String, val canSwitchKey: Boolean = true, val canPurchase: Boolean = true) {
    object Missing : ApiKeyIssue("No API key is set on this device yet.", canSwitchKey = false)
    object Invalid : ApiKeyIssue("This API key isn't recognized.")
    object Revoked : ApiKeyIssue("This API key has been revoked.")
    object Deleted : ApiKeyIssue("This API key no longer exists.")
    object Paused : ApiKeyIssue("This API key is paused.")
    object Suspended : ApiKeyIssue("This API key has been suspended.")
    object Expired : ApiKeyIssue("This API key has expired.")
    object AccountInactive : ApiKeyIssue("Your account is inactive.", canSwitchKey = false)
    object RateLimited : ApiKeyIssue("You've used up today's request limit for this key.", canPurchase = true)
    data class Other(val detail: String) : ApiKeyIssue(detail)
}

/**
 * Classifies a failed response purely from its HTTP status and body — no
 * dependency on which endpoint was called, since every authenticated route
 * in this backend fails the same way (see auth.js / rateLimit.js) and every
 * response (success or error) is wrapped by responseWrapper.js as
 * `{ success, data: {...}, timestamp, ... }`. The real error text always
 * lives at `data.error` / `data.message`, never at the root — that's the
 * one detail worth getting right here.
 */
object ApiIssueClassifier {
    private val moshi = Moshi.Builder().build()
    private val anyAdapter = moshi.adapter(Any::class.java)

    fun classify(code: Int, errorBody: String?): ApiKeyIssue? {
        if (code < 400) return null
        val text = extractErrorText(errorBody)?.lowercase().orEmpty()

        return when {
            code == 429 -> ApiKeyIssue.RateLimited
            code == 401 && text.contains("required") -> ApiKeyIssue.Missing
            code == 401 -> ApiKeyIssue.Invalid
            code == 403 && text.contains("revoked") -> ApiKeyIssue.Revoked
            code == 403 && text.contains("deleted") -> ApiKeyIssue.Deleted
            code == 403 && text.contains("paused") -> ApiKeyIssue.Paused
            code == 403 && text.contains("suspended") -> ApiKeyIssue.Suspended
            code == 403 && text.contains("expired") -> ApiKeyIssue.Expired
            code == 403 && text.contains("inactive") -> ApiKeyIssue.AccountInactive
            code == 403 -> ApiKeyIssue.Other(text.ifBlank { "Access denied for this API key." })
            else -> null
        }
    }

    /** Reads `data.error`/`data.message` first (the real shape, per responseWrapper.js), falling back to a root-level `error`/`message` in case a route ever bypasses the wrapper. */
    private fun extractErrorText(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            when (val parsed = anyAdapter.fromJson(body)) {
                is Map<*, *> -> {
                    val data = parsed["data"]
                    if (data is Map<*, *>) {
                        (data["message"] ?: data["error"])?.toString()
                    } else {
                        (parsed["message"] ?: parsed["error"])?.toString()
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Turns classified issues into one sticky app-wide signal that any screen's
 * failed request can raise and exactly one global dialog (see
 * ApiKeyIssueDialog) can observe — instead of each screen independently
 * showing (or more often, silently swallowing) its own fetch failure.
 *
 * This is a genuine singleton (not a class you instantiate) because more
 * than one OkHttpClient in this app can carry an AuthInterceptor — the
 * repository's own client, and separately the one YoApplication builds for
 * Coil's image loading (posters/covers also go out with X-API-Key, so a
 * revoked/expired key breaks those too). Both need to feed the exact same
 * signal, not two independent copies of it.
 *
 * [suppressed] is a narrow escape hatch for the one call site that should
 * NOT trigger this — LoginScreen's own key-validation request, which
 * already has its own inline error UI and would be confusing to also pop
 * a global "switch or purchase" modal over.
 */
object ApiIssueReporter {
    private val _issue = MutableStateFlow<ApiKeyIssue?>(null)
    val issueFlow: StateFlow<ApiKeyIssue?> = _issue

    @Volatile
    var suppressed: Boolean = false

    fun report(code: Int, errorBody: String?) {
        if (suppressed) return
        val issue = ApiIssueClassifier.classify(code, errorBody) ?: return
        _issue.value = issue
    }

    fun clear() {
        _issue.value = null
    }
}
