package com.whitecall.app

import com.whitecall.app.domain.usecase.NormalizePhoneNumberUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NormalizePhoneNumberUseCaseTest {

    private lateinit var useCase: NormalizePhoneNumberUseCase

    @Before
    fun setUp() {
        useCase = NormalizePhoneNumberUseCase()
    }

    @Test
    fun normalize_russianNumbers_converts8ToPlus7() {
        assertEquals("+79991234567", useCase.normalize("8 (999) 123-45-67"))
        assertEquals("+79991234567", useCase.normalize("+7 (999) 123-45-67"))
        assertEquals("+79991234567", useCase.normalize("89991234567"))
    }

    @Test
    fun normalize_internationalNumbers_retainsPlusAndDigits() {
        assertEquals("+12025550123", useCase.normalize("+1 (202) 555-0123"))
        assertEquals("+442071838750", useCase.normalize("+44 20 7183 8750"))
    }

    @Test
    fun normalize_emptyOrNull_returnsEmpty() {
        assertEquals("", useCase.normalize(null))
        assertEquals("", useCase.normalize("   "))
    }

    @Test
    fun areNumbersEquivalent_differentFormats_returnsTrue() {
        assertTrue(useCase.areNumbersEquivalent("+7 999 123-45-67", "89991234567"))
        assertTrue(useCase.areNumbersEquivalent("+7 (999) 123-45-67", "+79991234567"))
        assertTrue(useCase.areNumbersEquivalent("89991234567", "9991234567"))
    }

    @Test
    fun areNumbersEquivalent_differentNumbers_returnsFalse() {
        assertFalse(useCase.areNumbersEquivalent("+79991112233", "+79994445566"))
    }
}
