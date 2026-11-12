package com.example.smsriskanalysis.layout

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smsriskanalysis.composables.components.ProfileImage
import com.example.smsriskanalysis.data.repository.SmsRepository
import com.example.smsriskanalysis.functions.permissions.createDefaultSmsRoleRequestIntent
import com.example.smsriskanalysis.functions.permissions.isDefaultSmsApp
import com.example.smsriskanalysis.models.ConversationRowData
import com.example.smsriskanalysis.observers.SmsContentObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    conversation: ConversationRowData,
    onBack: () -> Unit,
    smsRepository: SmsRepository
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<SmsRepository.SmsMessageData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var messageInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val displayName = conversation.contactName ?: conversation.address

    val loadMessages: suspend () -> Unit = {
        try {
            messages = smsRepository.getMessagesForThread(conversation.threadId)
        } catch (e: Exception) {
            errorMsg = e.message ?: "Failed to load messages."
        }
    }

    LaunchedEffect(conversation.threadId) {
        isLoading = true
        errorMsg = null
        loadMessages()
        isLoading = false
    }

    // Real-time observer for incoming SMS messages in this chat screen
    DisposableEffect(conversation.threadId, smsRepository) {
        val observer = SmsContentObserver {
            coroutineScope.launch {
                loadMessages()
            }
        }
        context.contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI,
            true,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    // Scroll to latest message when messages load or send
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val sendSms: (String) -> Unit = { body ->
        isSending = true
        coroutineScope.launch {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                try {
                    smsManager?.sendTextMessage(
                        conversation.address,
                        null,
                        body,
                        null,
                        null
                    )
                } catch (sendEx: Exception) {
                    Log.w("ChatScreen", "SmsManager send failed: ${sendEx.message}")
                }

                // Insert into content://sms without forcing custom thread_id so system assigns correct thread_id automatically
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, conversation.address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
                val insertedUri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                Log.d("ChatScreen", "Inserted sent SMS uri: $insertedUri")

                messageInput = ""
                delay(300L)
                loadMessages()
            } catch (e: Exception) {
                Log.e("ChatScreen", "Failed to send/save message", e)
                errorMsg = "Failed to send message: ${e.message}"
            } finally {
                isSending = false
            }
        }
    }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || isDefaultSmsApp(context)) {
            if (messageInput.isNotBlank()) {
                sendSms(messageInput.trim())
            }
        } else {
            errorMsg = "App must be set as the Default SMS App to send messages."
        }
    }

    val sendPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && messageInput.isNotBlank()) {
            if (!isDefaultSmsApp(context)) {
                val intent = createDefaultSmsRoleRequestIntent(context)
                if (intent != null) {
                    roleRequestLauncher.launch(intent)
                } else {
                    errorMsg = "App must be set as the Default SMS App to send messages."
                }
            } else {
                sendSms(messageInput.trim())
            }
        } else if (!isGranted) {
            errorMsg = "SEND_SMS permission is required to send messages."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Navigation Bar
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                ProfileImage(
                    profileLink = conversation.contactPhotoUri,
                    contentDescription = displayName
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!conversation.contactName.isNullOrBlank() && conversation.address.isNotBlank()) {
                        Text(
                            text = conversation.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Chat Body / Messages History
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMsg != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMsg.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                messages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages in this conversation.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatMessageBubble(message = message)
                        }
                    }
                }
            }
        }

        // Reply / Send Message Input Bar
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 4
                )

                IconButton(
                    enabled = messageInput.isNotBlank() && !isSending,
                    onClick = {
                        val body = messageInput.trim()
                        if (body.isNotEmpty()) {
                            val hasSendPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.SEND_SMS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasSendPermission) {
                                sendPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            } else if (!isDefaultSmsApp(context)) {
                                val intent = createDefaultSmsRoleRequestIntent(context)
                                if (intent != null) {
                                    roleRequestLauncher.launch(intent)
                                } else {
                                    errorMsg = "App must be set as the Default SMS App to send messages."
                                }
                            } else {
                                sendSms(body)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: SmsRepository.SmsMessageData
) {
    val isOutgoing = message.type == Telephony.Sms.MESSAGE_TYPE_SENT ||
            message.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX

    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val timeColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.body,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatMessageTime(message.date),
                    color = timeColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}