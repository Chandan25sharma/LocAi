package com.locai.app.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.db.ConversationEntity
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.domain.Categories
import com.locai.app.domain.Category
import com.locai.app.ui.LambdaViewModelFactory
import com.locai.app.ui.components.LocAiLogo
import com.locai.app.ui.components.MarkdownText
import com.locai.app.ui.components.rememberSpeaker
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
    var showMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speaker = rememberSpeaker()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = viewModel.transcript()
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }.isSuccess
            }
            Toast.makeText(context, if (ok) "Chat saved" else "Couldn't save chat", Toast.LENGTH_SHORT).show()
        }
    }

    // Uses the phone's speech recognizer; EXTRA_PREFER_OFFLINE keeps it on-device where supported.
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            viewModel.onInputChanged(listOf(uiState.inputText.trim(), spoken).filter { it.isNotEmpty() }.joinToString(" "))
        }
    }
    val startVoiceInput = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask LocAi")
        try {
            voiceLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No speech recognizer on this device", Toast.LENGTH_SHORT).show()
        }
    }

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
                    searchQuery = uiState.searchQuery,
                    onSearchChange = viewModel::onSearchChanged,
                    onRename = viewModel::renameConversation,
                    onTogglePin = viewModel::togglePin,
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
                            Column {
                                Text("LocAi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                val category = uiState.activeCategory
                                if (category != Categories.GENERAL) {
                                    Text(category.displayName, style = MaterialTheme.typography.labelSmall, color = category.accent)
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open chat history")
                        }
                    },
                    actions = {
                        val inChat = uiState.activeConversationId != null
                        if (inChat) {
                            IconButton(onClick = viewModel::newChat) {
                                Icon(Icons.Default.Create, contentDescription = "New chat")
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                if (inChat) {
                                    DropdownMenuItem(
                                        text = { Text("Share chat") },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            shareText(context, viewModel.transcript())
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as file") },
                                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            exportLauncher.launch("locai-chat.md")
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onOpenSettings()
                                    }
                                )
                            }
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
                            category = uiState.activeCategory,
                            modelName = uiState.modelName,
                            onCategorySelect = viewModel::selectCategory,
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
                            val lastId = uiState.messages.lastOrNull()?.id
                            items(uiState.messages, key = { it.id }) { msg ->
                                HomeChatBubble(
                                    message = msg,
                                    isSpeaking = speaker.speakingKey == msg.id,
                                    onSpeak = {
                                        if (!speaker.toggle(msg.id, msg.content)) {
                                            Toast.makeText(context, "Text-to-speech isn't available on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onRegenerate = viewModel::regenerate.takeIf {
                                        msg.id == lastId && msg.role == MessageRole.ASSISTANT && !uiState.isGenerating
                                    }
                                )
                            }
                            uiState.streamingReply?.let { partial ->
                                item(key = "streaming") {
                                    HomeChatBubble(
                                        message = viewModel.streamingMessageEntity(conversationId, categoryId, partial),
                                        isStreaming = true
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.errorMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp)
                            )
                            // Retry only makes sense once a message actually went out in this chat.
                            if (uiState.activeConversationId != null && !uiState.isModelLoading) {
                                TextButton(onClick = viewModel::regenerate) { Text("Retry") }
                            }
                            IconButton(onClick = viewModel::dismissError) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
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
                    onStop = viewModel::stopGeneration,
                    onPlusClick = { showAttachSheet = true },
                    onMicClick = startVoiceInput,
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
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelect: (ConversationEntity) -> Unit,
    onRename: (ConversationEntity, String) -> Unit,
    onTogglePin: (ConversationEntity) -> Unit,
    onDelete: (ConversationEntity) -> Unit
) {
    var renaming by remember { mutableStateOf<ConversationEntity?>(null) }
    var deleting by remember { mutableStateOf<ConversationEntity?>(null) }

    renaming?.let { conv ->
        var title by remember(conv.id) { mutableStateOf(conv.title) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename chat") },
            text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { onRename(conv, title); renaming = null }, enabled = title.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }
    deleting?.let { conv ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete chat?") },
            text = { Text("\"${conv.title}\" will be permanently deleted from this device.") },
            confirmButton = {
                TextButton(onClick = { onDelete(conv); deleting = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }

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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search chats") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear search") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            label = { Text("New chat", fontWeight = FontWeight.Medium) },
            selected = false,
            onClick = onNewChat,
            icon = { Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(20.dp)) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

        val grouped = if (searchQuery.isBlank()) groupConversationsByDate(conversations)
                      else mapOf("Results" to conversations)
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
                        onRename = { renaming = conv },
                        onTogglePin = { onTogglePin(conv) },
                        onDelete = { deleting = conv }
                    )
                }
            }
            if (conversations.isEmpty()) {
                item {
                    Text(
                        if (searchQuery.isBlank()) "No chats yet" else "No matches",
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
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val category = Categories.byId(conversation.categoryId)
    var menuOpen by remember { mutableStateOf(false) }
    NavigationDrawerItem(
        icon = {
            Icon(
                if (conversation.isPinned) Icons.Default.PushPin else category.icon,
                contentDescription = category.displayName,
                tint = category.accent,
                modifier = Modifier.size(18.dp)
            )
        },
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
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Chat options", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (conversation.isPinned) "Unpin" else "Pin") },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        onClick = { menuOpen = false; onTogglePin() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
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

    val pinnedList = conversations.filter { it.isPinned }.sortedByDescending { it.updatedAt }
    val sorted = conversations.filterNot { it.isPinned }.sortedByDescending { it.updatedAt }
    val todayList = sorted.filter { it.updatedAt >= today.timeInMillis }
    val yesterdayList = sorted.filter { it.updatedAt >= yesterday.timeInMillis && it.updatedAt < today.timeInMillis }
    val weekList = sorted.filter { it.updatedAt >= sevenDaysAgo.timeInMillis && it.updatedAt < yesterday.timeInMillis }
    val olderList = sorted.filter { it.updatedAt < sevenDaysAgo.timeInMillis }

    return buildMap {
        if (pinnedList.isNotEmpty()) put("Pinned", pinnedList)
        if (todayList.isNotEmpty()) put("Today", todayList)
        if (yesterdayList.isNotEmpty()) put("Yesterday", yesterdayList)
        if (weekList.isNotEmpty()) put("Last 7 days", weekList)
        if (olderList.isNotEmpty()) put("Older", olderList)
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

/** New-chat screen: pick a topic (sets the persona prompt), then tap a starter question to send it. */
@Composable
private fun EmptyHomeState(
    category: Category,
    modelName: String,
    onCategorySelect: (Category) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onCreateImage: () -> Unit
) {
    // Box centres the content when it fits; the Column scrolls when it doesn't (small screens).
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LocAiLogo(size = 64.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "What can I help with?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        text = listOf("On-device", "Works offline", modelName).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                text = category.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Categories.ALL.forEach { c ->
                    FilterChip(
                        selected = c == category,
                        onClick = { onCategorySelect(c) },
                        label = { Text(c.displayName) },
                        leadingIcon = { Icon(c.icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                category.suggestedPrompts.forEach { prompt ->
                    SuggestionChip(
                        icon = category.icon,
                        tint = category.accent,
                        label = prompt,
                        onClick = { onSuggestionClick(prompt) }
                    )
                }
                SuggestionChip(
                    icon = Icons.Default.AutoAwesome,
                    label = "Create an image",
                    onClick = onCreateImage
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
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
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
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
    onStop: () -> Unit,
    onPlusClick: () -> Unit,
    onMicClick: () -> Unit,
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
            trailingIcon = {
                IconButton(onClick = onMicClick, enabled = isEnabled) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice input")
                }
            },
            enabled = isEnabled,
            shape = RoundedCornerShape(24.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        if (isGenerating) {
            FilledIconButton(onClick = onStop, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "Stop generating", modifier = Modifier.size(22.dp))
            }
        } else {
            FilledIconButton(
                onClick = onSend,
                enabled = isEnabled && text.isNotBlank(),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
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

/** User turns are bubbles; assistant replies are full-width markdown with an action row (copy/share/regenerate). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeChatBubble(
    message: MessageEntity,
    isStreaming: Boolean = false,
    isSpeaking: Boolean = false,
    onSpeak: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copy = {
        clipboard.setText(AnnotatedString(message.content))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }

    if (message.role == MessageRole.USER) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .combinedClickable(onClick = {}, onLongClick = copy, onLongClickLabel = "Copy message")
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
                                    .padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                    if (message.content.isNotEmpty()) {
                        Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            LocAiLogo(size = 24.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                if (isStreaming && message.content.isEmpty()) TypingDots()
                else MarkdownText(text = message.content)
            }
        }
        if (!isStreaming && message.content.isNotEmpty()) {
            Row(modifier = Modifier.padding(start = 26.dp)) {
                MessageAction(Icons.Default.ContentCopy, "Copy", copy)
                MessageAction(Icons.Default.Share, "Share") { shareText(context, message.content) }
                onSpeak?.let {
                    if (isSpeaking) MessageAction(Icons.Default.StopCircle, "Stop reading", it)
                    else MessageAction(Icons.AutoMirrored.Filled.VolumeUp, "Read aloud", it)
                }
                onRegenerate?.let { MessageAction(Icons.Default.Refresh, "Regenerate", it) }
            }
        }
    }
}

@Composable
private fun MessageAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 160), RepeatMode.Reverse),
                label = "dot$i"
            )
            Box(
                modifier = Modifier.size(8.dp).alpha(alpha).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
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

private fun shareText(context: android.content.Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
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
