package com.whitecall.app.ui.whitelist

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitecall.app.R
import com.whitecall.app.domain.model.GroupItem
import com.whitecall.app.domain.model.WhiteListEntry
import com.whitecall.app.ui.components.EmptyStateView
import com.whitecall.app.ui.components.PermissionRationaleDialog
import com.whitecall.app.ui.theme.StatusActive
import com.whitecall.app.ui.theme.StatusInactive
import com.whitecall.app.util.ContactHelper
import kotlinx.coroutines.launch

@Composable
fun WhiteListScreen(
    viewModel: WhiteListViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val entries by viewModel.entries.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allowAllContacts by viewModel.allowAllContacts.collectAsState()

    var showFabMenu by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf<GroupItem?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf<GroupItem?>(null) }
    var showContactsRationale by remember { mutableStateOf(false) }

    // System Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val picked = ContactHelper.extractContactFromUri(context, uri)
                if (picked != null) {
                    viewModel.addContactNumber(picked.name, picked.phoneNumber)
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_number_added))
                    }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        text = { Text(stringResource(R.string.whitelist_add_from_contacts)) },
                        leadingIcon = {
                            Icon(Icons.Default.Contacts, contentDescription = null)
                        },
                        onClick = {
                            showFabMenu = false
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
                            showManualDialog = true
                        }
                    )
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
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text(stringResource(R.string.whitelist_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // 2. Allow All Contacts Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                            style = MaterialTheme.typography.bodyMedium,
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // 3. Groups Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedGroupId == null,
                    onClick = { viewModel.selectGroup(null) },
                    label = { Text("${stringResource(R.string.group_all)}") },
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = selectedGroupId == -1L,
                    onClick = { viewModel.selectGroup(-1L) },
                    label = { Text(stringResource(R.string.group_unassigned)) },
                    shape = RoundedCornerShape(12.dp)
                )

                groups.forEach { group ->
                    FilterChip(
                        selected = selectedGroupId == group.id,
                        onClick = { viewModel.selectGroup(group.id) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (group.isActive) StatusActive else StatusInactive,
                                        CircleShape
                                    )
                            )
                        },
                        label = { Text(group.name) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 4. Selected Group Banner (if a custom group is selected)
            val currentGroup = groups.firstOrNull { it.id == selectedGroupId }
            if (currentGroup != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentGroup.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentGroup.isActive) stringResource(R.string.group_toggle_active) else stringResource(R.string.group_toggle_inactive),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentGroup.isActive) StatusActive else StatusInactive,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = currentGroup.isActive,
                            onCheckedChange = { active ->
                                viewModel.setGroupActive(currentGroup.id, active)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        IconButton(onClick = { showEditGroupDialog = currentGroup }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteGroupDialog = currentGroup }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // 5. Numbers List or Empty State
            if (entries.isEmpty()) {
                EmptyStateView(
                    iconRes = R.drawable.ic_shield,
                    title = stringResource(R.string.whitelist_empty_title),
                    description = stringResource(R.string.whitelist_empty_desc),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        val entryGroupName = groups.firstOrNull { it.id == entry.groupId }?.name
                        WhiteListEntryItem(
                            entry = entry,
                            groupName = entryGroupName,
                            onDelete = {
                                viewModel.deleteEntry(entry.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_number_deleted))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Manual Add Number Dialog
    if (showManualDialog) {
        var nameInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showManualDialog = false },
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
                                onSuccess = {
                                    showManualDialog = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.msg_number_added))
                                    }
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
                TextButton(onClick = { showManualDialog = false }) {
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
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_group_created))
                                }
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
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_group_updated))
                            }
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
            text = { Text(stringResource(R.string.dialog_delete_group_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(group.id)
                        showDeleteGroupDialog = null
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_group_deleted))
                        }
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
}

@Composable
fun WhiteListEntryItem(
    entry: WhiteListEntry,
    groupName: String? = null,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
            // Initial circle avatar
            val initial = entry.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (entry.displayName != entry.phoneNumber) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (groupName != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.btn_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
