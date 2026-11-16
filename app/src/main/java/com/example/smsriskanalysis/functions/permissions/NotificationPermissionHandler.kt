package com.example.smsriskanalysis.functions.permissions

import android.os.Build
import androidx.compose.runtime.Composable
import java.security.Permission

@Composable
fun NotificationPermissionHandler(
    requestKey: Int = 0,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RuntimePermissionHandler(
            permission = android.Manifest.permission.POST_NOTIFICATIONS,
            requestKey = requestKey,
            onPermissionGranted = onPermissionGranted,
            onPermissionDenied = onPermissionDenied
        )
    } else {
        onPermissionGranted()
    }
}