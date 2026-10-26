package com.example.smsriskanalysis.functions.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState

fun isDefaultSmsApp(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return false
    }
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return false

    return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
}

fun canRequestDefaultSmsRole(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return false
    }

    val roleManager = context.getSystemService(RoleManager::class.java) ?: return false

    return roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
}

fun createDefaultSmsRoleRequestIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return null
    }

    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null

    if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
        return null
    }

    if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
        return null
    }

    return roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
}

@Composable
fun DefaultSmsRoleHandler(
    onRoleGranted: () -> Unit,
    onRoleDenied: () -> Unit,
    onRequestableDenied: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentOnRoleGranted = rememberUpdatedState(onRoleGranted)
    val currentOnRequestUnavailable = rememberUpdatedState(onRequestableDenied)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (isDefaultSmsApp(context)) {
            currentOnRoleGranted.value()
        } else {
            currentOnRequestUnavailable.value()
        }
    }
}

