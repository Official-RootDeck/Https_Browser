// Built by MD.Nirob
package com.rootdeck.https_browser.reader

import android.content.res.AssetManager
import org.json.JSONObject

/**
 * Built by MD.Nirob — Extracts clean readable article content using assets/js/readability.js.
 */
data class ReaderArticle(
    val title: String,
    val byline: String,
    val siteName: String,
    val excerpt: String,
    val paragraphs: List<String>,
    val textContent: String,
    val readingTimeMinutes: Int,
    val sourceUrl: String
)

object ReaderModeExtractor {

    fun loadReadabilityScript(assetManager: AssetManager): String {
        return try {
            assetManager.open("js/readability.js").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "JSON.stringify({title: document.title || '', byline: '', siteName: location.hostname, excerpt: '', paragraphs: [document.body ? document.body.innerText : ''], textContent: document.body ? document.body.innerText : '', readingTimeMinutes: 1});"
        }
    }

    fun parseResult(rawJsResult: String?, sourceUrl: String): ReaderArticle? {
        if (rawJsResult.isNullOrBlank() || rawJsResult == "null") return null
        return try {
            var jsonStr = rawJsResult.trim()
            if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                jsonStr = org.json.JSONTokener(jsonStr).nextValue() as? String ?: jsonStr
            }
            val obj = JSONObject(jsonStr)
            val title = obj.optString("title", "Untitled Article").ifBlank { "Untitled Article" }
            val byline = obj.optString("byline", "")
            val siteName = obj.optString("siteName", "")
            val excerpt = obj.optString("excerpt", "")
            val textContent = obj.optString("textContent", "")
            val readingTime = obj.optInt("readingTimeMinutes", 1).coerceAtLeast(1)
            val arr = obj.optJSONArray("paragraphs")
            val paragraphs = mutableListOf<String>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val p = arr.optString(i, "").trim()
                    if (p.isNotEmpty()) paragraphs.add(p)
                }
            }
            if (paragraphs.isEmpty() && textContent.isBlank()) {
                return null
            }
            ReaderArticle(
                title = title,
                byline = byline,
                siteName = siteName,
                excerpt = excerpt,
                paragraphs = if (paragraphs.isNotEmpty()) paragraphs else listOf(textContent),
                textContent = textContent,
                readingTimeMinutes = readingTime,
                sourceUrl = sourceUrl
            )
        } catch (_: Exception) {
            null
        }
    }
}
