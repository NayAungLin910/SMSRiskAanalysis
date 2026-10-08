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

@Composable
fun ConversationRow(
    contact: String,
    message: String,
    time: String,
    profilelink: String?,
    profileLinkNumber: Int?,
    profileDescription: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        ProfileImage(
            profilelink, profileLinkNumber, profileDescription
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