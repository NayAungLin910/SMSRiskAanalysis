package com.example.smsriskanalysis.composables.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.smsriskanalysis.R
import com.example.smsriskanalysis.ui.theme.SMSRiskAnalysisTheme

@Composable
fun ProfileImage(profileLink: String? = null, profileLinkNumber: Int? = null, contentDescription: String? = null){
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = profileLink ?: profileLinkNumber,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SMSRiskAnalysisTheme {
        ProfileImage(profileLinkNumber = R.drawable.default_image)
    }
}