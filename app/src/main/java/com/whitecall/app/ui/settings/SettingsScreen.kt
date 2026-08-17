package com.whitecall.app.ui.settings

import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitecall.app.BuildConfig
import com.whitecall.app.R
import com.whitecall.app.ui.components.AppSnackbarHost
import com.whitecall.app.ui.components.AppTimePickerDialog
import com.whitecall.app.ui.components.SectionHeader
import com.whitecall.app.ui.components.showCustomSnackbar
import com.whitecall.app.ui.theme.StatusActive
import com.whitecall.app.ui.theme.StatusInactive
import com.whitecall.app.util.PermissionHelper
import com.whitecall.app.util.UpdateChecker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val scheduleSettings by viewModel.scheduleSettings.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var faqHowItWorksExpanded by remember { mutableStateOf(false) }
    var faqPermissionsExpanded by remember { mutableStateOf(false) }
    var faqPrivacyExpanded by remember { mutableStateOf(false) }
    var faqOemExpanded by remember { mutableStateOf(false) }

    // Role Manager Launcher & Lifecycle refresh
    var isRoleHeld by remember {
        mutableStateOf(PermissionHelper.isCallScreeningRoleHeld(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isRoleHeld = PermissionHelper.isCallScreeningRoleHeld(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isRoleHeld = PermissionHelper.isCallScreeningRoleHeld(context)
    }

    // JSON Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(
                context = context,
                uri = uri,
                onSuccess = { count ->
                    scope.showCustomSnackbar(
                        snackbarHostState,
                        context.getString(R.string.msg_export_success, count)
                    )
                },
                onError = {
                    scope.showCustomSnackbar(
                        snackbarHostState,
                        context.getString(R.string.msg_export_error)
                    )
                }
            )
        }
    }

    // JSON Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(
                context = context,
                uri = uri,
                onSuccess = { count ->
                    scope.showCustomSnackbar(
                        snackbarHostState,
                        context.getString(R.string.msg_import_success, count)
                    )
                },
                onError = {
                    scope.showCustomSnackbar(
                        snackbarHostState,
                        context.getString(R.string.msg_import_error)
                    )
                }
            )
        }
    }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = Color(0xFFCBD5E1),
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        uncheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    )

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // 1. Call Screening Permission Card
            SectionHeader(title = stringResource(R.string.section_permissions))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (isRoleHeld) StatusActive else StatusInactive,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.card_call_screening_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = if (isRoleHeld) StatusActive.copy(alpha = 0.15f) else StatusInactive.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(if (isRoleHeld) R.string.status_granted else R.string.status_not_granted),
                                color = if (isRoleHeld) StatusActive else StatusInactive,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.card_call_screening_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isRoleHeld && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val roleManager = context.getSystemService(RoleManager::class.java)
                                val intent = roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                if (intent != null) {
                                    roleLauncher.launch(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.btn_set_default))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Block Mode Selector (Reject vs Silence)
            val currentBlockMode by viewModel.blockMode.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.setting_block_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode 1: Reject
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setBlockMode(com.whitecall.app.data.preferences.AppPreferences.BLOCK_MODE_REJECT) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentBlockMode == com.whitecall.app.data.preferences.AppPreferences.BLOCK_MODE_REJECT,
                            onClick = { viewModel.setBlockMode(com.whitecall.app.data.preferences.AppPreferences.BLOCK_MODE_REJECT) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.block_mode_reject),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.block_mode_reject_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mode 2: Silence (no drop, rings for caller, phone silent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setBlockMode(com.whitecall.app.data.preferences.AppPreferences.BLOCK_MODE_SILENCE) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentBlockMode == com.whitecall.app.data.preferences.AppPreferences.BLOCK_MODE_SILENCE,
                            onClick = { viewModel.setBlockMode(com.whitecall.app.data.preferences.AppPreferences.BLOCK_MODE_SILENCE) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.block_mode_silence),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.block_mode_silence_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Compact 1-Row Theme Selector (Dark vs Light)
            val appTheme by viewModel.appTheme.collectAsState()
            SectionHeader(title = stringResource(R.string.section_theme))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isDark = appTheme != "light"
                    FilterChip(
                        selected = isDark,
                        onClick = { viewModel.setAppTheme("dark") },
                        label = { Text(stringResource(R.string.theme_dark), modifier = Modifier.padding(vertical = 2.dp)) },
                        leadingIcon = if (isDark) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = !isDark,
                        onClick = { viewModel.setAppTheme("light") },
                        label = { Text(stringResource(R.string.theme_light), modifier = Modifier.padding(vertical = 2.dp)) },
                        leadingIcon = if (!isDark) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Compact 1-Row Language Selector (Русский vs English)
            val appLanguage by viewModel.appLanguage.collectAsState()
            SectionHeader(title = stringResource(R.string.section_language))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isRu = appLanguage != "en"
                    FilterChip(
                        selected = isRu,
                        onClick = { viewModel.setAppLanguage("ru") },
                        label = { Text(stringResource(R.string.lang_ru), modifier = Modifier.padding(vertical = 2.dp)) },
                        leadingIcon = if (isRu) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = !isRu,
                        onClick = { viewModel.setAppLanguage("en") },
                        label = { Text(stringResource(R.string.lang_en), modifier = Modifier.padding(vertical = 2.dp)) },
                        leadingIcon = if (!isRu) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Schedule Mode
            SectionHeader(title = stringResource(R.string.section_schedule))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.schedule_enable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.schedule_enable_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = scheduleSettings.isEnabled,
                            onCheckedChange = {
                                viewModel.updateScheduleSettings(
                                    context,
                                    scheduleSettings.copy(isEnabled = it)
                                )
                            },
                            colors = switchColors
                        )
                    }

                    if (scheduleSettings.isEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Time range buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showStartTimePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.schedule_start_time),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", scheduleSettings.startHour, scheduleSettings.startMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showEndTimePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = stringResource(R.string.schedule_end_time),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", scheduleSettings.endHour, scheduleSettings.endMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.schedule_days),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Weekday Chips
                        val days = listOf(
                            Calendar.MONDAY to R.string.day_mon,
                            Calendar.TUESDAY to R.string.day_tue,
                            Calendar.WEDNESDAY to R.string.day_wed,
                            Calendar.THURSDAY to R.string.day_thu,
                            Calendar.FRIDAY to R.string.day_fri,
                            Calendar.SATURDAY to R.string.day_sat,
                            Calendar.SUNDAY to R.string.day_sun
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            days.forEach { (calDay, stringResId) ->
                                val isSelected = scheduleSettings.activeDays.contains(calDay)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newDays = if (isSelected) {
                                            if (scheduleSettings.activeDays.size > 1) {
                                                scheduleSettings.activeDays - calDay
                                            } else scheduleSettings.activeDays
                                        } else {
                                            scheduleSettings.activeDays + calDay
                                        }
                                        viewModel.updateScheduleSettings(
                                            context,
                                            scheduleSettings.copy(activeDays = newDays)
                                        )
                                    },
                                    label = { Text(stringResource(stringResId)) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. List Backup & Restore (2 Compact buttons in 1 row)
            SectionHeader(title = stringResource(R.string.section_backup))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("whitecall_backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_export),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_export_short))
                    }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_import),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_import_short))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. App Updates Section (GitHub Releases) - placed right after Backup
            SectionHeader(title = stringResource(R.string.section_updates))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WhiteCall",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.checkForUpdates() },
                        shape = RoundedCornerShape(12.dp),
                        enabled = updateState !is UpdateUiState.Checking
                    ) {
                        if (updateState is UpdateUiState.Checking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(stringResource(R.string.btn_check_updates))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 8. Quick Help & FAQ Accordions
            SectionHeader(title = stringResource(R.string.section_faq))

            FaqAccordionItem(
                title = stringResource(R.string.faq_how_it_works_title),
                content = stringResource(R.string.faq_how_it_works_desc),
                isExpanded = faqHowItWorksExpanded,
                onToggle = { faqHowItWorksExpanded = !faqHowItWorksExpanded }
            )

            FaqAccordionItem(
                title = stringResource(R.string.faq_permissions_title),
                content = stringResource(R.string.faq_permissions_desc),
                isExpanded = faqPermissionsExpanded,
                onToggle = { faqPermissionsExpanded = !faqPermissionsExpanded }
            )

            FaqAccordionItem(
                title = stringResource(R.string.faq_privacy_title),
                content = stringResource(R.string.faq_privacy_desc),
                isExpanded = faqPrivacyExpanded,
                onToggle = { faqPrivacyExpanded = !faqPrivacyExpanded }
            )

            FaqAccordionItem(
                title = stringResource(R.string.faq_oem_title),
                content = stringResource(R.string.faq_oem_desc),
                isExpanded = faqOemExpanded,
                onToggle = { faqOemExpanded = !faqOemExpanded }
            )
        }
    }

    // Update Result Dialog / Snackbar
    when (val state = updateState) {
        is UpdateUiState.Success -> {
            if (state.info.hasUpdate) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    title = { Text(stringResource(R.string.dialog_update_title, state.info.latestVersion)) },
                    text = {
                        Column {
                            if (state.info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = state.info.releaseNotes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.msg_update_available, state.info.latestVersion),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                UpdateChecker.openDownloadUrl(context, state.info.downloadUrl.ifBlank { state.info.releaseUrl })
                                viewModel.dismissUpdateDialog()
                            }
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_download_update))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                )
            } else {
                LaunchedEffect(state) {
                    scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_no_updates, BuildConfig.VERSION_NAME))
                    viewModel.dismissUpdateDialog()
                }
            }
        }
        is UpdateUiState.Error -> {
            LaunchedEffect(state) {
                scope.showCustomSnackbar(snackbarHostState, "Ошибка проверки: ${state.message}")
                viewModel.dismissUpdateDialog()
            }
        }
        else -> {}
    }

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        AppTimePickerDialog(
            title = stringResource(R.string.schedule_start_time),
            initialHour = scheduleSettings.startHour,
            initialMinute = scheduleSettings.startMinute,
            onConfirm = { hour, minute ->
                showStartTimePicker = false
                viewModel.updateScheduleSettings(
                    context,
                    scheduleSettings.copy(startHour = hour, startMinute = minute)
                )
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        AppTimePickerDialog(
            title = stringResource(R.string.schedule_end_time),
            initialHour = scheduleSettings.endHour,
            initialMinute = scheduleSettings.endMinute,
            onConfirm = { hour, minute ->
                showEndTimePicker = false
                viewModel.updateScheduleSettings(
                    context,
                    scheduleSettings.copy(endHour = hour, endMinute = minute)
                )
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
fun FaqAccordionItem(
    title: String,
    content: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
