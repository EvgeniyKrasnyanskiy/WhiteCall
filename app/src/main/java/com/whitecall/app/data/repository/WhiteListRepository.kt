package com.whitecall.app.data.repository

import com.whitecall.app.data.local.dao.GroupDao
import com.whitecall.app.data.local.dao.WhiteListDao
import com.whitecall.app.data.local.entity.GroupEntity
import com.whitecall.app.data.local.entity.WhiteListEntity
import com.whitecall.app.domain.model.GroupItem
import com.whitecall.app.domain.model.WhiteListEntry
import com.whitecall.app.domain.usecase.NormalizePhoneNumberUseCase
import com.whitecall.app.util.BackupData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WhiteListRepository(
    private val whiteListDao: WhiteListDao,
    private val groupDao: GroupDao,
    private val normalizePhoneNumberUseCase: NormalizePhoneNumberUseCase
) {

    fun getAllEntriesFlow(): Flow<List<WhiteListEntry>> {
        return whiteListDao.getAllNumbersFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllGroupsFlow(): Flow<List<GroupItem>> {
        return groupDao.getAllGroupsFlow().map { list ->
            list.map { GroupItem(it.id, it.name, it.isActive, it.createdAt) }
        }
    }

    suspend fun getAllEntries(): List<WhiteListEntry> {
        return whiteListDao.getAllNumbers().map { it.toDomain() }
    }

    suspend fun getAllGroups(): List<GroupItem> {
        return groupDao.getAllGroups().map { GroupItem(it.id, it.name, it.isActive, it.createdAt) }
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

    suspend fun getOrCreateDefaultGroup(): Long {
        val groups = groupDao.getAllGroups()
        if (groups.isNotEmpty()) {
            return groups.first().id
        }
        val defaultGroup = GroupEntity(name = "Основная")
        return groupDao.insert(defaultGroup)
    }

    suspend fun addEntry(displayName: String, phoneNumber: String, groupId: Long? = null): Boolean {
        val normalized = normalizePhoneNumberUseCase.normalize(phoneNumber)
        if (normalized.isBlank()) return false

        val targetGroupId = groupId ?: getOrCreateDefaultGroup()

        val entity = WhiteListEntity(
            displayName = displayName.ifBlank { phoneNumber },
            phoneNumber = phoneNumber.trim(),
            normalizedNumber = normalized,
            groupId = targetGroupId
        )
        whiteListDao.insert(entity)
        return true
    }

    suspend fun moveEntryToGroup(entryId: Long, newGroupId: Long) {
        whiteListDao.assignNumberToGroup(entryId, newGroupId)
    }

    suspend fun addGroup(name: String): Long {
        val entity = GroupEntity(name = name.trim())
        return groupDao.insert(entity)
    }

    suspend fun updateGroup(id: Long, name: String, isActive: Boolean) {
        groupDao.update(GroupEntity(id = id, name = name.trim(), isActive = isActive))
    }

    suspend fun setGroupActive(id: Long, isActive: Boolean) {
        groupDao.setGroupActive(id, isActive)
    }

    suspend fun deleteGroup(id: Long) {
        whiteListDao.deleteByGroupId(id)
        groupDao.deleteById(id)
    }

    suspend fun importBackupData(backupData: BackupData): Int {
        val existingGroups = groupDao.getAllGroups()
        val groupNameToId = existingGroups.associate { it.name.trim().lowercase() to it.id }.toMutableMap()

        // 1. Create missing groups
        for (bg in backupData.groups) {
            val key = bg.name.trim().lowercase()
            if (!groupNameToId.containsKey(key)) {
                val createdId = groupDao.insert(
                    GroupEntity(
                        name = bg.name.trim(),
                        isActive = bg.isActive
                    )
                )
                groupNameToId[key] = createdId
            }
        }

        val defaultGroupId = getOrCreateDefaultGroup()

        // 2. Map and insert numbers
        val entities = backupData.entries.map { entry ->
            val resolvedGroupId = when {
                entry.groupName != null && groupNameToId.containsKey(entry.groupName.trim().lowercase()) ->
                    groupNameToId[entry.groupName.trim().lowercase()]
                entry.groupId != null && groupDao.getGroupById(entry.groupId) != null ->
                    entry.groupId
                else -> defaultGroupId
            }

            WhiteListEntity(
                displayName = entry.displayName,
                phoneNumber = entry.phoneNumber,
                normalizedNumber = normalizePhoneNumberUseCase.normalize(entry.phoneNumber),
                groupId = resolvedGroupId,
                createdAt = entry.createdAt
            )
        }

        whiteListDao.insertAll(entities)
        return entities.size
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
