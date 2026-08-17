package com.whitecall.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.whitecall.app.data.local.entity.WhiteListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhiteListDao {

    @Query("SELECT * FROM whitelist_numbers ORDER BY display_name ASC, created_at DESC")
    fun getAllNumbersFlow(): Flow<List<WhiteListEntity>>

    @Query("SELECT * FROM whitelist_numbers ORDER BY display_name ASC")
    suspend fun getAllNumbers(): List<WhiteListEntity>

    @Query("SELECT * FROM whitelist_numbers WHERE normalized_number = :normalizedNumber LIMIT 1")
    suspend fun findByNormalizedNumber(normalizedNumber: String): WhiteListEntity?

    @Query("""
        SELECT * FROM whitelist_numbers 
        WHERE normalized_number LIKE '%' || :partialNumber 
           OR :partialNumber LIKE '%' || normalized_number 
        LIMIT 1
    """)
    suspend fun findMatchingNumber(partialNumber: String): WhiteListEntity?

    @Query("SELECT COUNT(*) FROM whitelist_numbers")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT * FROM whitelist_numbers WHERE (:groupId IS NULL AND group_id IS NULL) OR group_id = :groupId ORDER BY display_name ASC")
    fun getNumbersByGroupFlow(groupId: Long?): Flow<List<WhiteListEntity>>

    @Query("UPDATE whitelist_numbers SET group_id = NULL WHERE group_id = :groupId")
    suspend fun unassignNumbersFromGroup(groupId: Long)

    @Query("UPDATE whitelist_numbers SET group_id = :groupId WHERE id = :id")
    suspend fun assignNumberToGroup(id: Long, groupId: Long?)

    @Query("SELECT * FROM whitelist_numbers")
    fun getAllNumbersListFlow(): Flow<List<WhiteListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WhiteListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<WhiteListEntity>)

    @Update
    suspend fun update(entity: WhiteListEntity)

    @Delete
    suspend fun delete(entity: WhiteListEntity)

    @Query("DELETE FROM whitelist_numbers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM whitelist_numbers WHERE group_id = :groupId")
    suspend fun deleteByGroupId(groupId: Long)

    @Query("DELETE FROM whitelist_numbers")
    suspend fun deleteAll()
}
