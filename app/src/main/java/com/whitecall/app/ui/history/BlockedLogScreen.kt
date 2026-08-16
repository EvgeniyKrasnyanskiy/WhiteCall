package com.whitecall.app.ui.history

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whitecall.app.R
import com.whitecall.app.domain.model.BlockedCallLog
import com.whitecall.app.ui.components.EmptyStateView
import com.whitecall.app.util.PhoneUtils
import kotlinx.coroutines.launch

@Composable
fun BlockedLogScreen(
    viewModel: BlockedLogViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val blockedCalls by viewModel.blockedCalls.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header with action
            if (blockedCalls.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.blocked_log_title)} (${blockedCalls.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_clear_log),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (blockedCalls.isEmpty()) {
                EmptyStateView(
                    iconRes = R.drawable.ic_block,
                    title = stringResource(R.string.blocked_log_empty_title),
                    description = stringResource(R.string.blocked_log_empty_desc),
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockedCalls, key = { it.log.id }) { item ->
                        BlockedCallItem(
                            item = item,
                            onAddToWhiteList = {
                                viewModel.addToWhiteList(item.log.phoneNumber, item.log.callerName) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.msg_number_added)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
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
fun BlockedCallItem(
    item: BlockedCallUiItem,
    onAddToWhiteList: () -> Unit
) {
    val log = item.log
    val isWhitelisted = item.isWhitelisted

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
            // Blocked icon badge
            Surface(
                shape = CircleShape,
                color = if (isWhitelisted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = if (isWhitelisted) R.drawable.ic_check else R.drawable.ic_call_missed),
                        contentDescription = null,
                        tint = if (isWhitelisted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val displayName = log.callerName ?: log.phoneNumber
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (log.callerName != null && log.callerName != log.phoneNumber) {
                    Text(
                        text = log.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PhoneUtils.formatDateTime(log.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isWhitelisted) {
                        Text(
                            text = "• ${stringResource(R.string.status_in_whitelist)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.whitecall.app.ui.theme.StatusActive,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        val reasonText = if (log.reason == "ANONYMOUS_CALLER") {
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

            // Quick add to whitelist button (if not whitelisted and valid number)
            if (!isWhitelisted && log.phoneNumber.filter { it.isDigit() }.length >= 3) {
                IconButton(onClick = onAddToWhiteList) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = stringResource(R.string.btn_add_to_whitelist),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
