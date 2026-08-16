package com.whitecall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_calls")
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "caller_name")
    val callerName: String?,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "reason")
    val reason: String = "NOT_IN_WHITELIST"
)
