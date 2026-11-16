package com.example.smsriskanalysis.receivers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import com.example.smsriskanalysis.R
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.smsriskanalysis.MainActivity
import com.example.smsriskanalysis.data.repository.ContactRepository
import androidx.core.net.toUri
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val CHANNEL_ID = "incoming_sms_channel"
        private const val CHANNEL_NAME = "Incoming Messages"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isNullOrEmpty()) {
                Log.e("SmsReceiver", "No messages received")
                return
            }

            val sender = messages[0].originatingAddress

            val messageBody = messages.joinToString(separator = "") {
                it.messageBody.orEmpty()
            }

            if (sender.isNullOrBlank() || messageBody.isNullOrBlank()) {
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

            if (insertedUri != null) {
                Log.d("SmsReceiver", "SMS successfully saved: $insertedUri")
                Log.d("SmsReceiver", "Sender: $sender")
                Log.d("SmsReceiver", "Message Body: $messageBody")

                showSmsNotification(
                    context,
                    sender,
                    messageBody,
                    threatId
                )
            } else {
                Log.e("SmsReceiver", "Failed to insert SMS into the database")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error processing SMS", e)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showSmsNotification(
        context: Context,
        sender: String,
        messageBody: String,
        threatId: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted")
            return
        }

        val contactRepository = ContactRepository(context.contentResolver)
        val contact = contactRepository.getContactByPhoneNumber(sender)
        val contactName = contact?.name ?: sender
        val photoUri = contact?.photoUri

        val contactPhoto = photoUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri.toUri())?.use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Failed to load contact photo", e)
                null
            }
        }

        // Use the app's default profile image if no contact photo exists
        val notificationPhoto = contactPhoto
            ?: BitmapFactory.decodeResource(
                context.resources,
                R.drawable.default_image
            )

        val senderPerson= Person.Builder()
            .setName(contactName)
            .setIcon(IconCompat.createWithBitmap(notificationPhoto))
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(contactName).addMessage(
            messageBody,
            System.currentTimeMillis(),
            senderPerson
        )

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("sender", sender)
            putExtra("threatId", threatId)
        }

        val notificationId = System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(notificationPhoto)
            .setContentTitle(contactName)
            .setContentText(messageBody)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Reply", pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            notificationId,
            notification
        )

        Log.d(TAG, "SMS notification displayed")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for incoming SMS messages"
            }

            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }
}