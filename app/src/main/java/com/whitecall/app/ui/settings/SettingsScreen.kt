package com.whitecall.app.ui.settings

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitecall.app.BuildConfig
import com.whitecall.app.R
import com.whitecall.app.data.preferences.AppPreferences
import com.whitecall.app.ui.components.AppSnackbarHost
import com.whitecall.app.ui.components.SectionHeader
import com.whitecall.app.ui.components.showCustomSnackbar
import com.whitecall.app.ui.theme.StatusActive
import com.whitecall.app.util.PermissionHelper
import com.whitecall.app.util.UpdateChecker

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val appLanguage by viewModel.appLanguage.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val blockMode by viewModel.blockMode.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var showAboutAppDialog by remember { mutableStateOf(false) }

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
            // 1. System Permissions (Clean Single Row)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isRoleHeld) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.card_call_screening_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isRoleHeld) stringResource(R.string.screening_role_active_desc) else stringResource(R.string.screening_role_inactive_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRoleHeld) StatusActive else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!isRoleHeld) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    val intent = roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                    if (intent != null) {
                                        roleLauncher.launch(intent)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.btn_set_default), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (e2: Exception) {
                                        val intent = Intent(Settings.ACTION_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.btn_change_screening), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Call Blocking Mode (Compact chips)
            SectionHeader(title = stringResource(R.string.setting_block_mode_title))
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
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = blockMode == AppPreferences.BLOCK_MODE_SILENCE,
                        onClick = { viewModel.setBlockMode(AppPreferences.BLOCK_MODE_SILENCE) },
                        label = { Text(stringResource(R.string.block_mode_silence_short), fontSize = 12.sp) },
                        leadingIcon = if (blockMode == AppPreferences.BLOCK_MODE_SILENCE) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = blockMode == AppPreferences.BLOCK_MODE_REJECT,
                        onClick = { viewModel.setBlockMode(AppPreferences.BLOCK_MODE_REJECT) },
                        label = { Text(stringResource(R.string.block_mode_reject_short), fontSize = 12.sp) },
                        leadingIcon = if (blockMode == AppPreferences.BLOCK_MODE_REJECT) {
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

            // 3. App Theme Selector
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
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = appTheme == "dark",
                        onClick = { viewModel.setAppTheme("dark") },
                        label = { Text(stringResource(R.string.theme_dark), fontSize = 12.sp) },
                        leadingIcon = if (appTheme == "dark") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = appTheme == "light",
                        onClick = { viewModel.setAppTheme("light") },
                        label = { Text(stringResource(R.string.theme_light), fontSize = 12.sp) },
                        leadingIcon = if (appTheme == "light") {
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

            // 4. App Language Selector
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
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = appLanguage == "ru",
                        onClick = { viewModel.setAppLanguage("ru") },
                        label = { Text(stringResource(R.string.lang_ru), fontSize = 12.sp) },
                        leadingIcon = if (appLanguage == "ru") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = appLanguage == "en",
                        onClick = { viewModel.setAppLanguage("en") },
                        label = { Text(stringResource(R.string.lang_en), fontSize = 12.sp) },
                        leadingIcon = if (appLanguage == "en") {
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

            // 5. List Backup & Restore (2 Compact buttons in 1 row)
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

            // 6. About App (Single clickable card opening full Help & Updates)
            SectionHeader(title = stringResource(R.string.setting_about_app))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { showAboutAppDialog = true },
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
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_about_app),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.setting_about_app_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Full About App Dialog with FAQ & Updates
    if (showAboutAppDialog) {
        AboutAppDialog(
            updateState = updateState,
            onCheckUpdates = { viewModel.checkForUpdates() },
            onDismissUpdateState = { viewModel.dismissUpdateDialog() },
            onDismiss = { showAboutAppDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppDialog(
    updateState: UpdateUiState,
    onCheckUpdates: () -> Unit,
    onDismissUpdateState: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogSnackbarHostState = remember { SnackbarHostState() }

    var faqScreeningExpanded by remember { mutableStateOf(false) }
    var faqHowItWorksExpanded by remember { mutableStateOf(false) }
    var faqPrivacyExpanded by remember { mutableStateOf(false) }
    var faqOemExpanded by remember { mutableStateOf(false) }

    // React to update state right inside this dialog
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateUiState.Success -> {
                if (!updateState.info.hasUpdate) {
                    scope.showCustomSnackbar(
                        dialogSnackbarHostState,
                        context.getString(R.string.msg_no_updates, BuildConfig.VERSION_NAME)
                    )
                    onDismissUpdateState()
                }
            }
            is UpdateUiState.Error -> {
                scope.showCustomSnackbar(
                    dialogSnackbarHostState,
                    "Ошибка проверки: ${updateState.message}"
                )
                onDismissUpdateState()
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            snackbarHost = { AppSnackbarHost(dialogSnackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.setting_about_app), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_cancel))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                // Header Brand Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shield),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "WhiteCall",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FAQ Accordions
                SectionHeader(title = stringResource(R.string.section_faq))

                FaqAccordionItem(
                    title = stringResource(R.string.faq_screening_title),
                    content = stringResource(R.string.faq_screening_desc),
                    isExpanded = faqScreeningExpanded,
                    onToggle = { faqScreeningExpanded = !faqScreeningExpanded }
                )

                FaqAccordionItem(
                    title = stringResource(R.string.faq_how_it_works_title),
                    content = stringResource(R.string.faq_how_it_works_desc),
                    isExpanded = faqHowItWorksExpanded,
                    onToggle = { faqHowItWorksExpanded = !faqHowItWorksExpanded }
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

                Spacer(modifier = Modifier.height(12.dp))

                // Check for updates section at the bottom of About App
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
                                text = "GitHub Releases",
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
                            onClick = onCheckUpdates,
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
            }
        }
    }

    // New version available dialog
    if (updateState is UpdateUiState.Success && updateState.info.hasUpdate) {
        val info = updateState.info
        AlertDialog(
            onDismissRequest = onDismissUpdateState,
            title = { Text(stringResource(R.string.dialog_update_title, info.latestVersion)) },
            text = {
                Column {
                    if (info.releaseNotes.isNotBlank()) {
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.msg_update_available, info.latestVersion),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        UpdateChecker.openDownloadUrl(context, info.downloadUrl.ifBlank { info.releaseUrl })
                        onDismissUpdateState()
                    }
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_download_update))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdateState) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
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
