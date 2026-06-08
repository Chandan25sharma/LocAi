package com.locai.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.ui.LambdaViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    container: LocAiContainer,
    conversationId: Long,
    categoryId: String,
    onBack: () -> Unit
) {
    val viewModel: ChatViewModel = viewModel(
        factory = LambdaViewModelFactory { ChatViewModel(container, conversationId, categoryId) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.streamingReply) {
        val itemCount = uiState.messages.size + if (uiState.streamingReply != null) 1 else 0
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.category.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isModelLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading the on-device model…",
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.messages.isEmpty() && uiState.streamingReply == null) {
                            item { EmptyChatHint() }
                        }
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(message)
                        }
                        uiState.streamingReply?.let { partial ->
                            item(key = "streaming") {
                                MessageBubble(
                                    MessageEntity(
                                        id = -1,
                                        conversationId = conversationId,
                                        categoryId = categoryId,
                                        role = MessageRole.ASSISTANT,
                                        content = partial.ifEmpty { "…" },
                                        timestamp = 0L
                                    )
                                )
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask something…") },
                    enabled = !uiState.isModelLoading,
                    maxLines = 5
                )
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = !uiState.isModelLoading && !uiState.isGenerating && uiState.inputText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun EmptyChatHint() {
    Text(
        text = "Ask anything in this topic. Your conversation stays on this device, and LocAi " +
            "will recall relevant parts of your past chats here to answer more consistently " +
            "over time.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == MessageRole.USER
    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
