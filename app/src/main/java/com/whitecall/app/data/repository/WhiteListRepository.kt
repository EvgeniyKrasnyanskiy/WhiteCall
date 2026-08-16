package com.whitecall.app.data.repository

import com.whitecall.app.data.local.dao.WhiteListDao
import com.whitecall.app.data.local.entity.WhiteListEntity
import com.whitecall.app.domain.model.WhiteListEntry
import com.whitecall.app.domain.usecase.NormalizePhoneNumberUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WhiteListRepository(
    private val whiteListDao: WhiteListDao,
    private val groupDao: com.whitecall.app.data.local.dao.GroupDao,
    private val normalizePhoneNumberUseCase: NormalizePhoneNumberUseCase
) {

    fun getAllEntriesFlow(): Flow<List<WhiteListEntry>> {
        return whiteListDao.getAllNumbersFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllGroupsFlow(): Flow<List<com.whitecall.app.domain.model.GroupItem>> {
        return groupDao.getAllGroupsFlow().map { list ->
            list.map { com.whitecall.app.domain.model.GroupItem(it.id, it.name, it.isActive, it.createdAt) }
        }
    }

    suspend fun getAllEntries(): List<WhiteListEntry> {
        return whiteListDao.getAllNumbers().map { it.toDomain() }
    }

    suspend fun getWhiteListCount(): Int {
        return whiteListDao.getAllNumbers().size
    }

    suspend fun isNumberInWhiteList(incomingNumber: String?): Boolean {
        if (incomingNumber.isNullOrBlank()) return false

        val normalized = normalizePhoneNumberUseCase.normalize(incomingNumber)
        var matchedEntity = whiteListDao.findByNormalizedNumber(normalized)

        if (matchedEntity == null) {
            val sigDigits = normalizePhoneNumberUseCase.extractSignificantDigits(incomingNumber)
            if (sigDigits.length >= 7) {
                matchedEntity = whiteListDao.findMatchingNumber(sigDigits)
            }
        }

        if (matchedEntity == null) {
            val all = whiteListDao.getAllNumbers()
            matchedEntity = all.firstOrNull {
                normalizePhoneNumberUseCase.areNumbersEquivalent(it.phoneNumber, incomingNumber)
            }
        }

        if (matchedEntity == null) {
            return false
        }

        // If number belongs to a group, check if that group is currently active
        val gId = matchedEntity.groupId
        if (gId != null) {
            val group = groupDao.getGroupById(gId)
            if (group != null && !group.isActive) {
                return false // Group is disabled, so block calls from this group
            }
        }

        return true
    }

    suspend fun addEntry(displayName: String, phoneNumber: String, groupId: Long? = null): Boolean {
        val normalized = normalizePhoneNumberUseCase.normalize(phoneNumber)
        if (normalized.isBlank()) return false

        val entity = WhiteListEntity(
            displayName = displayName.ifBlank { phoneNumber },
            phoneNumber = phoneNumber.trim(),
            normalizedNumber = normalized,
            groupId = groupId
        )
        whiteListDao.insert(entity)
        return true
    }

    suspend fun addGroup(name: String): Long {
        val entity = com.whitecall.app.data.local.entity.GroupEntity(name = name.trim())
        return groupDao.insert(entity)
    }

    suspend fun updateGroup(id: Long, name: String, isActive: Boolean) {
        groupDao.update(com.whitecall.app.data.local.entity.GroupEntity(id = id, name = name.trim(), isActive = isActive))
    }

    suspend fun setGroupActive(id: Long, isActive: Boolean) {
        groupDao.setGroupActive(id, isActive)
    }

    suspend fun deleteGroup(id: Long) {
        whiteListDao.unassignNumbersFromGroup(id)
        groupDao.deleteById(id)
    }

    suspend fun addAllEntries(entries: List<WhiteListEntry>) {
        val entities = entries.map {
            WhiteListEntity(
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                normalizedNumber = normalizePhoneNumberUseCase.normalize(it.phoneNumber),
                groupId = it.groupId,
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
        groupId = groupId,
        createdAt = createdAt
    )
}
