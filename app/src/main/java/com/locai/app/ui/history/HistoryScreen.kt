package com.locai.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.db.ConversationEntity
import com.locai.app.domain.Categories
import com.locai.app.ui.LambdaViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    container: LocAiContainer,
    onBack: () -> Unit,
    onOpenConversation: (conversationId: Long, categoryId: String) -> Unit
) {
    val viewModel: HistoryViewModel = viewModel(factory = LambdaViewModelFactory { HistoryViewModel(container) })
    val query by viewModel.query.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    var pendingDelete by remember { mutableStateOf<ConversationEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search your past conversations…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (conversations.isEmpty()) {
                Text(
                    text = if (query.isBlank()) {
                        "No conversations yet. Start one from the home screen — everything you " +
                            "discuss is saved here, only on this device."
                    } else {
                        "No conversations match \"$query\"."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = { onOpenConversation(conversation.id, conversation.categoryId) },
                            onDeleteClick = { pendingDelete = conversation }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this conversation?") },
            text = { Text("\"${conversation.title}\" and its messages will be permanently removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(conversation)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val category = Categories.byId(conversation.categoryId)
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = conversation.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${category.displayName} · ${formatTimestamp(conversation.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete conversation")
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT)
