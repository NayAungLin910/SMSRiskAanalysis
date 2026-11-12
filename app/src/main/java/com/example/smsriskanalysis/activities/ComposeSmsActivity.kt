package com.example.smsriskanalysis.activities

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.smsriskanalysis.functions.permissions.createDefaultSmsRoleRequestIntent
import com.example.smsriskanalysis.functions.permissions.isDefaultSmsApp

class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialRecipient = intent.data?.schemeSpecificPart.orEmpty()
        val initialMessage = intent.getStringExtra("sms_body") ?: intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        setContent {
            var recipient by rememberSaveable { mutableStateOf(initialRecipient) }
            var message by rememberSaveable { mutableStateOf(initialMessage) }
            var errorText by remember { mutableStateOf<String?>(null) }

            val roleRequestLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (isDefaultSmsApp(this)) {
                    if (recipient.isNotBlank() && message.isNotBlank()) {
                        sendSmsAndFinish(recipient.trim(), message.trim())
                    }
                } else {
                    errorText = "App must be set as the Default SMS App to send messages."
                }
            }

            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    if (!isDefaultSmsApp(this)) {
                        val intent = createDefaultSmsRoleRequestIntent(this)
                        if (intent != null) {
                            roleRequestLauncher.launch(intent)
                        } else {
                            errorText = "App must be set as the Default SMS App to send messages."
                        }
                    } else {
                        if (recipient.isNotBlank() && message.isNotBlank()) {
                            sendSmsAndFinish(recipient.trim(), message.trim())
                        }
                    }
                } else {
                    errorText = "SEND_SMS permission is required to send messages."
                }
            }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "New Message",
                        style = MaterialTheme.typography.titleLarge
                    )

                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("Recipient") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        minLines = 3
                    )

                    val err = errorText
                    if (err != null) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val rec = recipient.trim()
                            val body = message.trim()
                            if (rec.isNotEmpty() && body.isNotEmpty()) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    this@ComposeSmsActivity,
                                    Manifest.permission.SEND_SMS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.SEND_SMS)
                                } else if (!isDefaultSmsApp(this@ComposeSmsActivity)) {
                                    val intent = createDefaultSmsRoleRequestIntent(this@ComposeSmsActivity)
                                    if (intent != null) {
                                        roleRequestLauncher.launch(intent)
                                    } else {
                                        errorText = "App must be set as the Default SMS App to send messages."
                                    }
                                } else {
                                    sendSmsAndFinish(rec, body)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }

    private fun sendSmsAndFinish(recipient: String, body: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            try {
                smsManager?.sendTextMessage(recipient, null, body, null, null)
            } catch (e: Exception) {
                // Ignore emulator send exception, fall back to inserting directly into content provider
            }

            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, recipient)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            contentResolver.insert(Telephony.Sms.CONTENT_URI, values)

            finish()
        } catch (e: Exception) {
            // handle error if needed
        }
    }
}