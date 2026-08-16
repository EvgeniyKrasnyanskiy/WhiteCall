package com.whitecall.app.domain.usecase

import android.content.Context
import com.whitecall.app.data.preferences.AppPreferences
import com.whitecall.app.data.repository.WhiteListRepository
import com.whitecall.app.util.ContactHelper
import java.util.Calendar

data class CallFilterResult(
    val shouldBlock: Boolean,
    val reason: String,
    val callerName: String? = null
)

class ShouldBlockCallUseCase(
    private val whiteListRepository: WhiteListRepository,
    private val preferences: AppPreferences,
    private val normalizePhoneNumberUseCase: NormalizePhoneNumberUseCase
) {

    suspend fun evaluateCall(
        context: Context,
        rawIncomingNumber: String?,
        calendar: Calendar = Calendar.getInstance()
    ): CallFilterResult {
        // 1. Check if protection is active (manual switch or active schedule)
        val isProtectionActive = preferences.isProtectionCurrentlyActive(calendar)
        if (!isProtectionActive) {
            return CallFilterResult(
                shouldBlock = false,
                reason = "PROTECTION_DISABLED"
            )
        }

        // 2. Check for hidden / anonymous numbers
        if (rawIncomingNumber.isNullOrBlank()) {
            return CallFilterResult(
                shouldBlock = true,
                reason = "ANONYMOUS_CALLER",
                callerName = null
            )
        }

        // 3. Check if number is in White List Database
        val isWhitelisted = whiteListRepository.isNumberInWhiteList(rawIncomingNumber)
        if (isWhitelisted) {
            return CallFilterResult(
                shouldBlock = false,
                reason = "WHITELISTED"
            )
        }

        // 4. Check if "Allow all contacts" is enabled and number is in System Contacts
        if (preferences.allowAllContacts) {
            val contactName = ContactHelper.getContactNameByNumber(context, rawIncomingNumber)
            if (!contactName.isNullOrBlank()) {
                return CallFilterResult(
                    shouldBlock = false,
                    reason = "ALLOWED_CONTACT",
                    callerName = contactName
                )
            }
        }

        // 5. Fallback: Lookup contact name for logging if possible, then block
        val contactName = ContactHelper.getContactNameByNumber(context, rawIncomingNumber)

        return CallFilterResult(
            shouldBlock = true,
            reason = "NOT_IN_WHITELIST",
            callerName = contactName
        )
    }
}
