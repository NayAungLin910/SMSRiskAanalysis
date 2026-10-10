package com.example.smsriskanalysis.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smsriskanalysis.composables.components.ProfileImage
import com.example.smsriskanalysis.models.ConversationRowData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationRow(
    conversation: ConversationRowData,
    modifier: Modifier = Modifier
) {
    val displayName = conversation.contactName ?: conversation.address
    val timeString = formatTimestamp(conversation.latestMessageTime)
    ConversationRow(
        contact = displayName,
        message = conversation.latestMessage,
        time = timeString,
        profilelink = conversation.contactPhotoUri,
        profileDescription = displayName,
        modifier = modifier
    )
}

@Composable
fun ConversationRow(
    contact: String,
    message: String,
    time: String,
    profilelink: String?,
    profileDescription: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        ProfileImage(
            profileLink = profilelink,
            contentDescription = profileDescription
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact,
                    modifier = Modifier.weight(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = time,
                    modifier = Modifier.weight(0.3f),
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }

            Text(
                text = message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}