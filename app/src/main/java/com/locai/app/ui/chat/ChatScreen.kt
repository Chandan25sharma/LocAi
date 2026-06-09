package com.locai.app.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.ui.LambdaViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun scrollToBottom() {
        val itemCount = uiState.messages.size + if (uiState.streamingReply != null) 1 else 0
        if (itemCount > 0) coroutineScope.launch { listState.animateScrollToItem(itemCount - 1) }
    }

    LaunchedEffect(uiState.messages.size, uiState.streamingReply) { scrollToBottom() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeBitmap(context, uri) }
            if (bitmap != null) viewModel.onImageAttached(bitmap)
        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
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
                            item {
                                SuggestedPrompts(
                                    prompts = uiState.category.suggestedPrompts,
                                    enabled = !uiState.isModelLoading && !uiState.isGenerating,
                                    onPromptClick = viewModel::sendSuggestion
                                )
                            }
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

            uiState.attachedImage?.let { bitmap ->
                AttachedImagePreview(
                    bitmap = bitmap,
                    onRemove = viewModel::clearAttachedImage,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.isVisionAvailable) {
                    IconButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !uiState.isModelLoading && !uiState.isGenerating
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach a photo")
                    }
                }
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { if (it.isFocused) scrollToBottom() },
                    placeholder = { Text("Ask something…") },
                    enabled = !uiState.isModelLoading,
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5
                )
                FilledIconButton(
                    onClick = viewModel::sendMessage,
                    enabled = !uiState.isModelLoading && !uiState.isGenerating && uiState.inputText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

/** Decodes a picked-photo [Uri] into a [Bitmap], using the modern decoder where available. */
private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            decoder.isMutableRequired = false
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()

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

/** Tappable starter questions so a brand-new chat doesn't start as just an empty box. */
@Composable
private fun SuggestedPrompts(prompts: List<String>, enabled: Boolean, onPromptClick: (String) -> Unit) {
    if (prompts.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            text = "Try asking",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            prompts.forEach { prompt ->
                Surface(
                    onClick = { if (enabled) onPromptClick(prompt) },
                    enabled = enabled,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/** Thumbnail + remove (×) shown above the input row while a picked photo is waiting to be sent. */
@Composable
private fun AttachedImagePreview(bitmap: Bitmap, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Attached photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove attached photo",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(3.dp)
                )
            }
        }
        Text(
            text = "Photo attached — ask your question and send",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/** Decodes a saved message photo off the main thread and remembers it for the lifetime of [path]. */
@Composable
private fun rememberLocalImage(path: String): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    return state.value
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == MessageRole.USER
    val containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) Alignment.End else Alignment.Start
    // Asymmetric "messaging app" tails: a squared-off corner on the side that points to the sender.
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                message.imagePath?.let { path ->
                    val image = rememberLocalImage(path)
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = "Attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(bottom = 8.dp)
                        )
                    }
                }
                if (message.content.isNotEmpty()) {
                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
