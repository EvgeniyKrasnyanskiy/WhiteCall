package com.whitecall.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.whitecall.app.data.local.entity.BlockedCallEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun getAllBlockedCallsFlow(): Flow<List<BlockedCallEntity>>

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBlockedCalls(limit: Int = 10): List<BlockedCallEntity>

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBlockedCall(): BlockedCallEntity?

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE timestamp >= :sinceTimestamp")
    suspend fun countBlockedCallsSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE timestamp >= :sinceTimestamp")
    fun countBlockedCallsSinceFlow(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedCallEntity): Long

    @Delete
    suspend fun delete(entity: BlockedCallEntity)

    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()
}
