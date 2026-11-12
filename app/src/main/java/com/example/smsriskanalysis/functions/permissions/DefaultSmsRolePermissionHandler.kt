package com.example.smsriskanalysis.functions.permissions

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

fun isDefaultSmsApp(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            return true
        }
    }
    return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
}

fun canRequestDefaultSmsRole(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
    }
    return true
}

fun createDefaultSmsRoleRequestIntent(context: Context): Intent? {
    if (isDefaultSmsApp(context)) {
        return null
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        }
        return null
    } else {
        return Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
        }
    }
}

@Composable
fun DefaultSmsRoleHandler(
    onRoleGranted: () -> Unit,
    onRoleDenied: () -> Unit,
    onRequestableDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnRoleGranted by rememberUpdatedState(onRoleGranted)
    val currentOnRoleDenied by rememberUpdatedState(onRoleDenied)
    val currentOnRequestableDenied by rememberUpdatedState(onRequestableDenied)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val isGranted = isDefaultSmsApp(context)

        Log.d(
            "DefaultSmsRoleHandler",
            "Role request resultCode = ${result.resultCode}, isGranted=$isGranted"
        )
        if (isGranted) {
            currentOnRoleGranted()
        } else {
            currentOnRoleDenied()
        }
    }

    return {
        Log.d("DefaultSmsRole", "Checking SMS role status")

        if (isDefaultSmsApp(context)) {
            Log.d("DefaultSmsRole", "App already holds SMS role")
            currentOnRoleGranted()
        } else {
            val intent = createDefaultSmsRoleRequestIntent(context)
            if (intent != null) {
                Log.d("DefaultSmsRole", "Requesting SMS role")
                launcher.launch(intent)
            } else {
                Log.w(
                    "DefaultSmsRole", "Check Android version, role availability, " +
                            "and whether this app qualifies for the SMS role."
                )
                currentOnRequestableDenied()
            }
        }
    }
}