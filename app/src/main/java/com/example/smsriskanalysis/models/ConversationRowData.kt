package com.example.smsriskanalysis.models

data class ConversationRowData(
    val contact: String,
    val name: String,
    val message: String,
    val time: String,
    val profileLink: String?,
    val profileLinkNumber: Int?,
    val profileDescription: String?
)