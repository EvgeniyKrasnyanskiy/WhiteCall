package com.whitecall.app.ui.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.domain.model.WhiteListEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WhiteListViewModel(
    private val app: WhiteCallApplication = WhiteCallApplication.instance
) : ViewModel() {

    private val repository = app.whiteListRepository
    private val preferences = app.preferences

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<Long?>(null) // null = All
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    val allowAllContacts: StateFlow<Boolean> = preferences.allowAllContactsFlow

    val groups: StateFlow<List<com.whitecall.app.domain.model.GroupItem>> = repository.getAllGroupsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val entries: StateFlow<List<WhiteListEntry>> = combine(
        repository.getAllEntriesFlow(),
        _searchQuery,
        _selectedGroupId
    ) { list, query, selectedGroup ->
        val groupFiltered = when (selectedGroup) {
            null -> list // All
            -1L -> list.filter { it.groupId == null } // Unassigned
            else -> list.filter { it.groupId == selectedGroup }
        }

        if (query.isBlank()) {
            groupFiltered
        } else {
            val trimmed = query.trim().lowercase()
            groupFiltered.filter {
                it.displayName.lowercase().contains(trimmed) ||
                        it.phoneNumber.contains(trimmed) ||
                        it.normalizedNumber.contains(trimmed)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
    }

    fun onToggleAllowAllContacts(enabled: Boolean) {
        preferences.allowAllContacts = enabled
    }

    fun addManualNumber(name: String, phoneNumber: String, groupId: Long? = null, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val targetGroupId = if (groupId != null && groupId > 0) groupId else _selectedGroupId.value?.takeIf { it > 0 }
            val success = repository.addEntry(name, phoneNumber, targetGroupId)
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun addContactNumber(name: String, phoneNumber: String, groupId: Long? = null) {
        viewModelScope.launch {
            val targetGroupId = if (groupId != null && groupId > 0) groupId else _selectedGroupId.value?.takeIf { it > 0 }
            repository.addEntry(name, phoneNumber, targetGroupId)
        }
    }

    fun addGroup(name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val newId = repository.addGroup(name)
                _selectedGroupId.value = newId
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
            if (_selectedGroupId.value == id) {
                _selectedGroupId.value = null
            }
            repository.deleteGroup(id)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }
}
