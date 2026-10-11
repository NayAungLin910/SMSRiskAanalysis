package com.example.smsriskanalysis.data.repository

import android.content.ContentResolver
import android.provider.Telephony
import com.example.smsriskanalysis.models.ConversationRowData

class SmsRepository(
    private val contentResolver: ContentResolver,
    private val contactRepository: ContactRepository
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
            val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(threadIdIndex)
                if (latestMessages.containsKey(threadId)) {
                    continue
                    continue
                }
                val message = SmsMessageData(
                    id = cursor.getLong(idIndex),
                    threadId = cursor.getLong(threadIdIndex),
                    address = cursor.getString(addressIndex),
                    body = cursor.getString(bodyIndex).orEmpty(),
                    date = cursor.getLong(dateIndex),
                    type = cursor.getInt(typeIndex)
                )
                latestMessages[threadId] = message
            }
        }

        return latestMessages.values.map { message ->

            val contact = message.address
                ?.takeIf { it.isNotBlank() }
                ?.let { address ->
                    contactRepository.getContactByPhoneNumber(address)
                }

            ConversationRowData(
                threadId = message.threadId,
                address = message.address.orEmpty(),
                contactName = contact?.name,
                contactPhotoUri = contact?.photoUri,
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