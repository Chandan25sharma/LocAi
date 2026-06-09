package com.locai.app.ui.home

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.db.ConversationEntity
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.ui.LambdaViewModelFactory
import com.locai.app.ui.components.LocAiLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeChatScreen(
    container: LocAiContainer,
    onOpenSettings: () -> Unit,
    onCreateImage: () -> Unit
) {
    val viewModel: HomeChatViewModel = viewModel(
        factory = LambdaViewModelFactory { HomeChatViewModel(container) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showAttachSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.messages.size, uiState.streamingReply) {
        val count = uiState.messages.size + if (uiState.streamingReply != null) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeBitmap(context, uri) }
            if (bitmap != null) viewModel.onImageAttached(bitmap)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                ChatHistoryDrawer(
                    conversations = uiState.allConversations,
                    activeId = uiState.activeConversationId,
                    onNewChat = {
                        scope.launch { drawerState.close() }
                        viewModel.newChat()
                    },
                    onSelect = { conv ->
                        scope.launch { drawerState.close() }
                        viewModel.selectConversation(conv)
                    },
                    onDelete = { conv -> viewModel.deleteConversation(conv) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LocAiLogo(size = 28.dp)
                            Text("LocAi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open chat history")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                            androidx.compose.material3.CircularProgressIndicator()
                            Text(
                                text = "Loading model…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    } else if (uiState.messages.isEmpty() && uiState.streamingReply == null) {
                        EmptyHomeState(
                            onSuggestionClick = viewModel::sendSuggestion,
                            onCreateImage = onCreateImage
                        )
                    } else {
                        val conversationId = uiState.activeConversationId ?: 0L
                        val categoryId = uiState.activeCategory.id
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.messages, key = { it.id }) { msg ->
                                HomeChatBubble(message = msg)
                            }
                            uiState.streamingReply?.let { partial ->
                                item(key = "streaming") {
                                    HomeChatBubble(
                                        message = viewModel.streamingMessageEntity(conversationId, categoryId, partial)
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                uiState.attachedImage?.let { bitmap ->
                    AttachedPhotoChip(
                        bitmap = bitmap,
                        onRemove = viewModel::clearAttachedImage,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                HomeInputBar(
                    text = uiState.inputText,
                    onTextChange = viewModel::onInputChanged,
                    onSend = viewModel::sendMessage,
                    onPlusClick = { showAttachSheet = true },
                    isEnabled = !uiState.isModelLoading,
                    isGenerating = uiState.isGenerating
                )
            }
        }

        if (showAttachSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachSheet = false },
                sheetState = sheetState
            ) {
                AttachOptionsSheet(
                    isVisionAvailable = uiState.isVisionAvailable,
                    onAttachPhoto = {
                        showAttachSheet = false
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onCreateImage = {
                        showAttachSheet = false
                        onCreateImage()
                    }
                )
            }
        }
    }
}

// ─── Drawer ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatHistoryDrawer(
    conversations: List<ConversationEntity>,
    activeId: Long?,
    onNewChat: () -> Unit,
    onSelect: (ConversationEntity) -> Unit,
    onDelete: (ConversationEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocAiLogo(size = 28.dp)
            Text(
                "LocAi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
        }

        NavigationDrawerItem(
            label = { Text("New chat", fontWeight = FontWeight.Medium) },
            selected = false,
            onClick = onNewChat,
            icon = { Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

        val grouped = groupConversationsByDate(conversations)
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            grouped.forEach { (label, items) ->
                item {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(items, key = { it.id }) { conv ->
                    ConversationDrawerItem(
                        conversation = conv,
                        isActive = conv.id == activeId,
                        onSelect = { onSelect(conv) },
                        onDelete = { onDelete(conv) }
                    )
                }
            }
            if (conversations.isEmpty()) {
                item {
                    Text(
                        "No chats yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationDrawerItem(
    conversation: ConversationEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = conversation.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        selected = isActive,
        onClick = onSelect,
        badge = {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

private fun groupConversationsByDate(conversations: List<ConversationEntity>): Map<String, List<ConversationEntity>> {
    val now = Calendar.getInstance()
    val today = now.clone() as Calendar; today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
    val yesterday = today.clone() as Calendar; yesterday.add(Calendar.DAY_OF_YEAR, -1)
    val sevenDaysAgo = today.clone() as Calendar; sevenDaysAgo.add(Calendar.DAY_OF_YEAR, -7)

    val sorted = conversations.sortedByDescending { it.updatedAt }
    val todayList = sorted.filter { it.updatedAt >= today.timeInMillis }
    val yesterdayList = sorted.filter { it.updatedAt >= yesterday.timeInMillis && it.updatedAt < today.timeInMillis }
    val weekList = sorted.filter { it.updatedAt >= sevenDaysAgo.timeInMillis && it.updatedAt < yesterday.timeInMillis }
    val olderList = sorted.filter { it.updatedAt < sevenDaysAgo.timeInMillis }

    return buildMap {
        if (todayList.isNotEmpty()) put("Today", todayList)
        if (yesterdayList.isNotEmpty()) put("Yesterday", yesterdayList)
        if (weekList.isNotEmpty()) put("Last 7 days", weekList)
        if (olderList.isNotEmpty()) put("Older", olderList)
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

private data class Suggestion(val icon: ImageVector, val label: String, val prompt: String)

private val HOME_SUGGESTIONS = listOf(
    Suggestion(Icons.Default.EditNote, "Write or edit", "Help me write "),
    Suggestion(Icons.Default.Search, "Look something up", "Explain "),
    Suggestion(Icons.AutoMirrored.Filled.Send, "Summarise for me", "Summarise this: ")
)

@Composable
private fun EmptyHomeState(onSuggestionClick: (String) -> Unit, onCreateImage: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LocAiLogo(size = 64.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "What can I help with?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            // "Create an image" chip navigates to the generator screen
            SuggestionChip(
                icon = Icons.Default.AutoAwesome,
                label = "Create an image",
                onClick = onCreateImage
            )
            HOME_SUGGESTIONS.forEach { s ->
                SuggestionChip(
                    icon = s.icon,
                    label = s.label,
                    onClick = { onSuggestionClick(s.prompt) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Input bar ───────────────────────────────────────────────────────────────

@Composable
private fun HomeInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onPlusClick: () -> Unit,
    isEnabled: Boolean,
    isGenerating: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onPlusClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            enabled = isEnabled,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Attach or create",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask anything…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            enabled = isEnabled,
            shape = RoundedCornerShape(24.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        FilledIconButton(
            onClick = onSend,
            enabled = isEnabled && !isGenerating && text.isNotBlank(),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
        }
    }
}

// ─── Attach bottom sheet ─────────────────────────────────────────────────────

@Composable
private fun AttachOptionsSheet(
    isVisionAvailable: Boolean,
    onAttachPhoto: () -> Unit,
    onCreateImage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Add to message",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        SheetOption(
            icon = Icons.Default.AddPhotoAlternate,
            label = "Attach a photo",
            description = if (isVisionAvailable) "Ask questions about an image"
                          else "Download image understanding model in Settings first",
            enabled = isVisionAvailable,
            onClick = onAttachPhoto
        )
        SheetOption(
            icon = Icons.Default.AutoAwesome,
            label = "Create an image",
            description = "Generate an image from a text prompt",
            onClick = onCreateImage
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    label: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

// ─── Chat bubbles ────────────────────────────────────────────────────────────

@Composable
private fun HomeChatBubble(message: MessageEntity) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                message.imagePath?.let { path ->
                    val imageBitmap = rememberLocalImage(path)
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                                .clip(RoundedCornerShape(12.dp)).padding(bottom = 8.dp)
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

@Composable
private fun AttachedPhotoChip(bitmap: Bitmap, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Attached photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
            )
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(20.dp).align(Alignment.TopEnd).padding(2.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(3.dp))
            }
        }
        Text(
            text = "Photo attached",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun rememberLocalImage(path: String): ImageBitmap? {
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    return state.value
}

private fun decodeBitmap(context: android.content.Context, uri: Uri): Bitmap? = runCatching {
    val raw: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            // MUST use software allocator — hardware bitmaps cannot be read by BitmapImageBuilder
            // (MediaPipe needs CPU-accessible pixel data). Without this the app crashes on send.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    // Resize to at most MAX_IMAGE_PX on the longest side — full-resolution phone photos can be
    // 40–50 MB in RAM while the vision model is also loaded, causing OOM crashes.
    resizeBitmap(raw, MAX_IMAGE_PX)
}.getOrNull()

private fun resizeBitmap(src: Bitmap, maxPx: Int): Bitmap {
    val w = src.width; val h = src.height
    if (w <= maxPx && h <= maxPx) return src
    val scale = maxPx.toFloat() / maxOf(w, h)
    return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
}

private const val MAX_IMAGE_PX = 768

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

@Suppress("unused")
private fun formatConversationTime(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return if (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
               cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
        timeFormat.format(Date(timestamp))
    } else {
        dateFormat.format(Date(timestamp))
    }
}
