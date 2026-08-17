package com.whitecall.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.whitecall.app.domain.model.ScheduleSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _protectionEnabledFlow = MutableStateFlow(isProtectionEnabled)
    val protectionEnabledFlow: StateFlow<Boolean> = _protectionEnabledFlow.asStateFlow()

    private val _allowAllContactsFlow = MutableStateFlow(allowAllContacts)
    val allowAllContactsFlow: StateFlow<Boolean> = _allowAllContactsFlow.asStateFlow()

    private val _scheduleSettingsFlow = MutableStateFlow(scheduleSettings)
    val scheduleSettingsFlow: StateFlow<ScheduleSettings> = _scheduleSettingsFlow.asStateFlow()

    private val _appLanguageFlow = MutableStateFlow(appLanguage)
    val appLanguageFlow: StateFlow<String> = _appLanguageFlow.asStateFlow()

    private val _appThemeFlow = MutableStateFlow(appTheme)
    val appThemeFlow: StateFlow<String> = _appThemeFlow.asStateFlow()

    private val _blockModeFlow = MutableStateFlow(blockMode)
    val blockModeFlow: StateFlow<String> = _blockModeFlow.asStateFlow()

    init {
        if (!prefs.contains(KEY_PROTECTION_ENABLED)) {
            prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, true).apply()
            _protectionEnabledFlow.value = true
        }
        if (!prefs.contains(KEY_ALLOW_ALL_CONTACTS)) {
            prefs.edit().putBoolean(KEY_ALLOW_ALL_CONTACTS, true).apply()
            _allowAllContactsFlow.value = true
        }
    }

    var isProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, value).apply()
            _protectionEnabledFlow.value = value
        }

    var allowAllContacts: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_ALL_CONTACTS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ALLOW_ALL_CONTACTS, value).apply()
            _allowAllContactsFlow.value = value
        }

    var scheduleSettings: ScheduleSettings
        get() {
            val enabled = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
            val startHour = prefs.getInt(KEY_SCHEDULE_START_HOUR, 22)
            val startMinute = prefs.getInt(KEY_SCHEDULE_START_MINUTE, 0)
            val endHour = prefs.getInt(KEY_SCHEDULE_END_HOUR, 7)
            val endMinute = prefs.getInt(KEY_SCHEDULE_END_MINUTE, 0)
            val daysSet = prefs.getStringSet(
                KEY_SCHEDULE_DAYS,
                setOf("1", "2", "3", "4", "5", "6", "7")
            )?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            )

            return ScheduleSettings(
                isEnabled = enabled,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                activeDays = daysSet
            )
        }
        set(value) {
            prefs.edit()
                .putBoolean(KEY_SCHEDULE_ENABLED, value.isEnabled)
                .putInt(KEY_SCHEDULE_START_HOUR, value.startHour)
                .putInt(KEY_SCHEDULE_START_MINUTE, value.startMinute)
                .putInt(KEY_SCHEDULE_END_HOUR, value.endHour)
                .putInt(KEY_SCHEDULE_END_MINUTE, value.endMinute)
                .putStringSet(KEY_SCHEDULE_DAYS, value.activeDays.map { it.toString() }.toSet())
                .apply()
            _scheduleSettingsFlow.value = value
        }

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "ru") ?: "ru"
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()
            _appLanguageFlow.value = value
        }

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "dark") ?: "dark"
        set(value) {
            prefs.edit().putString(KEY_APP_THEME, value).apply()
            _appThemeFlow.value = value
        }

    var blockMode: String
        get() = prefs.getString(KEY_BLOCK_MODE, BLOCK_MODE_SILENCE) ?: BLOCK_MODE_SILENCE
        set(value) {
            prefs.edit().putString(KEY_BLOCK_MODE, value).apply()
            _blockModeFlow.value = value
        }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
        }

    /**
     * Determine if call protection is currently active:
     * - If schedule is enabled, check if current time is inside schedule window.
     * - Otherwise check master protection toggle.
     */
    fun isProtectionCurrentlyActive(calendar: Calendar = Calendar.getInstance()): Boolean {
        val schedule = scheduleSettings
        return if (schedule.isEnabled) {
            schedule.isScheduleActive(calendar)
        } else {
            isProtectionEnabled
        }
    }

    companion object {
        const val BLOCK_MODE_REJECT = "reject"
        const val BLOCK_MODE_SILENCE = "silence"

        private const val PREFS_NAME = "whitecall_preferences"
        private const val KEY_PROTECTION_ENABLED = "key_protection_enabled"
        private const val KEY_ALLOW_ALL_CONTACTS = "key_allow_all_contacts"
        private const val KEY_SCHEDULE_ENABLED = "key_schedule_enabled"
        private const val KEY_SCHEDULE_START_HOUR = "key_schedule_start_hour"
        private const val KEY_SCHEDULE_START_MINUTE = "key_schedule_start_minute"
        private const val KEY_SCHEDULE_END_HOUR = "key_schedule_end_hour"
        private const val KEY_SCHEDULE_END_MINUTE = "key_schedule_end_minute"
        private const val KEY_SCHEDULE_DAYS = "key_schedule_days"
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val KEY_APP_THEME = "key_app_theme"
        private const val KEY_BLOCK_MODE = "key_block_mode"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
    }
}
