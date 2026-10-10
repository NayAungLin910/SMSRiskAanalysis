package com.example.smsriskanalysis

import com.example.smsriskanalysis.functions.permissions.ContactPermissionHandler
import com.example.smsriskanalysis.functions.permissions.SmsPermissionHandler
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smsriskanalysis.composables.ConversationRow
import com.example.smsriskanalysis.models.ConversationRowData
import com.example.smsriskanalysis.ui.theme.SMSRiskAnalysisTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.smsriskanalysis.composables.ConversationSearchBar
import com.example.smsriskanalysis.functions.search.searchConversations

val conversations = listOf<ConversationRowData>(
    ConversationRowData(
        threadId = 1L,
        address = "Amazon Japan",
        contactName = "Amazon Support",
        contactPhotoUri = null,
        latestMessage = "Your package has been shipped and will arrive tomorrow.",
        latestMessageTime = 1700000000000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 2L,
        address = "+1 555-0142",
        contactName = "John Smith",
        contactPhotoUri = null,
        latestMessage = "Hey, are you free this evening?",
        latestMessageTime = 1700003600000L,
        unreadCount = 1
    ),
    ConversationRowData(
        threadId = 3L,
        address = "+81 90-0000-0000",
        contactName = "Bank Verification Team",
        contactPhotoUri = null,
        latestMessage = "Your bank account has been suspended. Please verify your information immediately.",
        latestMessageTime = 1700007200000L,
        unreadCount = 2
    ),
    ConversationRowData(
        threadId = 4L,
        address = "+81 80-1111-2222",
        contactName = "Yamada Taro",
        contactPhotoUri = null,
        latestMessage = "Let's meet at the station tomorrow.",
        latestMessageTime = 1700010800000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 5L,
        address = "Japan Post",
        contactName = "Japan Post Support",
        contactPhotoUri = null,
        latestMessage = "Your delivery address needs to be confirmed before we can deliver your package.",
        latestMessageTime = 1700014400000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 6L,
        address = "Rakuten Service",
        contactName = "Rakuten Service",
        contactPhotoUri = null,
        latestMessage = "Thank you for using our service. Your order information has been updated.",
        latestMessageTime = 1700018000000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 7L,
        address = "Microsoft",
        contactName = "Microsoft Support",
        contactPhotoUri = null,
        latestMessage = "We detected unusual activity on your account. Please review your security settings.",
        latestMessageTime = 1700021600000L,
        unreadCount = 1
    ),
    ConversationRowData(
        threadId = 8L,
        address = "+81 80-3333-4444",
        contactName = "Yamada Hanako",
        contactPhotoUri = null,
        latestMessage = "The meeting schedule has been changed to Friday afternoon.",
        latestMessageTime = 1700025200000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 9L,
        address = "Seven Bank",
        contactName = "Seven Bank Customer Center",
        contactPhotoUri = null,
        latestMessage = "Please check your latest transaction history and contact us if you notice anything unusual.",
        latestMessageTime = 1700028800000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 10L,
        address = "Google",
        contactName = "Google Security Team",
        contactPhotoUri = null,
        latestMessage = "Your Google account requires additional security verification.",
        latestMessageTime = 1700032400000L,
        unreadCount = 1
    ),
    ConversationRowData(
        threadId = 11L,
        address = "SoftGuide",
        contactName = "SoftGuide Dev Team",
        contactPhotoUri = null,
        latestMessage = "The latest development task has been assigned to your project.",
        latestMessageTime = 1700036000000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 12L,
        address = "Immigration Services",
        contactName = "Immigration Support Division",
        contactPhotoUri = null,
        latestMessage = "Please check the latest information regarding your residence status application.",
        latestMessageTime = 1700039600000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 13L,
        address = "Netflix",
        contactName = "Netflix Customer Service",
        contactPhotoUri = null,
        latestMessage = "Your monthly subscription payment could not be processed successfully.",
        latestMessageTime = 1700043200000L,
        unreadCount = 1
    ),
    ConversationRowData(
        threadId = 14L,
        address = "Mizuho Bank",
        contactName = "Mizuho Security Team",
        contactPhotoUri = null,
        latestMessage = "A new login was detected on your online banking account.",
        latestMessageTime = 1700046800000L,
        unreadCount = 2
    ),
    ConversationRowData(
        threadId = 15L,
        address = "ECC College",
        contactName = "ECC Student Office",
        contactPhotoUri = null,
        latestMessage = "Please remember to check your timetable and upcoming assignments.",
        latestMessageTime = 1700050400000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 16L,
        address = "Customs",
        contactName = "Customs Support Center",
        contactPhotoUri = null,
        latestMessage = "Additional information is required to complete the customs clearance process.",
        latestMessageTime = 1700054000000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 17L,
        address = "TEPCO",
        contactName = "TEPCO Account Dept",
        contactPhotoUri = null,
        latestMessage = "Your latest electricity bill is now available for online payment.",
        latestMessageTime = 1700057600000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 18L,
        address = "Osaka Govt",
        contactName = "Osaka Tax Info Center",
        contactPhotoUri = null,
        latestMessage = "Please review the latest notification regarding your municipal tax payment.",
        latestMessageTime = 1700061200000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 19L,
        address = "Academic Center",
        contactName = "Academic Guidance Office",
        contactPhotoUri = null,
        latestMessage = "Your academic guidance appointment has been scheduled for next week.",
        latestMessageTime = 1700064800000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 20L,
        address = "Credit Card Support",
        contactName = "Fraud Prevention Dept",
        contactPhotoUri = null,
        latestMessage = "We need to verify a recent payment made using your credit card.",
        latestMessageTime = 1700068400000L,
        unreadCount = 1
    ),
    ConversationRowData(
        threadId = 21L,
        address = "Digital ID",
        contactName = "Digital ID Verification Center",
        contactPhotoUri = null,
        latestMessage = "Please complete the required verification process using the official website.",
        latestMessageTime = 1700072000000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 22L,
        address = "Asset Management",
        contactName = "Asset Management Advisory",
        contactPhotoUri = null,
        latestMessage = "Your investment account has received a new notification regarding your portfolio.",
        latestMessageTime = 1700075600000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 23L,
        address = "Mobile Network",
        contactName = "Mobile Telecom Support",
        contactPhotoUri = null,
        latestMessage = "Your mobile service account requires confirmation of your registered information.",
        latestMessageTime = 1700079200000L,
        unreadCount = 0
    ),
    ConversationRowData(
        threadId = 24L,
        address = "Career Support",
        contactName = "Career Consulting Center",
        contactPhotoUri = null,
        latestMessage = "A new position matching your software engineering experience is available.",
        latestMessageTime = 1700082800000L,
        unreadCount = 0
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

    val context = LocalContext.current
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var hasSmsPermission by remember { mutableStateOf(false) }
    var hasContactPermission by remember { mutableStateOf(false) }
    var smsRequestKey by remember { mutableStateOf(0) }
    var contactRequestKey by remember { mutableStateOf(0) }

    SmsPermissionHandler(
        requestKey = smsRequestKey,
        onPermissionGranted = {
            hasSmsPermission = true
            showPermissionDeniedDialog = false
        },
        onPermissionDenied = {
            hasSmsPermission = false
            showPermissionDeniedDialog = true
        }
    )

    if (hasSmsPermission) {
        ContactPermissionHandler(
            requestKey = contactRequestKey,
            onPermissionGranted = {
                hasContactPermission = true
            },
            onPermissionDenied = {
                hasContactPermission = false
                showPermissionDeniedDialog = true
            }
        )
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("Permission Required")
            },
            text = {
                Text(
                    "SMS and Contacts permissions are required for " +
                            "this app to work properly.\n\n" +
                            "SMS permission: Allows the app to read your SMS " +
                            "messages so it can display your conversations and " +
                            "help identify potential scam messages.\n\n" +
                            "Contacts permission: Allows the app to access your " +
                            "contacts so it can display contact names alongside " +
                            "their phone numbers.\n\n" +
                            "You can manage these permissions in your device settings."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { (context as Activity).finish() }
                ) {
                    Text("Close App")
                }
            }
        )
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var displayedConversations = if (searchQuery.isBlank()) {
        conversations
    } else {
        searchConversations(searchQuery, conversations)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConversationSearchBar(query = searchQuery, onQueryChange = { query -> searchQuery = query })
        LazyColumn() {
            items(displayedConversations) { conversation ->
                ConversationRow(conversation = conversation)
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