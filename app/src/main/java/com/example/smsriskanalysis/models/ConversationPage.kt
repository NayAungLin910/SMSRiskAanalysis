package com.example.smsriskanalysis.models

data class ConversationPage (
    val conversations: List<ConversationRowData>,
    val hasMore: Boolean,
    val nextOffset: Int,
)