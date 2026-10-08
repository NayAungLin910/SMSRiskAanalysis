package com.example.smsriskanalysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smsriskanalysis.composables.ConversationRow
import com.example.smsriskanalysis.models.ConversationRowData
import com.example.smsriskanalysis.ui.theme.SMSRiskAnalysisTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.smsriskanalysis.composables.ConversationSearchBar
import com.example.smsriskanalysis.functions.search.searchConversations

val conversations = listOf<ConversationRowData>(
    ConversationRowData(
        contact = "Amazon Customer Service and Online Shopping Support Department Japan",
        name = "Amazon Support",
        message = "Your package has been shipped and will arrive tomorrow.",
        time = "10:42",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Amazon"
    ),
    ConversationRowData(
        contact = "John Smith International Business Development and Customer Relations Team",
        name = "John Smith",
        message = "Hey, are you free this evening?",
        time = "10:42:35",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "John Smith"
    ),
    ConversationRowData(
        contact = "Unknown Bank Account Security Verification and Customer Protection Department",
        name = "Bank Verification Team",
        message = "Your bank account has been suspended. Please verify your information immediately.",
        time = "Yesterday at 10:42",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Unknown"
    ),
    ConversationRowData(
        contact = "Yamada Taro Tokyo International Software Engineering Development Department",
        name = "Yamada Taro",
        message = "Let's meet at the station tomorrow.",
        time = "Yesterday at 10:42 PM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Yamada Taro"
    ),
    ConversationRowData(
        contact = "Japan Post Package Delivery and International Shipping Customer Support Center",
        name = "Japan Post Support",
        message = "Your delivery address needs to be confirmed before we can deliver your package.",
        time = "Wednesday, 10:42",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Japan Post"
    ),
    ConversationRowData(
        contact = "Rakuten Online Shopping Membership and Premium Customer Service Support",
        name = "Rakuten Service",
        message = "Thank you for using our service. Your order information has been updated.",
        time = "Wednesday, October 7",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Rakuten"
    ),
    ConversationRowData(
        contact = "Microsoft Windows Security Account Protection and Technical Support Department",
        name = "Microsoft Support",
        message = "We detected unusual activity on your account. Please review your security settings.",
        time = "Wednesday, October 7 at 10:42",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Microsoft"
    ),
    ConversationRowData(
        contact = "Yamada Hanako Osaka University Research and International Student Support Office",
        name = "Yamada Hanako",
        message = "The meeting schedule has been changed to Friday afternoon.",
        time = "Yesterday at 11:59:59 PM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Yamada Hanako"
    ),
    ConversationRowData(
        contact = "Seven Bank ATM Transaction Verification and Customer Financial Support Center",
        name = "Seven Bank Customer Center",
        message = "Please check your latest transaction history and contact us if you notice anything unusual.",
        time = "October 7, 2026 10:42",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Seven Bank"
    ),
    ConversationRowData(
        contact = "Google Account Security Verification and Personal Information Protection Department",
        name = "Google Security Team",
        message = "Your Google account requires additional security verification.",
        time = "October 7, 2026 at 10:42 AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Google"
    ),
    ConversationRowData(
        contact = "SoftGuide Software Development and Japan Myanmar Engineering Communication Team",
        name = "SoftGuide Dev Team",
        message = "The latest development task has been assigned to your project.",
        time = "2026-10-07 10:42",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "SoftGuide"
    ),
    ConversationRowData(
        contact = "Japan Immigration Services Agency Residence Status and Foreign Student Support Division",
        name = "Immigration Support Division",
        message = "Please check the latest information regarding your residence status application.",
        time = "2026-10-07 10:42:35",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Immigration Services"
    ),
    ConversationRowData(
        contact = "Netflix Japan Premium Membership Payment and Account Management Customer Service",
        name = "Netflix Customer Service",
        message = "Your monthly subscription payment could not be processed successfully.",
        time = "Wed, Oct 7, 2026 at 10:42 AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Netflix"
    ),
    ConversationRowData(
        contact = "Mizuho Bank Online Banking Account Security and Unauthorized Transaction Investigation Team",
        name = "Mizuho Security Team",
        message = "A new login was detected on your online banking account.",
        time = "Wednesday, October 7, 2026 at 10:42:35 AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Mizuho Bank"
    ),
    ConversationRowData(
        contact = "ECC Computer College Osaka System Engineering Department Student Support Office",
        name = "ECC Student Office",
        message = "Please remember to check your timetable and upcoming assignments.",
        time = "2026/10/07 10:42:35 AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "ECC Computer College"
    ),
    ConversationRowData(
        contact = "International Parcel Delivery Customs Clearance and Import Documentation Support Center",
        name = "Customs Support Center",
        message = "Additional information is required to complete the customs clearance process.",
        time = "Wednesday 7 October 2026 10:42 AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "International Delivery"
    ),
    ConversationRowData(
        contact = "Tokyo Electric Power Electricity Bill Payment and Customer Account Management Department",
        name = "TEPCO Account Dept",
        message = "Your latest electricity bill is now available for online payment.",
        time = "Wednesday, October 7, 2026, 10:42:35 AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Electricity Service"
    ),
    ConversationRowData(
        contact = "Osaka Municipal Government Tax Payment and Resident Services Information Center",
        name = "Osaka Tax Info Center",
        message = "Please review the latest notification regarding your municipal tax payment.",
        time = "Wednesday, October 7, 2026 at exactly 10:42:35 AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Osaka Municipal Government"
    ),
    ConversationRowData(
        contact = "Japanese Language Education International Student Academic Guidance and Career Support Office",
        name = "Academic Guidance Office",
        message = "Your academic guidance appointment has been scheduled for next week.",
        time = "Wednesday, October 7, 2026 at 10:42:35 in the morning",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Academic Support"
    ),
    ConversationRowData(
        contact = "International Credit Card Payment Verification and Fraud Prevention Customer Support Department",
        name = "Fraud Prevention Dept",
        message = "We need to verify a recent payment made using your credit card.",
        time = "Wednesday October seventh two thousand twenty-six at ten forty-two AM",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Credit Card Support"
    ),
    ConversationRowData(
        contact = "Japanese Government Administrative Services Digital Identification Verification Center",
        name = "Digital ID Verification Center",
        message = "Please complete the required verification process using the official website.",
        time = "This is an intentionally extremely long timestamp for overflow testing",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Government Services"
    ),
    ConversationRowData(
        contact = "International Online Investment Financial Consultation and Asset Management Advisory Department",
        name = "Asset Management Advisory",
        message = "Your investment account has received a new notification regarding your portfolio.",
        time = "This is another extremely long time value that should definitely overflow",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Investment Support"
    ),
    ConversationRowData(
        contact = "Mobile Network Customer Account Security and Telecommunications Service Support Center Japan",
        name = "Mobile Telecom Support",
        message = "Your mobile service account requires confirmation of your registered information.",
        time = "Wednesday October 7 2026 at 10:42:35 AM Japan Standard Time",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Mobile Network"
    ),
    ConversationRowData(
        contact = "International Job Recruitment Career Consulting and Foreign Student Employment Support Center",
        name = "Career Consulting Center",
        message = "A new position matching your software engineering experience is available.",
        time = "This timestamp is deliberately ridiculously long to test the maximum possible overflow behavior",
        profileLink = null,
        profileLinkNumber = R.drawable.default_image,
        profileDescription = "Career Support"
    )
)



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSRiskAnalysisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyScreen(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        conversations
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    name: String,
    modifier: Modifier = Modifier,
    conversations: List<ConversationRowData>
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var displayedConversations = if(searchQuery.isBlank()) {
        conversations
    } else {
        searchConversations(searchQuery, conversations)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConversationSearchBar(query = searchQuery, onQueryChange = { query -> searchQuery = query })
        LazyColumn() {
            items(displayedConversations) { conversation ->
                ConversationRow(
                    contact = conversation.contact,
                    message = conversation.message,
                    time = conversation.time,
                    profilelink = conversation.profileLink,
                    profileLinkNumber = conversation.profileLinkNumber,
                    profileDescription = conversation.profileDescription
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SMSRiskAnalysisTheme {
        MyScreen(
            "Android",
            conversations = conversations
        )
    }
}