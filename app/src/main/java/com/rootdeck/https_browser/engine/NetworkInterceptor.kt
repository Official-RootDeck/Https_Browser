// Built by MD.Nirob
package com.rootdeck.https_browser.engine

import com.rootdeck.https_browser.devtools.NetworkEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Captures network activity via WebViewClient.shouldInterceptRequest.
 * Note: WebView shouldInterceptRequest intercepts document, script, image, css, font, and sub-resource
 * HTTP/HTTPS requests. For XHR/fetch without custom service worker, WebView API may not capture all
 * body payloads directly, so requests are tracked via request headers and URL interception.
 * Built by MD.Nirob
 */
class NetworkInterceptor {

    private val _requests = MutableSharedFlow<NetworkEntry>(extraBufferCapacity = 500)
    val requests: SharedFlow<NetworkEntry> = _requests.asSharedFlow()

    // In-memory LinkedHashMap storing up to 500 entries (memory safety)
    private val capturedEntries = LinkedHashMap<String, NetworkEntry>()

    fun recordRequest(item: NetworkEntry) {
        synchronized(capturedEntries) {
            capturedEntries[item.id] = item
            // Auto-trim to last 500 entries
            while (capturedEntries.size > 500) {
                val oldestKey = capturedEntries.keys.firstOrNull() ?: break
                capturedEntries.remove(oldestKey)
            }
        }
        _requests.tryEmit(item)
    }

    fun getAll(): List<NetworkEntry> = synchronized(capturedEntries) {
        capturedEntries.values.toList().reversed()
    }

    fun clear() {
        synchronized(capturedEntries) {
            capturedEntries.clear()
        }
    }
}
