package com.example.smsriskanalysis.data.repository

import android.content.ContentResolver
import android.provider.Telephony
import androidx.compose.runtime.mutableStateOf
import com.example.smsriskanalysis.models.ConversationPage
import com.example.smsriskanalysis.models.ConversationRowData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(
    private val contentResolver: ContentResolver,
    private val contactRepository: ContactRepository
) {
    suspend fun getConversationsPage(
        limit: Int = 50,
        offset: Int = 0
    ): ConversationPage = withContext(Dispatchers.IO) {
        require(limit > 0) { "Limit must be a positive integer" }
        require(offset >= 0) { "Offset must be a non-negative integer" }

        val seenThreadIds = mutableSetOf<Long>()
        val pageMessages = mutableListOf<SmsMessageData>()

        var uniqueCount = 0
        var hasMore = false

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.TYPE
        )

        val sortOrder = "${Telephony.Sms.DATE} DESC, ${Telephony.Sms._ID} DESC"

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

                // skip older messages
                if (!seenThreadIds.add(threadId)) {
                    continue
                }

                if (uniqueCount >= offset + limit) {
                    hasMore = true
                    break
                }

                if (uniqueCount >= offset) {
                    pageMessages.add(
                        SmsMessageData(
                            id = cursor.getLong(idIndex),
                            threadId = threadId,
                            address = cursor.getString(addressIndex),
                            body = cursor.getString(bodyIndex).orEmpty(),
                            date = cursor.getLong(dateIndex),
                            type = cursor.getInt(typeIndex)
                        )
                    )
                }
                uniqueCount++
            }
        }

        val conversations = pageMessages.map { message ->
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

        ConversationPage(
            conversations = conversations,
            hasMore = hasMore,
            nextOffset = offset + conversations.size
        )
    }

    suspend fun searchConversations(
        query: String,
        limit: Int = 50,
        offset: Int = 0,
    ): ConversationSearchPage = withContext(Dispatchers.IO) {
        require(limit > 0) { "Limit must be a positive integer" }
        require(offset >= 0) { "Offset must be a non-negative integer" }
        val keyword = query.trim()
        if (keyword.isBlank()) {
            return@withContext ConversationSearchPage(
                conversations = emptyList(),
                hasMore = false,
                nextOffset = offset
            )
        }

        val matchingThreadIds = mutableListOf<Long>()
        val seenThreadIds = mutableSetOf<Long>()

        val matchProjection = arrayOf(
            Telephony.Sms.THREAD_ID
        )

        val selection =
            "(${Telephony.Sms.BODY} LIKE ? OR " +
                    "${Telephony.Sms.ADDRESS} LIKE ?)"

        val searchPattern = "%$keyword%"

        val targetCount = offset + limit + 1

        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            matchProjection,
            selection,
            arrayOf(searchPattern, searchPattern),
            "${Telephony.Sms.DATE} DESC, ${Telephony.Sms._ID} DESC"
        )?.use { cursor ->
            val threadIdINdex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)

            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(threadIdINdex)
                if (seenThreadIds.add(threadId)) {
                    matchingThreadIds.add(threadId)
                    if (matchingThreadIds.size >= targetCount) {
                        break
                    }
                }
            }
        }

        val hasMore = matchingThreadIds.size > offset + limit

        val pageThreadIds = matchingThreadIds.drop(offset).take(limit)

        if (pageThreadIds.isEmpty()) {
            return@withContext ConversationSearchPage(
                conversations = emptyList(),
                hasMore = false,
                nextOffset = offset
            )
        }

        val placeHolders = pageThreadIds.joinToString(",") { "?" }

        val latestMessageSelection = "${Telephony.Sms.THREAD_ID} IN ($placeHolders)"

        val messageProjection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.TYPE
        )

        val lastMessages = linkedMapOf<Long, ConversationRowData>()

        contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            messageProjection,
            latestMessageSelection,
            pageThreadIds.map { it.toString() }.toTypedArray(),
            "${Telephony.Sms.DATE} DESC, ${Telephony.Sms._ID} DESC"
        )?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val threadIdIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)

            while (cursor.moveToNext()) {
                val threadId = cursor.getLong(threadIdIndex)
                if (lastMessages.containsKey(threadId)) {
                    continue
                }
                val address = cursor.getString(addressIndex).orEmpty()
                val body = cursor.getString(bodyIndex).orEmpty()
                val date = cursor.getLong(dateIndex)

                val contact = address.takeIf { it.isNotBlank() }
                    ?.let { contactRepository.getContactByPhoneNumber(it) }

                lastMessages[threadId] = ConversationRowData(
                    threadId,
                    address,
                    contactName = contact?.name,
                    contact?.photoUri,
                    latestMessage = body,
                    latestMessageTime = date,
                    unreadCount = 0
                )
            }
        }

        val conversations = lastMessages.values.toList()

        ConversationSearchPage(
            conversations,
            hasMore,
            nextOffset = offset + conversations.size
        )
    }

    data class SmsMessageData(
        val id: Long,
        val threadId: Long,
        val address: String?,
        val body: String,
        val date: Long,
        val type: Int
    )

    data class ConversationSearchPage(
        val conversations: List<ConversationRowData>,
        val hasMore: Boolean,
        val nextOffset: Int
    )
}