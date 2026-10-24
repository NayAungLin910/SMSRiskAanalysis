package com.example.smsriskanalysis

import com.example.smsriskanalysis.functions.permissions.ContactPermissionHandler
import com.example.smsriskanalysis.functions.permissions.SmsPermissionHandler
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smsriskanalysis.composables.ConversationRow
import com.example.smsriskanalysis.models.ConversationRowData
import com.example.smsriskanalysis.ui.theme.SMSRiskAnalysisTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smsriskanalysis.composables.ConversationSearchBar
import com.example.smsriskanalysis.composables.MainNavbar
import com.example.smsriskanalysis.data.repository.ContactRepository
import com.example.smsriskanalysis.data.repository.SmsRepository
import com.example.smsriskanalysis.layout.SearchScreen
import com.example.smsriskanalysis.observers.SmsContentObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
) {

    val context = LocalContext.current

    val contentResolver = context.contentResolver

    val contactRepository = remember(context) {
        ContactRepository(contentResolver)
    }

    val smsRepository = remember(context) { SmsRepository(contentResolver, contactRepository) }

    val listState = rememberLazyListState();

    var hasAppliedInitialScroll by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var isSearchScreen by remember {
        mutableStateOf(false)
    }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var hasSmsPermission by remember { mutableStateOf(false) }
    var hasContactPermission by remember { mutableStateOf(false) }
    var smsRequestKey by remember { mutableStateOf(0) }
    var contactRequestKey by remember { mutableStateOf(0) }

    var conversations by remember {
        mutableStateOf<List<ConversationRowData>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    val pageSize = 50

    var hasMoreConversations by remember {
        mutableStateOf(true)
    }

    var nextConversationOffset by remember {
        mutableStateOf(0)
    }

    var isLoadingMore by remember {
        mutableStateOf(false)
    }

    var loadMoreError by remember {
        mutableStateOf<String?>(null)
    }

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

    LaunchedEffect(conversations.isNotEmpty(), isLoading, isSearchScreen) {
        if (conversations.isNotEmpty() && !isLoading && !hasAppliedInitialScroll && !isSearchScreen) {
            listState.animateScrollToItem(1)
            hasAppliedInitialScroll = true
        }
    }

    LaunchedEffect(hasSmsPermission, hasContactPermission) {
        if (!hasSmsPermission || !hasContactPermission) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        loadError = null

        //Reset pagination when starting a fresh load
        conversations = emptyList()
        nextConversationOffset = 0
        hasMoreConversations = true
        isLoadingMore = false
        loadMoreError = null

        try {
            val page = smsRepository.getConversationsPage(
                limit = pageSize,
                offset = 0
            )

            conversations = page.conversations
            hasMoreConversations = page.hasMore
            nextConversationOffset = page.nextOffset

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SmsDebug", "Failed to load conversations", e)
            loadError = e.message ?: "Unable to load conversations."
        } finally {
            isLoading = false
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex =
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1

            val loadMoreThreshold =
                (conversations.size + 1 - 20).coerceAtLeast(0) // Ensure a minimum threshold

            conversations.isNotEmpty() && lastVisibleIndex >= loadMoreThreshold
        }
    }

    LaunchedEffect(
        shouldLoadMore,
        hasMoreConversations,
        isLoadingMore,
        isLoading,
        isSearchScreen
    ) {
        if (isSearchScreen) return@LaunchedEffect

        if (!hasSmsPermission || !hasContactPermission) {
            return@LaunchedEffect
        }

        if (
            !shouldLoadMore ||
            !hasMoreConversations ||
            isLoading ||
            isLoadingMore
        ) {
            return@LaunchedEffect
        }

        isLoadingMore = true
        loadMoreError = null

        try {
            val page = smsRepository.getConversationsPage(
                limit = pageSize,
                offset = nextConversationOffset
            )

            conversations = conversations + page.conversations

            hasMoreConversations = page.hasMore
            nextConversationOffset = page.nextOffset
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("My Screen", "Failed to load more conversations", e)
            loadMoreError = e.message ?: "An unexpected error occurred"
        } finally {
            isLoadingMore = false
        }
    }

    DisposableEffect(
        hasSmsPermission,
        hasContactPermission,
        smsRepository
    ) {
        if (hasSmsPermission && hasContactPermission) {
            val observer = SmsContentObserver {
                coroutineScope.launch {
                    try {
                        val page = smsRepository.getConversationsPage(
                            limit = pageSize,
                            offset = 0
                        )

                        conversations = page.conversations
                        hasMoreConversations = page.hasMore
                        nextConversationOffset = page.nextOffset
                        isLoadingMore = false
                        loadError = null
                        loadMoreError = null


                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("SmsDebug", "Failed to load conversations", e)
                        loadError = e.message ?: "Unable to load conversations."
                    }
                }
            }

            contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer
            )

            onDispose {
                contentResolver.unregisterContentObserver(observer)
            }
        } else {
            onDispose { }
        }
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

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (isSearchScreen) {
            SearchScreen(
                onBack = { isSearchScreen = false },
                contactRepository = contactRepository,
                smsRepository = smsRepository
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                MainNavbar(onEditClick = {})
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Loading messages...")
                            }
                        }
                    }

                    loadError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Failed to load messages.\n$loadError",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    conversations.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No messages yet!",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your SMS conversations will appear here when you send or receive text messages.",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                            item(key = "search_bar") {
                                ConversationSearchBar(
                                    onClick = {
                                        if (hasSmsPermission && hasContactPermission) {
                                            isSearchScreen = true
                                        } else if (!hasSmsPermission) {
                                            smsRequestKey++
                                        } else {
                                            contactRequestKey++
                                        }
                                    }
                                )
                            }
                            items(
                                items = conversations,
                                key = { conversation -> conversation.threadId }
                            ) { conversation ->
                                ConversationRow(
                                    conversation = conversation
                                )
                            }
                            if (isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            if (loadMoreError != null) {
                                item {
                                    Text(
                                        text = loadMoreError ?: "Unable to load more messages.",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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
        )
    }
}