package com.example.smsriskanalysis.functions.search

import com.example.smsriskanalysis.models.ConversationRowData

fun searchConversations (
    query: String,
    converations: List<ConversationRowData>
): List<ConversationRowData> {
    val searchedResults = converations.filter { conversation ->
        conversation.contact.contains(query, ignoreCase = true) ||
        conversation.name.contains(query, ignoreCase = true) ||
        conversation.message.contains(query, ignoreCase = true)
    }
    return searchedResults
}