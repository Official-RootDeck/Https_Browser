// Built by MD.Nirob
package com.rootdeck.https_browser.session

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Built by MD.Nirob — Room DAO for saved sessions and session tabs.
 */
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session_tabs WHERE sessionId = :sessionId ORDER BY position ASC")
    suspend fun getTabsForSession(sessionId: Long): List<SessionTabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionTabs(tabs: List<SessionTabEntity>)

    @Delete
    suspend fun deleteSession(session: SessionEntity)
}
