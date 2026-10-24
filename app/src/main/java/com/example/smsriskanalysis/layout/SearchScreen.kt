package com.example.smsriskanalysis.layout

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import com.example.smsriskanalysis.composables.ConversationRow
import com.example.smsriskanalysis.composables.components.ProfileImage
import com.example.smsriskanalysis.data.repository.ContactRepository
import com.example.smsriskanalysis.data.repository.SmsRepository
import com.example.smsriskanalysis.models.ConversationRowData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SearchScreen(
    onBack: () -> Unit, contactRepository: ContactRepository, smsRepository: SmsRepository
) {
    var searchQuery by remember {
        mutableStateOf("")
    }

    var contacts by remember {
        mutableStateOf<List<ContactRepository.ContactSearchData>>(emptyList())
    }

    var conversations by remember {
        mutableStateOf<List<ConversationRowData>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var hasMore by remember { mutableStateOf(false) }
    var nextOffset by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    LaunchedEffect(searchQuery, contactRepository, smsRepository) {
        val query = searchQuery.trim()

        contacts = emptyList()
        conversations = emptyList()
        hasMore = false
        nextOffset = 0
        searchError = null

        if (query.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        try {
            delay(250.milliseconds)

            val results = coroutineScope {
                val contactSearch = async {
                    contactRepository.searchContacts(query)
                }

                val conversationSearch = async {
                    smsRepository.searchConversations(query, limit = 50, offset = 0)
                }

                contactSearch.await() to conversationSearch.await()
            }
            contacts = results.first
            conversations = results.second.conversations
            hasMore = results.second.hasMore
            nextOffset = results.second.nextOffset

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SearchScreen", "Search operation failed", e)
            searchError = "Unable to search. Please try again"
            contacts = emptyList()
            conversations = emptyList()
            hasMore = false
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                )
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp, vertical = 14.dp
                    ), verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search, contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BoxWithSearchField(
                        value = searchQuery, onValueChange = { searchQuery = it })
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search"
                            )
                        }
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            if (contacts.isNotEmpty()) {
                item {
                    Text(
                        text = "CONTACTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 20.dp, vertical = 8.dp
                        )
                    )
                }
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp, vertical = 8.dp
                    ), horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = contacts, key = { "${it.id}:${it.phoneNumber}" }) { contact ->
                        Column(
                            modifier = Modifier.width(72.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProfileImage(
                                profileLink = contact.photoUri,
                                contentDescription = contact.name
                            )
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1
                            )
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            if (conversations.isNotEmpty()) {
                item {
                    Text(
                        text = "MESSAGES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 8.dp,
                        )
                    )
                }
            }
            items(
                items = conversations, key = { it.threadId }) { conversation ->
                ConversationRow(conversation, modifier = Modifier.fillMaxWidth())
            }
            if (isLoading) {
                item {
                    Text(
                        text = "Searching...",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (searchError != null) {
                item {
                    Text(
                        text = searchError!!,
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (searchQuery.isNotBlank() && !isLoading && !isLoadingMore && !hasMore && conversations.isEmpty() && contacts.isEmpty() && searchError == null) {
                item {
                    Text(
                        text = "No matching contacts or messages",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hasMore && !isLoading && searchError == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            enabled = !isLoadingMore,
                            onClick = {
                                val query = searchQuery.trim()
                                if (query.isNotBlank() && !isLoadingMore) {
                                    coroutineScope.launch {
                                        isLoadingMore = true
                                        try {
                                            val result = smsRepository.searchConversations(
                                                query = query,
                                                limit = 50,
                                                offset = nextOffset
                                            )
                                            if (searchQuery.trim() == query) {
                                                val existingIds =
                                                    conversations.map { it.threadId }.toSet()

                                                conversations =
                                                    conversations + result.conversations.filter {
                                                        it.threadId !in existingIds
                                                    }

                                                hasMore = result.hasMore
                                                nextOffset = result.nextOffset
                                            }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            Log.e(
                                                "SearchScreen",
                                                "Unable to load more conversations",
                                                e
                                            )
                                            searchError = "Unable to load more results. "
                                        } finally {
                                            isLoadingMore = false
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(text = "Load More")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BoxWithSearchField(
    value: String, onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = "Search contacts and messages",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                innerTextField()
            }
        })
}