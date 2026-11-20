// Built by MD.Nirob
package com.rootdeck.https_browser.devtools

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Internal NetworkEntry data model representing captured HTTP/HTTPS requests in Dev Tools.
 * Built by MD.Nirob
 */
data class NetworkEntry(
    val id: String = (1000..9999).random().toString() + "-" + System.currentTimeMillis(),
    val url: String,
    val method: String = "GET",
    val status: Int = 200,
    val sizeBytes: Long = 0,
    val durationMs: Long = 0,
    val cookies: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
    val isBlocked: Boolean = false,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
) {
    fun formattedSize(): String {
        return when {
            sizeBytes <= 0 -> "0 B"
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }
    }

    fun formattedDuration(): String = "${durationMs} ms"
}
