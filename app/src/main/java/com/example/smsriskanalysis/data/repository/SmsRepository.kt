package com.example.smsriskanalysis.data.repository

import android.content.ContentResolver
import android.provider.Telephony
import com.example.smsriskanalysis.models.ConversationRowData

class SmsRepository(
    private val contentResolver: ContentResolver
) {
    fun getConversations(): List<ConversationRowData> {
        val latestMessages = linkedMapOf<Long, SmsMessageData>()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.TYPE,
        )

        val sortOrder = "${Telephony.Sms.DATE} Desc"

        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val threatIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (cursor.moveToNext()) {
                val threatId = cursor.getLong(threatIdIndex)
                if (latestMessages.containsKey(threatId)) {
                    continue
                }
                val message = SmsMessageData(
                    id = cursor.getLong(idIndex),
                    threadId = cursor.getLong(threatIdIndex),
                    address = cursor.getString(addressIndex),
                    body = cursor.getString(bodyIndex),
                    date = cursor.getLong(dateIndex),
                    type = cursor.getInt(typeIndex)
                )
                latestMessages[threatId] = message
            }
        }

        return latestMessages.values.map { message ->
            ConversationRowData(
                threadId = message.threadId,
                address = message.address.orEmpty(),
                contactName = null,
                contactPhotoUri = null,
                latestMessage = message.body,
                latestMessageTime = message.date,
                unreadCount = 0
            )
        }
    }

    private data class SmsMessageData(
        val id: Long,
        val threadId: Long,
        val address: String?,
        val body: String,
        val date: Long,
        val type: Int
    )
}