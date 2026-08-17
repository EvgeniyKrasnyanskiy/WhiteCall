package com.whitecall.app.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.domain.model.GroupItem
import com.whitecall.app.domain.model.WhiteListEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupWithEntries(
    val group: GroupItem,
    val entries: List<WhiteListEntry>
)

class WhiteListViewModel(
    private val app: WhiteCallApplication = WhiteCallApplication.instance
) : ViewModel() {

    private val repository = app.whiteListRepository
    private val preferences = app.preferences

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _expandedGroupIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedGroupIds: StateFlow<Set<Long>> = _expandedGroupIds.asStateFlow()

    val allowAllContacts: StateFlow<Boolean> = preferences.allowAllContactsFlow

    val groups: StateFlow<List<GroupItem>> = repository.getAllGroupsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val folderList: StateFlow<List<GroupWithEntries>> = combine(
        repository.getAllGroupsFlow(),
        repository.getAllEntriesFlow(),
        _searchQuery
    ) { groupList, entryList, query ->
        val trimmedQuery = query.trim().lowercase()

        // Auto-expand all groups if user is searching
        if (trimmedQuery.isNotEmpty()) {
            _expandedGroupIds.value = groupList.map { it.id }.toSet()
        }

        groupList.map { group ->
            val matchingEntries = entryList.filter { it.groupId == group.id }
            val filteredEntries = if (trimmedQuery.isBlank()) {
                matchingEntries
            } else {
                matchingEntries.filter {
                    it.displayName.lowercase().contains(trimmedQuery) ||
                            it.phoneNumber.contains(trimmedQuery) ||
                            it.normalizedNumber.contains(trimmedQuery)
                }
            }
            GroupWithEntries(group = group, entries = filteredEntries)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleGroupExpanded(groupId: Long) {
        val current = _expandedGroupIds.value.toMutableSet()
        if (current.contains(groupId)) {
            current.remove(groupId)
        } else {
            current.add(groupId)
        }
        _expandedGroupIds.value = current
    }

    fun onToggleAllowAllContacts(enabled: Boolean) {
        preferences.allowAllContacts = enabled
    }

    fun addManualNumber(name: String, phoneNumber: String, groupId: Long? = null, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = repository.addEntry(name, phoneNumber, groupId)
            if (success) {
                // Expand the group so the user sees their newly added contact immediately
                val targetGroupId = groupId ?: repository.getOrCreateDefaultGroup()
                _expandedGroupIds.value = _expandedGroupIds.value + targetGroupId
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun addContactNumber(name: String, phoneNumber: String, groupId: Long? = null) {
        viewModelScope.launch {
            repository.addEntry(name, phoneNumber, groupId)
            val targetGroupId = groupId ?: repository.getOrCreateDefaultGroup()
            _expandedGroupIds.value = _expandedGroupIds.value + targetGroupId
        }
    }

    fun addGroup(name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val newId = repository.addGroup(name)
                _expandedGroupIds.value = _expandedGroupIds.value + newId
                onSuccess()
            }
        }
    }

    fun updateGroup(id: Long, name: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateGroup(id, name, isActive)
        }
    }

    fun setGroupActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.setGroupActive(id, isActive)
        }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            repository.deleteGroup(id)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }
}
