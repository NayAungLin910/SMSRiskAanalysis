package com.example.smsriskanalysis.functions.permissions

import androidx.compose.runtime.Composable
import android.Manifest

@Composable
fun SmsPermissionHandler(
    requestKey: Int = 0,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
){
    RuntimePermissionHandler(
        permission = Manifest.permission.READ_SMS,
        requestKey = requestKey,
        onPermissionGranted = onPermissionGranted,
        onPermissionDenied = onPermissionDenied
    )
}