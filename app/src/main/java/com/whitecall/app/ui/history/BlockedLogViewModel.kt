package com.whitecall.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.domain.model.BlockedCallLog
import com.whitecall.app.util.ContactHelper
import com.whitecall.app.util.PhoneUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupedBlockedCall(
    val key: String,
    val phoneNumber: String,
    val callerName: String?,
    val isWhitelisted: Boolean,
    val latestCall: BlockedCallLog,
    val calls: List<BlockedCallLog>
) {
    val totalCount: Int get() = calls.size
}

data class DateGroupedBlockedCalls(
    val startOfDayMillis: Long,
    val items: List<GroupedBlockedCall>
)

data class BlockedLogUiState(
    val dateGroups: List<DateGroupedBlockedCalls> = emptyList(),
    val totalCount: Int = 0
)

class BlockedLogViewModel(
    private val app: WhiteCallApplication = WhiteCallApplication.instance
) : ViewModel() {

    private val blockingRepository = app.callBlockingRepository
    private val whiteListRepository = app.whiteListRepository
    private val normalizeUseCase = app.normalizePhoneNumberUseCase

    val uiState: StateFlow<BlockedLogUiState> = combine(
        blockingRepository.getAllBlockedCallsFlow(),
        whiteListRepository.getAllEntriesFlow()
    ) { logs, whitelist ->
        val dateGroups = logs
            .groupBy { PhoneUtils.getStartOfDay(it.timestamp) }
            .map { (startOfDay, dayLogs) ->
                val groupedByNumber = LinkedHashMap<String, MutableList<BlockedCallLog>>()
                for (call in dayLogs) {
                    val norm = normalizeUseCase.normalize(call.phoneNumber).ifBlank { call.phoneNumber }
                    groupedByNumber.getOrPut(norm) { mutableListOf() }.add(call)
                }

                val items = groupedByNumber.map { (_, callsForNumber) ->
                    val latest = callsForNumber.first()
                    val isWhitelisted = whitelist.any {
                        normalizeUseCase.areNumbersEquivalent(it.phoneNumber, latest.phoneNumber)
                    }
                    val callerName = callsForNumber.firstOrNull { !it.callerName.isNullOrBlank() }?.callerName
                        ?: ContactHelper.getContactNameByNumber(app, latest.phoneNumber)

                    GroupedBlockedCall(
                        key = "${startOfDay}_${latest.id}",
                        phoneNumber = latest.phoneNumber,
                        callerName = callerName,
                        isWhitelisted = isWhitelisted,
                        latestCall = latest,
                        calls = callsForNumber
                    )
                }

                DateGroupedBlockedCalls(
                    startOfDayMillis = startOfDay,
                    items = items
                )
            }

        BlockedLogUiState(
            dateGroups = dateGroups,
            totalCount = logs.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BlockedLogUiState()
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

    fun removeFromWhiteList(phoneNumber: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            whiteListRepository.deleteByPhoneNumber(phoneNumber)
            onSuccess()
        }
    }
}

