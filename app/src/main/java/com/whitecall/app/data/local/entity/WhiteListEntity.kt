package com.whitecall.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelist_numbers",
    indices = [Index(value = ["normalized_number"], unique = true)]
)
data class WhiteListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
