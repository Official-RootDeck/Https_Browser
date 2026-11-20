// Built by MD.Nirob
package com.rootdeck.https_browser.readinglist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Built by MD.Nirob — Room DAO for offline Reading List storage.
 */
@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY savedAt DESC")
    fun getAll(): Flow<List<ReadingListEntity>>

    @Query("SELECT * FROM reading_list WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): ReadingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReadingListEntity): Long

    @Query("UPDATE reading_list SET isRead = :isRead WHERE id = :id")
    suspend fun setReadStatus(id: Long, isRead: Boolean)

    @Delete
    suspend fun delete(item: ReadingListEntity)

    @Query("DELETE FROM reading_list")
    suspend fun deleteAll()
}
