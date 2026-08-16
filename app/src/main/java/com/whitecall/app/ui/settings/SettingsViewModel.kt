package com.whitecall.app.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.domain.model.ScheduleSettings
import com.whitecall.app.util.JsonBackupHelper
import com.whitecall.app.util.LocaleHelper
import com.whitecall.app.widget.WhiteCallWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val app: WhiteCallApplication = WhiteCallApplication.instance
) : ViewModel() {

    private val preferences = app.preferences
    private val whiteListRepository = app.whiteListRepository

    val isProtectionEnabled: StateFlow<Boolean> = preferences.protectionEnabledFlow
    val scheduleSettings: StateFlow<ScheduleSettings> = preferences.scheduleSettingsFlow
    val appLanguage: StateFlow<String> = preferences.appLanguageFlow
    val appTheme: StateFlow<String> = preferences.appThemeFlow

    private val _isRoleHeld = MutableStateFlow(false)
    val isRoleHeld: StateFlow<Boolean> = _isRoleHeld.asStateFlow()

    fun checkCallScreeningRole(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            _isRoleHeld.value = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            _isRoleHeld.value = true // Pre-Q devices use default call screening binding directly
        }
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        preferences.isProtectionEnabled = enabled
        WhiteCallWidgetProvider.updateAllWidgets(context)
    }

    fun updateScheduleSettings(context: Context, settings: ScheduleSettings) {
        preferences.scheduleSettings = settings
        WhiteCallWidgetProvider.updateAllWidgets(context)
    }

    fun setAppLanguage(languageCode: String) {
        preferences.appLanguage = languageCode
        LocaleHelper.applyLanguage(languageCode)
    }

    fun setAppTheme(theme: String) {
        preferences.appTheme = theme
    }

    fun exportToJson(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val entries = whiteListRepository.getAllEntries()
            val jsonString = JsonBackupHelper.exportToJson(entries)
            val success = JsonBackupHelper.writeToUri(context, uri, jsonString)
            onResult(success)
        }
    }

    fun importFromJson(context: Context, uri: Uri, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val jsonString = JsonBackupHelper.readFromUri(context, uri)
            if (jsonString == null) {
                onResult(-1)
                return@launch
            }
            try {
                val entries = JsonBackupHelper.parseFromJson(jsonString)
                if (entries.isNotEmpty()) {
                    whiteListRepository.addAllEntries(entries)
                    onResult(entries.size)
                } else {
                    onResult(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(-1)
            }
        }
    }
}
