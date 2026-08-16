package com.whitecall.app.domain.model

data class BlockedCallLog(
    val id: Long = 0,
    val phoneNumber: String,
    val callerName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "NOT_IN_WHITELIST"
)
