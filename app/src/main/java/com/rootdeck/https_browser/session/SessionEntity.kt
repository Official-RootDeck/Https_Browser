// Built by MD.Nirob
package com.rootdeck.https_browser.session

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Built by MD.Nirob — Room entities for saved browsing sessions and their tab snapshots.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tabCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "session_tabs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class SessionTabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val url: String,
    val title: String,
    val position: Int
)
