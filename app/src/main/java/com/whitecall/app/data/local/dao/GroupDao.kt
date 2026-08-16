package com.whitecall.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.whitecall.app.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM whitelist_groups ORDER BY created_at ASC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM whitelist_groups ORDER BY created_at ASC")
    suspend fun getAllGroups(): List<GroupEntity>

    @Query("SELECT * FROM whitelist_groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Query("UPDATE whitelist_groups SET is_active = :isActive WHERE id = :id")
    suspend fun setGroupActive(id: Long, isActive: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity): Long

    @Update
    suspend fun update(group: GroupEntity)

    @Delete
    suspend fun delete(group: GroupEntity)

    @Query("DELETE FROM whitelist_groups WHERE id = :id")
    suspend fun deleteById(id: Long)
}
