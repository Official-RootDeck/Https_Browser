// Built by MD.Nirob
package com.rootdeck.https_browser.readinglist

import android.content.res.AssetManager
import com.rootdeck.https_browser.reader.ReaderModeExtractor

/**
 * Built by MD.Nirob — Extracts clean offline text snapshots from pages for the Reading List.
 */
object ReadingListExtractor {

    fun getExtractionScript(assetManager: AssetManager): String {
        return ReaderModeExtractor.loadReadabilityScript(assetManager)
    }

    fun buildEntity(rawJsResult: String?, fallbackUrl: String, fallbackTitle: String): ReadingListEntity {
        val article = ReaderModeExtractor.parseResult(rawJsResult, fallbackUrl)
        return if (article != null) {
            ReadingListEntity(
                url = fallbackUrl,
                title = article.title.ifBlank { fallbackTitle.ifBlank { fallbackUrl } },
                excerpt = article.excerpt,
                contentText = article.textContent.ifBlank { article.paragraphs.joinToString("\n\n") },
                isRead = false
            )
        } else {
            ReadingListEntity(
                url = fallbackUrl,
                title = fallbackTitle.ifBlank { fallbackUrl },
                excerpt = fallbackUrl,
                contentText = "Saved link: $fallbackUrl",
                isRead = false
            )
        }
    }
}
