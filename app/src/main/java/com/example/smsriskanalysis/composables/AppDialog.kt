package com.example.smsriskanalysis.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

enum class AppDialogType {
    CONFIRMATION,
    MESSAGE,
    PERMISSION
}

@Composable
fun AppDialog(
    title: String,
    message: String,
    type: AppDialogType,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    dismissRequest: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = {
            if (dismissRequest) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm?.invoke()

                    if (type == AppDialogType.MESSAGE) {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = when (type) {
                        AppDialogType.PERMISSION -> "Open Settings"
                        else -> confirmText
                    },
                    color = if (type == AppDialogType.CONFIRMATION) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary

                    }
                )
            }
        },
        dismissButton = {
            if (type != AppDialogType.MESSAGE) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text(
                        text = dismissText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.extraLarge
    )
}