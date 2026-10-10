package com.example.smsriskanalysis.functions.search

import com.example.smsriskanalysis.models.ConversationRowData

fun searchConversations(
    query: String,
    conversations: List<ConversationRowData>
): List<ConversationRowData> {
    return conversations.filter { conversation ->
        conversation.address.contains(query, ignoreCase = true) ||
        (conversation.contactName?.contains(query, ignoreCase = true) == true) ||
        conversation.latestMessage.contains(query, ignoreCase = true)
    }
}