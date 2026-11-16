package com.example.smsriskanalysis

import com.example.smsriskanalysis.functions.permissions.ContactPermissionHandler
import com.example.smsriskanalysis.functions.permissions.SmsPermissionHandler
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.core.content.ContextCompat
import com.example.smsriskanalysis.composables.AppDialog
import com.example.smsriskanalysis.composables.AppDialogType
import com.example.smsriskanalysis.composables.ConversationSearchBar
import com.example.smsriskanalysis.composables.MainNavbar
import com.example.smsriskanalysis.data.repository.ContactRepository
import com.example.smsriskanalysis.data.repository.SmsRepository
import com.example.smsriskanalysis.functions.permissions.DefaultSmsRoleHandler
import com.example.smsriskanalysis.functions.permissions.isDefaultSmsApp
import com.example.smsriskanalysis.layout.ChatScreen
import com.example.smsriskanalysis.layout.SearchScreen
import com.example.smsriskanalysis.observers.SmsContentObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.smsriskanalysis.composables.NewMessageDialog
import com.example.smsriskanalysis.functions.permissions.NotificationPermissionHandler
import com.example.smsriskanalysis.functions.sms.SmsSender
import java.util.jar.Manifest

class MainActivity : ComponentActivity() {
    private val notificationThreadId = mutableStateOf<Long?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationThreadId.value = intent.getLongExtra("threatId", -1L).takeIf {
            it != -1L
        }
        enableEdgeToEdge()
        setContent {
            SMSRiskAnalysisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyScreen(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        notificationThreadId = notificationThreadId.value,
                        onNotificationHandled = {
                            notificationThreadId.value = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationThreadId.value = intent.getLongExtra("threatId", -1L).takeIf {
            it != -1L
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    name: String,
    modifier: Modifier = Modifier,
    notificationThreadId: Long? = null,
    onNotificationHandled: () -> Unit = {}
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

    var isSelectionMode by remember {
        mutableStateOf(false)
    }

    var selectedThreadIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }

    var deleteError by remember {
        mutableStateOf<String?>(null)
    }

    var hasCompletedInitialLoad by remember {
        mutableStateOf(false)
    }

    var hasSmsPermission by remember { mutableStateOf(false) }
    var hasContactPermission by remember { mutableStateOf(false) }
    var smsRequestKey by remember { mutableStateOf(0) }
    var contactRequestKey by remember { mutableStateOf(0) }
    var permissionWasDenied by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog =
        permissionWasDenied && (!hasSmsPermission || !hasContactPermission)

    var selectedConversationForChat by remember {
        mutableStateOf<ConversationRowData?>(null)
    }

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

    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    var roleRequestForDelete by remember {
        mutableStateOf(false)
    }

    var isDefaultSms by remember {
        mutableStateOf(isDefaultSmsApp(context))
    }

    var showMessageDialog by remember {
        mutableStateOf(false)
    }

    var newMessageError by remember {
        mutableStateOf<String?>(null)
    }

    var pendingNewMessage by remember {
        mutableStateOf<Pair<String, String>?>(null)
    }

    var hasNotificationPermission by remember {
        mutableStateOf(false)
    }

    var notificationRequestKey by remember {
        mutableStateOf(0)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    fun performNewMessageSend(
        recipient: String,
        message: String
    ) {
        when (
            val result = SmsSender.send(
                context,
                recipient,
                message,
            )
        ) {
            SmsSender.SendResult.Success -> {
                newMessageError = null
                showMessageDialog = false
                Log.d(
                    "New Message",
                    "Sms submission accepeted for $recipient"
                )
            }

            is SmsSender.SendResult.Failure -> {
                newMessageError = result.reason
            }
        }
    }

    val sendSmsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingNewMessage
        pendingNewMessage = null

        if (granted && pending != null) {
            performNewMessageSend(
                pending.first,
                pending.second
            )
        } else if (!granted) {
            newMessageError = "SMS permission is required to send messages."
        }
    }

    fun sendNewMessage(
        recipient: String,
        message: String
    ) {
        newMessageError = null

        if (
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            performNewMessageSend(recipient, message)
        } else {
            pendingNewMessage = recipient to message

            sendSmsPermissionLauncher.launch(
                android.Manifest.permission.SEND_SMS
            )
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultSms = isDefaultSmsApp(context)

                Log.d(
                    "SmsRoleDebug",
                    "App resumed. Default SMS role: $isDefaultSms"
                )
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isDefaultSms) {
        SmsPermissionHandler(
            requestKey = smsRequestKey,
            onPermissionGranted = {
                hasSmsPermission = true
            },
            onPermissionDenied = {
                hasSmsPermission = false
                permissionWasDenied = true
            }
        )
    }

    if (isDefaultSms && hasSmsPermission) {
        ContactPermissionHandler(
            requestKey = contactRequestKey,
            onPermissionGranted = {
                hasContactPermission = true
            },
            onPermissionDenied = {
                hasContactPermission = false
                permissionWasDenied = true
            }
        )
    }

    if (isDefaultSms && hasSmsPermission) {
        NotificationPermissionHandler(
            requestKey = notificationRequestKey,
            onPermissionGranted = {
                hasNotificationPermission = true
                Log.d("NotificationPermission", "Permission granted")
            },
            onPermissionDenied = {
                hasNotificationPermission = false
                Log.d("NotificationPermission", "Permission denied")
            }
        )
    }
    fun deleteSelectedConversations() {
        val idsToDelete = selectedThreadIds
        if (idsToDelete.isEmpty()) return

        coroutineScope.launch {
            deleteError = null

            try {
                smsRepository.deleteConversations(idsToDelete)

                conversations = conversations.filterNot {
                    it.threadId in idsToDelete
                }

                selectedThreadIds = emptySet()
                isSelectionMode = false

                isLoading = true

                try {
                    val page = smsRepository.getConversationsPage(
                        limit = pageSize,
                        offset = 0
                    )
                    conversations = page.conversations
                    hasMoreConversations = page.hasMore
                    nextConversationOffset = page.nextOffset
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        "MyScreen",
                        "Messages were deleted but the list could not refresh."
                    )
                } finally {
                    isLoading = false
                }
            } catch (e: SecurityException) {
                deleteError =
                    "Android denied deletion. This app may need to be set as the default SMS app. "
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MyScreen", "Failed to delete conversations", e)
                deleteError = e.message ?: "Unable to delete selected conversations."
            }
        }
    }

    val requestDefaultSmsRole = DefaultSmsRoleHandler(
        onRoleGranted = {
            isDefaultSms = true

            if (roleRequestForDelete) {
                roleRequestForDelete = false
                deleteSelectedConversations()
            }
        },
        onRoleDenied = {
            roleRequestForDelete = false
        },
        onRequestableDenied = {
            roleRequestForDelete = false
        }
    )

    LaunchedEffect(conversations.isNotEmpty(), isLoading, isSearchScreen) {
        if (conversations.isNotEmpty() && !isLoading && !hasAppliedInitialScroll && !isSearchScreen) {
            listState.animateScrollToItem(1)
            hasAppliedInitialScroll = true
        }
    }

    LaunchedEffect(
        notificationThreadId,
        conversations,
        hasCompletedInitialLoad
    ) {
        val targetThreatId = notificationThreadId ?: return@LaunchedEffect
        if (!hasSmsPermission || !hasCompletedInitialLoad) {
            return@LaunchedEffect
        }
        val targetConversation = conversations.find {
            it.threadId == targetThreatId
        }
        if (targetConversation != null) {
            isSearchScreen = false
            isSelectionMode = false
            selectedThreadIds = emptySet()

            selectedConversationForChat = targetConversation
            Log.d(
                "NotificationNavigation",
                "Opening conversation: $targetThreatId"
            )
        } else {
            Log.w(
                "NotificationNavigation",
                "Notification target conversation not found: $targetThreatId"
            )
        }
        onNotificationHandled()
    }

    LaunchedEffect(hasSmsPermission) {
        if (!hasSmsPermission) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        loadError = null


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
            hasCompletedInitialLoad = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SmsDebug", "Failed to load conversations", e)
            loadError = e.message ?: "Unable to load conversations."
            hasCompletedInitialLoad = true
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

        if (!hasSmsPermission) {
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
        smsRepository,
        hasCompletedInitialLoad
    ) {
        if (hasSmsPermission && hasCompletedInitialLoad) {
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
        AppDialog(
            title = "Permission Required",
            message = """
                SMS and Contacts permissions are required for this app to work properly.
    
                SMS permission: Allows the app to read your SMS messages so it can display your conversations and help identify potential scam messages.
    
                Contacts permission: Allows the app to access your contacts so it can display contact names alongside their phone numbers.
    
                You can manage these permissions in your device settings.
            """.trimIndent(),
            type = AppDialogType.PERMISSION,
            dismissText = "Close App",
            dismissRequest = false,
            onDismiss = {
                (context as Activity).finish()
            },
            onConfirm = {
                try {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("DefaultSmsRole", "Failed to open app settings", e)
                } finally {
                }
            }
        )
    }

    if (!isDefaultSms) {
        AppDialog(
            title = "Default SMS App Required",
            message = "SMS Risk Analysis must be set as your default SMS app to operate. Please set this app as your default SMS app to use it.",
            type = AppDialogType.MESSAGE,
            confirmText = "Set as Default SMS App",
            dismissRequest = false,
            onDismiss = {},
            onConfirm = {
                requestDefaultSmsRole()
            }
        )
    }

    if (showDeleteConfirmation) {
        AppDialog(
            title = "Delete conversations?",
            message = "Are you sure you want to permanently delete the selected conversations?",
            type = AppDialogType.CONFIRMATION,
            confirmText = "Delete",
            onDismiss = {
                showDeleteConfirmation = false
            },
            onConfirm = {
                showDeleteConfirmation = false
                deleteSelectedConversations()
            }
        )
    }

    if (showMessageDialog) {
        NewMessageDialog(
            onDismiss = {
                showMessageDialog = false
            },
            onSend = { recipient, message ->
                sendNewMessage(
                    recipient = recipient,
                    message = message
                )
            },
            errorMessage = newMessageError
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (selectedConversationForChat != null) {
            ChatScreen(
                conversation = selectedConversationForChat!!,
                onBack = { selectedConversationForChat = null },
                smsRepository = smsRepository
            )
        } else if (isSearchScreen) {
            SearchScreen(
                onBack = { isSearchScreen = false },
                contactRepository = contactRepository,
                smsRepository = smsRepository,
                onConversationClick = { conversation ->
                    isSearchScreen = false
                    selectedConversationForChat = conversation
                },
                onContactClick = { contact ->
                    isSearchScreen = false
                    coroutineScope.launch {
                        val threadId = smsRepository.getOrCreateThreadId(contact.phoneNumber)
                        selectedConversationForChat = ConversationRowData(
                            threadId = threadId,
                            address = contact.phoneNumber,
                            contactName = contact.name,
                            contactPhotoUri = contact.photoUri,
                            latestMessage = "",
                            latestMessageTime = System.currentTimeMillis(),
                            unreadCount = 0
                        )
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!isLoading && !isSearchScreen && hasCompletedInitialLoad && isDefaultSms && hasSmsPermission && hasContactPermission) {
                    MainNavbar(
                        isSelectionMode = isSelectionMode,
                        onSelectCLick = {
                            selectedThreadIds = emptySet()
                            deleteError = null
                            isSelectionMode = true
                        },
                        onDoneClick = {
                            selectedThreadIds = emptySet()
                            deleteError = null
                            isSelectionMode = false
                        },
                        onMessageClick = {
                            showMessageDialog = true
                        },
                        hasConversations = conversations.isNotEmpty(),
                    )
                }
                when {
                    !hasSmsPermission || !hasContactPermission -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Waiting for permissions...")
                        }
                    }

                    isLoading || !hasCompletedInitialLoad -> {
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
                                            permissionWasDenied = false
                                            smsRequestKey++
                                        } else {
                                            permissionWasDenied = false
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
                                    conversation = conversation,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = conversation.threadId in selectedThreadIds,
                                    onSelectClick = {
                                        val threadId = conversation.threadId
                                        selectedThreadIds = if (threadId in selectedThreadIds) {
                                            selectedThreadIds - threadId
                                        } else {
                                            selectedThreadIds + threadId
                                        }
                                    },
                                    onClick = {
                                        selectedConversationForChat = conversation
                                    }
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
                if (isSelectionMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (deleteError != null) {
                            Text(
                                text = deleteError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                enabled = selectedThreadIds.isNotEmpty(),
                                onClick = {
                                    showDeleteConfirmation = true
                                }
                            ) {
                                Text(
                                    text = "Delete (${selectedThreadIds.size})",
                                    color = MaterialTheme.colorScheme.error
                                )
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