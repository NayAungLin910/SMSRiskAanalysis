package com.example.smsriskanalysis.functions.permissions

import androidx.compose.runtime.Composable
import android.Manifest

@Composable
fun ContactPermissionHandler(
    requestKey: Int = 0,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
){
    RuntimePermissionHandler(
        permission = Manifest.permission.READ_CONTACTS,
        requestKey = requestKey,
        onPermissionGranted = onPermissionGranted,
        onPermissionDenied = onPermissionDenied
    )
}