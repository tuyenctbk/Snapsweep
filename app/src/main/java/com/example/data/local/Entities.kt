package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleanup_records")
data class CleanupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val categoryName: String,
    val sizeBytes: Long,
    val cleanedAtMillis: Long
)

@Entity(tableName = "kept_media")
data class KeptMediaEntity(
    @PrimaryKey val mediaId: Long,
    val keptAtMillis: Long
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalBytesFreed: Long = 0L,
    val totalItemsCleaned: Int = 0,
    val lastCleanupMillis: Long = 0L
)

@Entity(tableName = "tagged_media")
data class TaggedMediaEntity(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val tagType: String, // "DUPLICATE", "SCREENSHOT", "BLURRY", "HEAVY"
    val fileHash: String,
    val tagReason: String,
    val taggedAtMillis: Long
)
