package com.whitecall.app.ui.whitelist

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitecall.app.R
import com.whitecall.app.domain.model.GroupItem
import com.whitecall.app.domain.model.WhiteListEntry
import com.whitecall.app.ui.components.AppSnackbarHost
import com.whitecall.app.ui.components.AppTimePickerDialog
import com.whitecall.app.ui.components.EmptyStateView
import com.whitecall.app.ui.components.PermissionRationaleDialog
import com.whitecall.app.ui.components.showCustomSnackbar
import com.whitecall.app.ui.theme.StatusActive
import com.whitecall.app.ui.theme.StatusInactive
import com.whitecall.app.util.ContactHelper
import com.whitecall.app.util.PermissionHelper
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhiteListScreen(
    viewModel: WhiteListViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val folders by viewModel.folderList.collectAsState()
    val allGroups by viewModel.groups.collectAsState()
    val expandedGroupIds by viewModel.expandedGroupIds.collectAsState()
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsState()
    val allowAllContacts by viewModel.allowAllContacts.collectAsState()
    val scheduleSettings by viewModel.scheduleSettings.collectAsState()

    var isScheduleExpanded by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var showFabMenu by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var targetGroupIdForManualAdd by remember { mutableStateOf<Long?>(null) }

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf<GroupItem?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf<GroupItem?>(null) }
    var showMoveContactDialog by remember { mutableStateOf<WhiteListEntry?>(null) }
    var showContactsRationale by remember { mutableStateOf(false) }

    var targetGroupIdForContactPicker by remember { mutableStateOf<Long?>(null) }

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

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = Color(0xFFCBD5E1),
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        uncheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    )

    // System Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val picked = ContactHelper.extractContactFromUri(context, uri)
                if (picked != null) {
                    viewModel.addContactNumber(picked.name, picked.phoneNumber, targetGroupIdForContactPicker)
                    scope.showCustomSnackbar(
                        snackbarHostState,
                        context.getString(R.string.msg_number_added)
                    )
                }
            }
        }
    }

    // Contact Permission Launcher
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dialog_add_number_title))
                }

                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.whitelist_add_group)) },
                        leadingIcon = {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                        },
                        onClick = {
                            showFabMenu = false
                            showAddGroupDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.whitelist_add_from_contacts)) },
                        leadingIcon = {
                            Icon(Icons.Default.Contacts, contentDescription = null)
                        },
                        onClick = {
                            showFabMenu = false
                            targetGroupIdForContactPicker = null
                            if (ContactHelper.hasContactsPermission(context)) {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            } else {
                                showContactsRationale = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.whitelist_add_manual)) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            showFabMenu = false
                            targetGroupIdForManualAdd = null
                            showManualAddDialog = true
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Warning Banner if Call Screening is not set as default
            AnimatedVisibility(
                visible = !isRoleHeld,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.banner_call_screening_warning_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.banner_call_screening_warning_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                            Text(stringResource(R.string.btn_enable_screening), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Main Protection Settings Panel (Framed Card with Border)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // A. Master Call Protection Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.protection_master_switch),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.protection_master_switch_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { viewModel.setProtectionEnabled(context, it) },
                            colors = switchColors
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // B. Schedule Settings Row
                    val dayLabels = listOf(
                        Calendar.MONDAY to stringResource(R.string.day_mon),
                        Calendar.TUESDAY to stringResource(R.string.day_tue),
                        Calendar.WEDNESDAY to stringResource(R.string.day_wed),
                        Calendar.THURSDAY to stringResource(R.string.day_thu),
                        Calendar.FRIDAY to stringResource(R.string.day_fri),
                        Calendar.SATURDAY to stringResource(R.string.day_sat),
                        Calendar.SUNDAY to stringResource(R.string.day_sun)
                    )
                    val scheduleDaysSummary = if (scheduleSettings.activeDays.size == 7) {
                        "ежедневно"
                    } else {
                        dayLabels.filter { scheduleSettings.activeDays.contains(it.first) }.joinToString(", ") { it.second }
                    }
                    val scheduleSummary = String.format(
                        "%02d:%02d – %02d:%02d (%s)",
                        scheduleSettings.startHour,
                        scheduleSettings.startMinute,
                        scheduleSettings.endHour,
                        scheduleSettings.endMinute,
                        scheduleDaysSummary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = scheduleSettings.isEnabled) {
                                isScheduleExpanded = !isScheduleExpanded
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.schedule_enable),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (scheduleSettings.isEnabled && !isScheduleExpanded) {
                                Text(
                                    text = scheduleSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.schedule_enable_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (scheduleSettings.isEnabled) {
                            IconButton(
                                onClick = { isScheduleExpanded = !isScheduleExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isScheduleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Switch(
                            checked = scheduleSettings.isEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) isScheduleExpanded = true
                                viewModel.updateScheduleSettings(
                                    context,
                                    scheduleSettings.copy(isEnabled = enabled)
                                )
                            },
                            colors = switchColors
                        )
                    }

                    AnimatedVisibility(visible = scheduleSettings.isEnabled && isScheduleExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.schedule_start_time),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = { showStartTimePicker = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d:%02d", scheduleSettings.startHour, scheduleSettings.startMinute),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.schedule_end_time),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedButton(
                                    onClick = { showEndTimePicker = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d:%02d", scheduleSettings.endHour, scheduleSettings.endMinute),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.schedule_days),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dayLabels.forEach { (calDay, label) ->
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
                                        label = { Text(label, fontSize = 11.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = { isScheduleExpanded = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Свернуть", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    // C. Allow All Contacts Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.whitelist_allow_all_contacts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.whitelist_allow_all_contacts_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = allowAllContacts,
                            onCheckedChange = { enabled ->
                                if (enabled && !ContactHelper.hasContactsPermission(context)) {
                                    showContactsRationale = true
                                }
                                viewModel.onToggleAllowAllContacts(enabled)
                            },
                            colors = switchColors
                        )
                    }
                }
            }

            // 5. Folders List or Empty State
            if (folders.isEmpty()) {
                if (allowAllContacts) {
                    EmptyStateView(
                        iconRes = R.drawable.ic_contact,
                        title = stringResource(R.string.whitelist_contacts_allowed_title),
                        description = stringResource(R.string.whitelist_contacts_allowed_desc),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    EmptyStateView(
                        iconRes = R.drawable.ic_shield,
                        title = stringResource(R.string.whitelist_empty_title),
                        description = stringResource(R.string.whitelist_empty_desc),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(folders, key = { it.group.id }) { item ->
                        val isExpanded = expandedGroupIds.contains(item.group.id)
                        FolderCardItem(
                            folder = item,
                            isExpanded = isExpanded,
                            switchColors = switchColors,
                            onToggleExpand = { viewModel.toggleGroupExpanded(item.group.id) },
                            onToggleActive = { active -> viewModel.setGroupActive(item.group.id, active) },
                            onEdit = { showEditGroupDialog = item.group },
                            onDelete = { showDeleteGroupDialog = item.group },
                            onAddManual = {
                                targetGroupIdForManualAdd = item.group.id
                                showManualAddDialog = true
                            },
                            onAddContact = {
                                targetGroupIdForContactPicker = item.group.id
                                if (ContactHelper.hasContactsPermission(context)) {
                                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                                    contactPickerLauncher.launch(intent)
                                } else {
                                    showContactsRationale = true
                                }
                            },
                            onMoveEntry = { entry ->
                                showMoveContactDialog = entry
                            },
                            onDeleteEntry = { entryId ->
                                viewModel.deleteEntry(entryId)
                                scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_number_deleted))
                            }
                        )
                    }
                }
            }
        }
    }

    // Move Contact to Folder Dialog
    if (showMoveContactDialog != null) {
        val entry = showMoveContactDialog!!
        var selectedGroupId by remember { mutableStateOf(entry.groupId ?: (allGroups.firstOrNull()?.id ?: 1L)) }
        var showNewFolderInput by remember { mutableStateOf(false) }
        var newFolderNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showMoveContactDialog = null },
            title = { Text(stringResource(R.string.dialog_move_contact_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${entry.displayName} (${entry.phoneNumber})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (!showNewFolderInput) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            allGroups.forEach { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedGroupId = group.id }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedGroupId == group.id,
                                        onClick = { selectedGroupId = group.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = group.name, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { showNewFolderInput = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_create_and_move))
                        }
                    } else {
                        OutlinedTextField(
                            value = newFolderNameInput,
                            onValueChange = { newFolderNameInput = it },
                            label = { Text(stringResource(R.string.input_group_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (showNewFolderInput) {
                            if (newFolderNameInput.isNotBlank()) {
                                viewModel.moveEntryToNewGroup(entry.id, newFolderNameInput) {
                                    val groupName = newFolderNameInput
                                    showMoveContactDialog = null
                                    scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_contact_moved, groupName))
                                }
                            }
                        } else {
                            viewModel.moveEntryToGroup(entry.id, selectedGroupId)
                            val targetName = allGroups.firstOrNull { it.id == selectedGroupId }?.name ?: ""
                            showMoveContactDialog = null
                            scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_contact_moved, targetName))
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_move))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveContactDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Manual Add Number Dialog
    if (showManualAddDialog) {
        var nameInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text(stringResource(R.string.dialog_add_number_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.input_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = {
                            phoneInput = it
                            isError = false
                        },
                        label = { Text(stringResource(R.string.input_phone_label)) },
                        placeholder = { Text(stringResource(R.string.input_phone_placeholder)) },
                        isError = isError,
                        supportingText = if (isError) {
                            { Text(stringResource(R.string.error_invalid_phone)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneInput.isBlank() || phoneInput.filter { it.isDigit() }.length < 3) {
                            isError = true
                        } else {
                            viewModel.addManualNumber(
                                name = nameInput,
                                phoneNumber = phoneInput,
                                groupId = targetGroupIdForManualAdd,
                                onSuccess = {
                                    showManualAddDialog = false
                                    scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_number_added))
                                },
                                onError = { isError = true }
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Add Group Dialog
    if (showAddGroupDialog) {
        var groupNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text(stringResource(R.string.dialog_add_group_title)) },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text(stringResource(R.string.input_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupNameInput.isNotBlank()) {
                            viewModel.addGroup(groupNameInput) {
                                showAddGroupDialog = false
                                scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_group_created))
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Edit Group Dialog
    if (showEditGroupDialog != null) {
        val group = showEditGroupDialog!!
        var groupNameInput by remember { mutableStateOf(group.name) }

        AlertDialog(
            onDismissRequest = { showEditGroupDialog = null },
            title = { Text(stringResource(R.string.dialog_edit_group_title)) },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text(stringResource(R.string.input_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupNameInput.isNotBlank()) {
                            viewModel.updateGroup(group.id, groupNameInput, group.isActive)
                            showEditGroupDialog = null
                            scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_group_updated))
                        }
                    }
                ) {
                    Text(stringResource(R.string.btn_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGroupDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Delete Group Dialog
    if (showDeleteGroupDialog != null) {
        val group = showDeleteGroupDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = null },
            title = { Text(stringResource(R.string.dialog_delete_group_title)) },
            text = { Text(stringResource(R.string.dialog_delete_folder_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(group.id)
                        showDeleteGroupDialog = null
                        scope.showCustomSnackbar(snackbarHostState, context.getString(R.string.msg_group_deleted))
                    }
                ) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Contacts Permission Rationale Dialog
    if (showContactsRationale) {
        PermissionRationaleDialog(
            title = stringResource(R.string.perm_contacts_title),
            description = stringResource(R.string.perm_contacts_desc),
            onGrant = {
                showContactsRationale = false
                contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            },
            onDismiss = { showContactsRationale = false }
        )
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
fun FolderCardItem(
    folder: GroupWithEntries,
    isExpanded: Boolean,
    switchColors: SwitchDefaultsColors? = null,
    onToggleExpand: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddManual: () -> Unit,
    onAddContact: () -> Unit,
    onMoveEntry: (WhiteListEntry) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val group = folder.group
    val entries = folder.entries
    var folderSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredEntries = if (folderSearchQuery.isBlank()) {
        entries
    } else {
        val q = folderSearchQuery.trim().lowercase()
        entries.filter {
            it.displayName.lowercase().contains(q) || it.phoneNumber.contains(q)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Folder Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder Icon with active indicator
                Box(contentAlignment = Alignment.BottomEnd) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (group.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (group.isActive) StatusActive else StatusInactive,
                                CircleShape
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.group_numbers_count, entries.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Switch for folder active status
                Switch(
                    checked = group.isActive,
                    onCheckedChange = onToggleActive,
                    colors = switchColors ?: SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color(0xFFCBD5E1),
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        uncheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                )

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded Folder Content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // TOP ACTION BAR: [+ Из контактов] [+ Вручную] [🔍 Поиск]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddContact,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.whitelist_add_from_contacts), fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onAddManual,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.whitelist_add_manual), fontSize = 11.sp, maxLines = 1)
                        }

                        IconButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) folderSearchQuery = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = null,
                                tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // In-Folder Search Field
                    AnimatedVisibility(visible = isSearchActive) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = folderSearchQuery,
                                onValueChange = { folderSearchQuery = it },
                                placeholder = { Text(stringResource(R.string.folder_search_hint), fontSize = 12.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    if (folderSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { folderSearchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredEntries.isEmpty()) {
                        Text(
                            text = if (folderSearchQuery.isNotBlank()) "Номера не найдены" else "В этой папке пока нет номеров",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        filteredEntries.forEach { entry ->
                            FolderContactRow(
                                entry = entry,
                                onMove = { onMoveEntry(entry) },
                                onDelete = { onDeleteEntry(entry.id) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

typealias SwitchDefaultsColors = androidx.compose.material3.SwitchColors

@Composable
fun FolderContactRow(
    entry: WhiteListEntry,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = entry.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (entry.displayName != entry.phoneNumber) {
                    Text(
                        text = entry.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Move to another folder button
            IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.DriveFileMove,
                    contentDescription = stringResource(R.string.dialog_move_contact_title),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.btn_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
