package com.whitecall.app.domain.usecase

class NormalizePhoneNumberUseCase {

    /**
     * Strips all non-digit and non-plus characters.
     */
    fun normalize(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""

        val cleaned = rawNumber.trim()
        val hasPlus = cleaned.startsWith("+")
        val digitsOnly = cleaned.filter { it.isDigit() }

        if (digitsOnly.isEmpty()) return ""

        // For standard 11-digit Russian numbers starting with 8: replace 8 with +7
        if (!hasPlus && digitsOnly.length == 11 && digitsOnly.startsWith("8")) {
            return "+7" + digitsOnly.substring(1)
        }

        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }

    /**
     * Extracts significant national digits (typically the last 10 digits)
     * to safely compare numbers stored in different formats (e.g. +79991234567 vs 89991234567).
     */
    fun extractSignificantDigits(rawNumber: String?): String {
        val digits = rawNumber?.filter { it.isDigit() } ?: ""
        return if (digits.length > 10) {
            digits.takeLast(10)
        } else {
            digits
        }
    }

    /**
     * Checks if two phone numbers refer to the same destination.
     */
    fun areNumbersEquivalent(number1: String?, number2: String?): Boolean {
        if (number1.isNullOrBlank() || number2.isNullOrBlank()) return false

        val norm1 = normalize(number1)
        val norm2 = normalize(number2)

        if (norm1 == norm2) return true

        val sig1 = extractSignificantDigits(number1)
        val sig2 = extractSignificantDigits(number2)

        return sig1.isNotEmpty() && sig1.length >= 7 && sig1 == sig2
    }
}
