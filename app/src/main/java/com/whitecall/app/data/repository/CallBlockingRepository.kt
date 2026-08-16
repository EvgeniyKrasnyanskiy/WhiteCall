package com.whitecall.app.data.repository

import com.whitecall.app.data.local.dao.BlockedCallDao
import com.whitecall.app.data.local.entity.BlockedCallEntity
import com.whitecall.app.data.preferences.AppPreferences
import com.whitecall.app.domain.model.BlockedCallLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class CallBlockingRepository(
    private val blockedCallDao: BlockedCallDao,
    private val preferences: AppPreferences
) {

    fun getAllBlockedCallsFlow(): Flow<List<BlockedCallLog>> {
        return blockedCallDao.getAllBlockedCallsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getLatestBlockedCall(): BlockedCallLog? {
        return blockedCallDao.getLatestBlockedCall()?.toDomain()
    }

    suspend fun recordBlockedCall(
        phoneNumber: String,
        callerName: String?,
        reason: String = "NOT_IN_WHITELIST"
    ): Long {
        val entity = BlockedCallEntity(
            phoneNumber = phoneNumber,
            callerName = callerName,
            timestamp = System.currentTimeMillis(),
            reason = reason
        )
        val id = blockedCallDao.insert(entity)
        // Keep last 500 records to prevent infinite growth
        blockedCallDao.trimOldRecords(500)
        return id
    }

    suspend fun getBlockedTodayCount(): Int {
        val midnight = getTodayMidnightTimestamp()
        return blockedCallDao.getBlockedCountSince(midnight)
    }

    suspend fun getBlockedWeekCount(): Int {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        return blockedCallDao.getBlockedCountSince(sevenDaysAgo)
    }

    suspend fun getBlockedMonthCount(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        return blockedCallDao.getBlockedCountSince(thirtyDaysAgo)
    }

    fun getBlockedTodayCountFlow(): Flow<Int> {
        val midnight = getTodayMidnightTimestamp()
        return blockedCallDao.countBlockedCallsSinceFlow(midnight)
    }

    suspend fun clearLog() {
        blockedCallDao.deleteAll()
    }

    private fun getTodayMidnightTimestamp(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun BlockedCallEntity.toDomain() = BlockedCallLog(
        id = id,
        phoneNumber = phoneNumber,
        callerName = callerName,
        timestamp = timestamp,
        reason = reason
    )
}
