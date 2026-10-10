package com.example.smsriskanalysis.models

data class ConversationRowData(
    val threadId: Long,
    val address: String,
    val contactName: String?,
    val contactPhotoUri: String?,
    val latestMessage: String,
    val latestMessageTime: Long,
    val unreadCount: Int
)