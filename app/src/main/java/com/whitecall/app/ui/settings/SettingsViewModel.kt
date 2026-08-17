package com.whitecall.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whitecall.app.WhiteCallApplication
import com.whitecall.app.domain.model.ScheduleSettings
import com.whitecall.app.util.JsonBackupHelper
import com.whitecall.app.util.UpdateChecker
import com.whitecall.app.util.UpdateInfo
import com.whitecall.app.widget.WhiteCallWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateUiState {
    object Idle : UpdateUiState()
    object Checking : UpdateUiState()
    data class Success(val info: UpdateInfo) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}

class SettingsViewModel(
    private val app: WhiteCallApplication = WhiteCallApplication.instance
) : ViewModel() {

    private val preferences = app.preferences
    private val whiteListRepository = app.whiteListRepository

    val isProtectionEnabled: StateFlow<Boolean> = preferences.protectionEnabledFlow
    val scheduleSettings: StateFlow<ScheduleSettings> = preferences.scheduleSettingsFlow
    val appLanguage: StateFlow<String> = preferences.appLanguageFlow
    val appTheme: StateFlow<String> = preferences.appThemeFlow
    val blockMode: StateFlow<String> = preferences.blockModeFlow

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

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
        com.whitecall.app.util.LocaleHelper.applyLanguage(languageCode)
    }

    fun setAppTheme(themeCode: String) {
        preferences.appTheme = themeCode
    }

    fun setBlockMode(mode: String) {
        preferences.blockMode = mode
    }

    fun checkForUpdates() {
        _updateState.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = UpdateChecker.checkLatestRelease()
            result.onSuccess { info ->
                _updateState.value = UpdateUiState.Success(info)
            }.onFailure { err ->
                _updateState.value = UpdateUiState.Error(err.localizedMessage ?: "Network error")
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateState.value = UpdateUiState.Idle
    }

    fun exportBackup(context: Context, uri: android.net.Uri, onSuccess: (Int) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val groups = whiteListRepository.getAllGroups()
            val entries = whiteListRepository.getAllEntries()
            val json = JsonBackupHelper.exportToJson(groups, entries)
            val ok = JsonBackupHelper.writeToUri(context, uri, json)
            if (ok) onSuccess(entries.size) else onError()
        }
    }

    fun importBackup(context: Context, uri: android.net.Uri, onSuccess: (Int) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val json = JsonBackupHelper.readFromUri(context, uri)
            if (json != null) {
                val backupData = JsonBackupHelper.parseFromJson(json)
                val count = whiteListRepository.importBackupData(backupData)
                onSuccess(count)
            } else {
                onError()
            }
        }
    }
}
