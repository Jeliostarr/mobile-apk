package com.example.data.cache

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * TTL cache for repository calls.
 *
 *  - Returns the cached value if it's still fresh.
 *  - Coalesces concurrent callers for the same key into one in-flight
 *    request, so if four screens ask for the same thing in the same
 *    frame, only one HTTP call goes out.
 *  - `forceRefresh = true` bypasses both the cache and any in-flight
 *    request (used by pull-to-refresh).
 *  - Key is caller-supplied (e.g. "sports.leagues", "md.detail.xyz").
 *
 * The cache lives in memory only and dies with the app process.
 * That's deliberate: a cold start re-fetches everything fresh, which
 * is what you want.
 */
class ApiCache {

    private data class Entry<T>(val value: T, val fetchedAtMs: Long)

    private val store = ConcurrentHashMap<String, Entry<*>>()
    private val inFlight = ConcurrentHashMap<String, Deferred<*>>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(
        key: String,
        ttlSeconds: Long,
        forceRefresh: Boolean = false,
        fetch: suspend () -> T,
    ): T = coroutineScope {
        val now = System.currentTimeMillis()
        val ttlMs = ttlSeconds * 1000L

        if (!forceRefresh) {
            val cached = store[key] as? Entry<T>
            if (cached != null && (now - cached.fetchedAtMs) < ttlMs) {
                return@coroutineScope cached.value
            }

            // Coalesce: if another coroutine is already fetching this key,
            // wait for its result instead of firing a second request.
            val existing = inFlight[key]
            if (existing != null) {
                return@coroutineScope existing.await() as T
            }
        }

        // We're the first caller — start the fetch.
        val deferred = async {
            val value = fetch()
            mutex.withLock {
                store[key] = Entry(value, System.currentTimeMillis())
            }
            value
        }
        inFlight[key] = deferred
        try {
            deferred.await()
        } finally {
            inFlight.remove(key)
        }
    }

    /** Called by logout / key switch to drop everything. */
    fun clear() {
        store.clear()
        inFlight.clear()
    }
}
