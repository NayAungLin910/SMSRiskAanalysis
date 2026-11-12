package com.example.smsriskanalysis.functions.permissions

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun RuntimePermissionHandler(
    permission: String,
    requestKey: Int = 0,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnPermissionGranted by rememberUpdatedState(onPermissionGranted)
    val currentOnPermissionDenied by rememberUpdatedState(onPermissionDenied)

    var isRequestInProgress by remember { mutableStateOf(false) }
    val currentIsRequestInProgress by rememberUpdatedState(isRequestInProgress)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isRequestInProgress = false
        if (isGranted) {
            currentOnPermissionGranted()
        } else {
            currentOnPermissionDenied()
        }
    }

    // Request permission initially or when requestKey changes.
    LaunchedEffect(permission, requestKey) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            isRequestInProgress = false
            currentOnPermissionGranted()
        } else {
            isRequestInProgress = true
            launcher.launch(permission)
        }
    }

    // Recheck permission when returning from Settings or another activity.
    DisposableEffect(lifecycleOwner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !currentIsRequestInProgress) {
                val isGranted = ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
                if (isGranted) {
                    currentOnPermissionGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}