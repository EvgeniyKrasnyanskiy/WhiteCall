package com.whitecall.app.data.repository

import com.whitecall.app.data.local.dao.WhiteListDao
import com.whitecall.app.data.local.entity.WhiteListEntity
import com.whitecall.app.domain.model.WhiteListEntry
import com.whitecall.app.domain.usecase.NormalizePhoneNumberUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WhiteListRepository(
    private val whiteListDao: WhiteListDao,
    private val normalizePhoneNumberUseCase: NormalizePhoneNumberUseCase
) {

    fun getAllEntriesFlow(): Flow<List<WhiteListEntry>> {
        return whiteListDao.getAllNumbersFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAllEntries(): List<WhiteListEntry> {
        return whiteListDao.getAllNumbers().map { it.toDomain() }
    }

    suspend fun isNumberInWhiteList(incomingNumber: String?): Boolean {
        if (incomingNumber.isNullOrBlank()) return false

        val normalized = normalizePhoneNumberUseCase.normalize(incomingNumber)
        // Check direct match
        val directMatch = whiteListDao.findByNormalizedNumber(normalized)
        if (directMatch != null) return true

        // Check significant digits match
        val sigDigits = normalizePhoneNumberUseCase.extractSignificantDigits(incomingNumber)
        if (sigDigits.length >= 7) {
            val partialMatch = whiteListDao.findMatchingNumber(sigDigits)
            if (partialMatch != null) return true
        }

        // Fallback check: iterate entries for equivalence
        val all = whiteListDao.getAllNumbers()
        return all.any {
            normalizePhoneNumberUseCase.areNumbersEquivalent(it.phoneNumber, incomingNumber)
        }
    }

    suspend fun addEntry(displayName: String, phoneNumber: String): Boolean {
        val normalized = normalizePhoneNumberUseCase.normalize(phoneNumber)
        if (normalized.isBlank()) return false

        val entity = WhiteListEntity(
            displayName = displayName.ifBlank { phoneNumber },
            phoneNumber = phoneNumber.trim(),
            normalizedNumber = normalized
        )
        whiteListDao.insert(entity)
        return true
    }

    suspend fun addAllEntries(entries: List<WhiteListEntry>) {
        val entities = entries.map {
            WhiteListEntity(
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                normalizedNumber = normalizePhoneNumberUseCase.normalize(it.phoneNumber),
                createdAt = it.createdAt
            )
        }
        whiteListDao.insertAll(entities)
    }

    suspend fun deleteEntry(id: Long) {
        whiteListDao.deleteById(id)
    }

    suspend fun deleteAll() {
        whiteListDao.deleteAll()
    }

    private fun WhiteListEntity.toDomain() = WhiteListEntry(
        id = id,
        displayName = displayName,
        phoneNumber = phoneNumber,
        normalizedNumber = normalizedNumber,
        createdAt = createdAt
    )
}
