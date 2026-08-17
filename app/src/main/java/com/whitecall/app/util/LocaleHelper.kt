package com.whitecall.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {

    /**
     * Applies language preference ("system", "en", "ru").
     * Safely checks language code to avoid activity recreation loops during configuration changes.
     */
    fun applyLanguage(languageCode: String) {
        val targetLocales = when (languageCode.lowercase()) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "ru" -> LocaleListCompat.forLanguageTags("ru")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentCode = if (currentLocales.isEmpty) "system" else currentLocales.get(0)?.language ?: "system"
        val targetCode = if (targetLocales.isEmpty) "system" else targetLocales.get(0)?.language ?: "system"

        if (currentCode != targetCode) {
            AppCompatDelegate.setApplicationLocales(targetLocales)
        }
    }

    /**
     * Returns the currently applied language code.
     */
    fun getCurrentLanguageCode(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return "system"
        val tag = locales.get(0)?.language ?: return "system"
        return when (tag) {
            "ru" -> "ru"
            "en" -> "en"
            else -> "system"
        }
    }
}
