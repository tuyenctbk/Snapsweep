package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanupDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStatsFlow(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserStats(stats: UserStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleanupRecords(records: List<CleanupRecordEntity>)

    @Query("SELECT mediaId FROM kept_media")
    suspend fun getAllKeptMediaIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeptMedia(kept: KeptMediaEntity)

    @Query("DELETE FROM kept_media WHERE mediaId = :mediaId")
    suspend fun deleteKeptMedia(mediaId: Long)

    @Query("DELETE FROM kept_media")
    suspend fun clearKeptMedia()

    @Query("DELETE FROM cleanup_records")
    suspend fun clearCleanupRecords()

    @Query("DELETE FROM user_stats")
    suspend fun clearUserStats()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaggedMedia(items: List<TaggedMediaEntity>)

    @Query("SELECT * FROM tagged_media")
    suspend fun getAllTaggedMedia(): List<TaggedMediaEntity>

    @Query("SELECT * FROM tagged_media WHERE tagType = :type")
    suspend fun getTaggedMediaByType(type: String): List<TaggedMediaEntity>

    @Query("DELETE FROM tagged_media WHERE mediaId = :mediaId")
    suspend fun deleteTaggedMedia(mediaId: Long)

    @Query("DELETE FROM tagged_media")
    suspend fun clearTaggedMedia()
}
