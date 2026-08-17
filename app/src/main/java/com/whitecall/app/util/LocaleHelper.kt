package com.whitecall.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {

    /**
     * Applies language preference ("system", "en", "ru").
     */
    fun applyLanguage(languageCode: String) {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val targetLocales = when (languageCode.lowercase()) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            "ru" -> LocaleListCompat.forLanguageTags("ru")
            else -> LocaleListCompat.getEmptyLocaleList() // System default
        }
        if (currentLocales != targetLocales) {
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
