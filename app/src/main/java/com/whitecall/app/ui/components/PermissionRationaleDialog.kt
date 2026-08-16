package com.whitecall.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.whitecall.app.R

@Composable
fun PermissionRationaleDialog(
    iconRes: Int = R.drawable.ic_contact,
    title: String,
    description: String,
    onGrant: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(onClick = onGrant) {
                Text(stringResource(R.string.btn_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_later))
            }
        }
    )
}
