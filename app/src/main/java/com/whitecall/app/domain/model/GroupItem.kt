package com.whitecall.app.domain.model

data class GroupItem(
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
