package com.whitecall.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.domain.model.BlockedCallLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockedLogViewModel(
    private val app: WhiteCallApplication = WhiteCallApplication.instance
) : ViewModel() {

    private val blockingRepository = app.callBlockingRepository
    private val whiteListRepository = app.whiteListRepository

    val blockedCalls: StateFlow<List<BlockedCallLog>> = blockingRepository.getAllBlockedCallsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch {
            blockingRepository.clearLog()
        }
    }

    fun addToWhiteList(phoneNumber: String, callerName: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val name = callerName ?: phoneNumber
            whiteListRepository.addEntry(name, phoneNumber)
            onSuccess()
        }
    }
}
