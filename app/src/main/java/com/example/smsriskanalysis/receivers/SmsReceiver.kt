package com.example.smsriskanalysis.receivers

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if(messages.isNullOrEmpty()) {
                Log.e("SmsReceiver", "No messages received")
                return
            }

            val sender = messages[0].originatingAddress

            val messageBody = messages.joinToString(separator = "") {
                it.messageBody.orEmpty()
            }

            if(sender.isNullOrBlank() || messageBody.isNullOrBlank()) {
                Log.e("SmsReceiver", "Sender or message body is null or blank")
                return
            }

            val threatId = Telephony.Threads.getOrCreateThreadId(
                context,
                sender
            )

            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, messageBody)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(
                    Telephony.Sms.DATE_SENT,
                    messages[0].timestampMillis
                )
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                put(Telephony.Sms.THREAD_ID, threatId)
            }

            val insertedUri = context?.contentResolver?.insert(
                Telephony.Sms.Inbox.CONTENT_URI,
                values
            )

            if(insertedUri != null) {
                Log.d("SmsReceiver", "SMS successfully saved: $insertedUri")
                Log.d("SmsReceiver", "Sender: $sender")
                Log.d("SmsReceiver", "Message Body: $messageBody")
            }else {
                Log.e("SmsReceiver", "Failed to insert SMS into the database")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS", e)
        }
    }
}