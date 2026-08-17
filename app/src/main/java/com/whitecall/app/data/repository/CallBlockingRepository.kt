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

    @Volatile
    private var lastBlockedPhone: String? = null
    @Volatile
    private var lastBlockedTimestamp: Long = 0L

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
        val now = System.currentTimeMillis()
        // Deduplication guard: ignore duplicate triggers within 2.5s for the same number
        synchronized(this) {
            if (phoneNumber == lastBlockedPhone && (now - lastBlockedTimestamp) < 2500L) {
                return -1L
            }
            lastBlockedPhone = phoneNumber
            lastBlockedTimestamp = now
        }

        val entity = BlockedCallEntity(
            phoneNumber = phoneNumber,
            callerName = callerName,
            timestamp = now,
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
        synchronized(this) {
            lastBlockedPhone = null
            lastBlockedTimestamp = 0L
        }
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
