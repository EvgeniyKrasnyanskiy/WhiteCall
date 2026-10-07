package com.whitecall.app.ui.history

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitecall.app.R
import com.whitecall.app.ui.components.AppSnackbarHost
import com.whitecall.app.ui.components.EmptyStateView
import com.whitecall.app.ui.components.showCustomSnackbar
import com.whitecall.app.ui.theme.StatusActive
import com.whitecall.app.util.ContactHelper
import com.whitecall.app.util.PhoneUtils

@Composable
fun BlockedLogScreen(
    viewModel: BlockedLogViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    val uiState by viewModel.uiState.collectAsState()
    val dateGroups = uiState.dateGroups
    val totalCount = uiState.totalCount

    var showClearDialog by remember { mutableStateOf(false) }
    var targetForActionDialog by remember { mutableStateOf<GroupedBlockedCall?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header with action
            if (totalCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.blocked_log_title)} ($totalCount)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.btn_clear_log),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (dateGroups.isEmpty()) {
                EmptyStateView(
                    iconRes = R.drawable.ic_block,
                    title = stringResource(R.string.blocked_log_empty_title),
                    description = stringResource(R.string.blocked_log_empty_desc),
                    modifier = Modifier.weight(1f)
                )
            } else {
                val todayStr = stringResource(R.string.log_date_today)
                val yesterdayStr = stringResource(R.string.log_date_yesterday)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateGroups.forEach { dateGroup ->
                        item(key = "header_${dateGroup.startOfDayMillis}") {
                            Text(
                                text = PhoneUtils.formatDateGroup(dateGroup.startOfDayMillis, todayStr, yesterdayStr),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(dateGroup.items, key = { it.key }) { item ->
                            GroupedBlockedCallItem(
                                item = item,
                                onCall = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(item.phoneNumber)}")).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {}
                                },
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(item.phoneNumber))
                                    scope.showCustomSnackbar(
                                        snackbarHostState,
                                        context.getString(R.string.msg_number_copied)
                                    )
                                },
                                onAddClick = {
                                    targetForActionDialog = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add to Whitelist or Contacts Dialog
    if (targetForActionDialog != null) {
        val target = targetForActionDialog!!
        val isTargetWhitelisted = target.isWhitelisted

        AlertDialog(
            onDismissRequest = { targetForActionDialog = null },
            title = {
                Text(
                    text = stringResource(R.string.dialog_add_number_action_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = target.callerName?.let { "$it (${target.phoneNumber})" } ?: target.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Whitelist Action (Add or Remove)
                    if (isTargetWhitelisted) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.removeFromWhiteList(target.phoneNumber) {
                                        scope.showCustomSnackbar(
                                            snackbarHostState,
                                            context.getString(R.string.msg_number_removed_whitelist)
                                        )
                                    }
                                    targetForActionDialog = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.action_remove_from_whitelist),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addToWhiteList(target.phoneNumber, target.callerName) {
                                        scope.showCustomSnackbar(
                                            snackbarHostState,
                                            context.getString(R.string.msg_number_added)
                                        )
                                    }
                                    targetForActionDialog = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shield),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.action_add_to_whitelist),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Option 2: Save to device contacts or View in Contacts
                    if (target.isContact) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val uri = ContactHelper.getContactLookupUri(context, target.phoneNumber)
                                    val viewIntent = if (uri != null) {
                                        Intent(Intent.ACTION_VIEW, uri)
                                    } else {
                                        Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                                    }.apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(viewIntent)
                                    } catch (_: Exception) {}
                                    targetForActionDialog = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_contact),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.action_open_in_contacts),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val insertIntent = Intent(Intent.ACTION_INSERT).apply {
                                        type = ContactsContract.RawContacts.CONTENT_TYPE
                                        putExtra(ContactsContract.Intents.Insert.PHONE, target.phoneNumber)
                                        if (!target.callerName.isNullOrBlank() && target.callerName != target.phoneNumber) {
                                            putExtra(ContactsContract.Intents.Insert.NAME, target.callerName)
                                        }
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(insertIntent)
                                    } catch (_: Exception) {}
                                    targetForActionDialog = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_contact),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.action_save_to_contacts),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { targetForActionDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_log_title)) },
            text = { Text(stringResource(R.string.dialog_clear_log_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun GroupedBlockedCallItem(
    item: GroupedBlockedCall,
    onCall: () -> Unit,
    onCopy: () -> Unit,
    onAddClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val latest = item.latestCall
    val isWhitelisted = item.isWhitelisted
    val isAllowedByContacts = item.isAllowedByContacts
    val isAllowed = item.isAllowed
    val hasMultipleCalls = item.calls.size > 1
    val isValidNumber = item.phoneNumber.filter { it.isDigit() }.length >= 3

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasMultipleCalls) Modifier.clickable { isExpanded = !isExpanded }
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon badge
                Surface(
                    shape = CircleShape,
                    color = if (isAllowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val badgeIcon = when {
                            isWhitelisted -> R.drawable.ic_check
                            isAllowedByContacts -> R.drawable.ic_contact
                            else -> R.drawable.ic_call_missed
                        }
                        Icon(
                            painter = painterResource(id = badgeIcon),
                            contentDescription = null,
                            tint = if (isAllowed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val displayName = item.callerName ?: item.phoneNumber
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (hasMultipleCalls) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "(${item.calls.size})",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (item.callerName != null && item.callerName != item.phoneNumber) {
                        Text(
                            text = item.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = PhoneUtils.formatTimeOnly(latest.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isWhitelisted) {
                            Text(
                                text = "• ${stringResource(R.string.status_in_whitelist)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusActive,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (isAllowedByContacts) {
                            Text(
                                text = "• ${stringResource(R.string.status_in_contacts)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusActive,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            val reasonText = if (latest.reason == "ANONYMOUS_CALLER") {
                                stringResource(R.string.blocked_reason_anonymous)
                            } else {
                                stringResource(R.string.blocked_reason_not_in_whitelist)
                            }
                            Text(
                                text = "• $reasonText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // If multiple calls, show expand chevron
                if (hasMultipleCalls) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Quick actions row: Call, Copy, Add to whitelist/contacts
            if (isValidNumber) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call Button
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.btn_call),
                            tint = StatusActive,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Copy Number Button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.btn_copy_number),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Add or Remove Whitelist Button
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isWhitelisted) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.btn_remove_from_whitelist),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add),
                                contentDescription = stringResource(R.string.btn_add_to_whitelist),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Expanded details: list of previous calls from this number on this date
            if (hasMultipleCalls) {
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.log_expand_details),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        item.calls.forEachIndexed { index, call ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${item.calls.size - index}  •  ${PhoneUtils.formatTimeOnly(call.timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (call.reason == "ANONYMOUS_CALLER") {
                                    Text(
                                        text = stringResource(R.string.blocked_reason_anonymous),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


