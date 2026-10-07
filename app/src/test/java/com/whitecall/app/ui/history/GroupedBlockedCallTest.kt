package com.whitecall.app.ui.history

import com.whitecall.app.domain.model.BlockedCallLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupedBlockedCallTest {

    private fun createCall(
        isWhitelisted: Boolean = false,
        isContact: Boolean = false,
        isAllowedByContacts: Boolean = false
    ): GroupedBlockedCall {
        val sampleLog = BlockedCallLog(
            id = 1L,
            phoneNumber = "+79991234567",
            callerName = null,
            reason = "NOT_IN_WHITELIST"
        )
        return GroupedBlockedCall(
            key = "key_1",
            phoneNumber = "+79991234567",
            callerName = "Test Contact",
            isWhitelisted = isWhitelisted,
            isContact = isContact,
            isAllowedByContacts = isAllowedByContacts,
            latestCall = sampleLog,
            calls = listOf(sampleLog)
        )
    }

    @Test
    fun isAllowed_whenWhitelisted_returnsTrue() {
        val item = createCall(isWhitelisted = true, isContact = false, isAllowedByContacts = false)
        assertTrue(item.isAllowed)
    }

    @Test
    fun isAllowed_whenAllowedByContacts_returnsTrue() {
        val item = createCall(isWhitelisted = false, isContact = true, isAllowedByContacts = true)
        assertTrue(item.isAllowed)
    }

    @Test
    fun isAllowed_whenInContactsButContactsNotAllowed_returnsFalse() {
        val item = createCall(isWhitelisted = false, isContact = true, isAllowedByContacts = false)
        assertFalse(item.isAllowed)
    }

    @Test
    fun isAllowed_whenNeither_returnsFalse() {
        val item = createCall(isWhitelisted = false, isContact = false, isAllowedByContacts = false)
        assertFalse(item.isAllowed)
    }
}
