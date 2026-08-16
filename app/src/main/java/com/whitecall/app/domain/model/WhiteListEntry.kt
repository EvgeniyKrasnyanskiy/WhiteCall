package com.whitecall.app.domain.model

data class WhiteListEntry(
    val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val createdAt: Long = System.currentTimeMillis()
)
