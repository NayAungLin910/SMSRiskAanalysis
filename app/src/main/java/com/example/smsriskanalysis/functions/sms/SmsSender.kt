package com.example.smsriskanalysis.functions.sms

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

object SmsSender {
    sealed class SendResult {
        data object Success : SendResult()
        data class Failure(val reason: String) : SendResult()
    }

    fun send(
        context: Context,
        recipient: String,
        message: String
    ): SendResult {
        if (recipient.isBlank()) {
            return SendResult.Failure("Recipient cannot be blank")
        }

        if (message.isBlank()) {
            return SendResult.Failure("Message cannot be empty.")
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return SendResult.Failure("SEND_SMS permission is required.")
        }

        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
                    ?: return SendResult.Failure("SMS service is unavailable.")
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(
                    recipient,
                    null,
                    parts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    recipient,
                    null,
                    message,
                    null,
                    null
                )
            }

            // Insert into content://sms/sent so it is immediately recorded in telephony history and UI
            try {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, recipient)
                    put(Telephony.Sms.BODY, message)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
                context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            } catch (insertEx: Exception) {
                Log.w("SmsSender", "Failed to insert sent sms into provider", insertEx)
            }

            SendResult.Success
        } catch (e: Exception) {
            SendResult.Failure(e.message ?: "Failed to send SMS.")
        }
    }
}