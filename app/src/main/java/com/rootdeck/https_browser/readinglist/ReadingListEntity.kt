// Built by MD.Nirob
package com.rootdeck.https_browser.readinglist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Built by MD.Nirob — Room entity for offline Reading List articles.
 */
@Entity(tableName = "reading_list")
data class ReadingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val excerpt: String,
    val contentText: String,
    val isRead: Boolean = false,
    val savedAt: Long = System.currentTimeMillis()
)
