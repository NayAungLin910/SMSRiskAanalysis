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
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import com.example.smsriskanalysis.data.repository.ContactRepository
import com.example.smsriskanalysis.data.repository.SmsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

    var messages by remember {
        mutableStateOf<List<SmsRepository.SmsMessageData>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(false) }

    var searchError by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onBack()
    }

    LaunchedEffect(searchQuery, contactRepository, smsRepository) {
        val query = searchQuery.trim()

        if (query.isBlank()) {
            contacts = emptyList()
            messages = emptyList()
            searchError = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        searchError = null

        try {
            delay(250.milliseconds)

            val results = coroutineScope {
                val contactSearch = async {
                    contactRepository.searchContacts(query)
                }

                val messageSearch = async {
                    smsRepository.searchMessages(query)
                }

                contactSearch.await() to messageSearch.await()
            }
            contacts = results.first
            messages = results.second
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SearchScreen", "Search operation failed", e)
            searchError = "Unable to search. Please try again"
            contacts = emptyList()
            messages = emptyList()
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
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                   contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = contact.name, style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
            items(
                items = messages, key = { it.id }) { message ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp, vertical = 10.dp
                        )
                ) {
                    Text(
                        text = message.address ?: "Unkown sender",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
            if (isLoading) {
                item {
                    Text(
                        text = "Searching...",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchError != null) {
                item {
                    Text(
                        text = searchError!!,
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (searchQuery.isNotBlank() && contacts.isEmpty() && messages.isEmpty()) {
                item {
                    Text(
                        text = "No matching contacts or messages",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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