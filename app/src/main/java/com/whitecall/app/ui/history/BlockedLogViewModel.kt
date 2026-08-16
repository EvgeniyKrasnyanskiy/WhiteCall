package com.whitecall.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.domain.model.BlockedCallLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlockedCallUiItem(
    val log: BlockedCallLog,
    val isWhitelisted: Boolean
)

class BlockedLogViewModel(
    private val app: WhiteCallApplication = WhiteCallApplication.instance
) : ViewModel() {

    private val blockingRepository = app.callBlockingRepository
    private val whiteListRepository = app.whiteListRepository
    private val normalizeUseCase = app.normalizePhoneNumberUseCase

    val blockedCalls: StateFlow<List<BlockedCallUiItem>> = combine(
        blockingRepository.getAllBlockedCallsFlow(),
        whiteListRepository.getAllEntriesFlow()
    ) { logs, whitelist ->
        logs.map { log ->
            val isWhitelisted = whitelist.any {
                normalizeUseCase.areNumbersEquivalent(it.phoneNumber, log.phoneNumber)
            }
            BlockedCallUiItem(log = log, isWhitelisted = isWhitelisted)
        }
    }.stateIn(
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
