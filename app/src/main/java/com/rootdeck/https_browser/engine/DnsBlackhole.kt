package com.rootdeck.https_browser.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class DnsBlackhole(context: Context) {

    private val blockedDomains = HashSet<String>()
    var blockedCount = 0
        private set

    init {
        try {
            context.assets.open("blocklist.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            blockedDomains.add(trimmed.lowercase())
                        }
                    }
                }
            }
            Log.d(TAG, "Loaded ${blockedDomains.size} blocked tracker domains.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load DNS blocklist", e)
        }
    }

    fun isBlocked(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            val blocked = blockedDomains.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
            if (blocked) {
                blockedCount++
                Log.d(TAG, "DnsBlackhole blocked tracker: $host (Total: $blockedCount)")
            }
            blocked
        } catch (e: Exception) {
            false
        }
    }

    fun resetCount() {
        blockedCount = 0
    }

    companion object {
        private const val TAG = "DnsBlackhole"
    }
}
