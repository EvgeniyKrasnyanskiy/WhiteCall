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

    val allowAllContacts: StateFlow<Boolean> = preferences.allowAllContactsFlow

    val entries: StateFlow<List<WhiteListEntry>> = repository.getAllEntriesFlow()
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                val trimmed = query.trim().lowercase()
                list.filter {
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

    fun onToggleAllowAllContacts(enabled: Boolean) {
        preferences.allowAllContacts = enabled
    }

    fun addManualNumber(name: String, phoneNumber: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = repository.addEntry(name, phoneNumber)
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun addContactNumber(name: String, phoneNumber: String) {
        viewModelScope.launch {
            repository.addEntry(name, phoneNumber)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }
}
