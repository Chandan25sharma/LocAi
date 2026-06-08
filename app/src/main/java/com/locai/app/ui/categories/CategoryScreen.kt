package com.locai.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.domain.Categories
import com.locai.app.domain.Category
import com.locai.app.ui.LambdaViewModelFactory
import com.locai.app.ui.components.LocAiLogo
import com.locai.app.ui.components.LocAiMascot
import com.locai.app.ui.theme.AccentAmber
import com.locai.app.ui.theme.AccentBlue
import com.locai.app.ui.theme.AccentDeepIndigo
import com.locai.app.ui.theme.AccentGreen
import com.locai.app.ui.theme.AccentViolet
import com.locai.app.ui.theme.PrivacyBannerEnd
import com.locai.app.ui.theme.PrivacyBannerStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    container: LocAiContainer,
    onOpenChat: (conversationId: Long, categoryId: String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel: CategoryViewModel = viewModel(factory = LambdaViewModelFactory { CategoryViewModel(container) })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LocAiLogo(size = 34.dp)
                        Column {
                            Text(text = "LocAi", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Your AI. Your Device. Your Privacy.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenHistory,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Chat history")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader()
            }
            items(Categories.ALL) { category ->
                CategoryGridCard(
                    category = category,
                    onClick = {
                        viewModel.startNewChat(category) { conversationId ->
                            onOpenChat(conversationId, category.id)
                        }
                    }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeatureBadgeRow(modifier = Modifier.padding(top = 4.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                PrivacyBanner(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Hello! 👋", style = MaterialTheme.typography.titleMedium)
            Text(
                text = buildAnnotatedString {
                    append("What's on ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("your mind?")
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "Pick a topic and start chatting — everything stays private, on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        LocAiMascot(size = 76.dp)
    }
}

private fun accentColorFor(categoryId: String): Color = when (categoryId) {
    "general" -> AccentViolet
    "medical" -> AccentGreen
    "finance" -> AccentAmber
    "legal" -> AccentBlue
    "tech" -> AccentDeepIndigo
    else -> AccentViolet
}

/**
 * Tints the card with the topic's accent without baking in a fixed light pastel: blending a thin
 * layer of the accent over the *current* surface keeps good text contrast in both themes — a
 * flat pastel would wash out against light text in dark mode (and vice versa).
 */
@Composable
private fun softAccentColorFor(categoryId: String): Color =
    accentColorFor(categoryId).copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)

@Composable
private fun CategoryGridCard(category: Category, onClick: () -> Unit) {
    val accent = accentColorFor(category.id)
    val soft = softAccentColorFor(category.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = soft),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = category.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = category.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.End)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureBadgeRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FeatureBadge(
            icon = Icons.Default.Lock,
            label = "100% Private",
            caption = "Your data never leaves your device",
            modifier = Modifier.weight(1f)
        )
        FeatureBadge(
            icon = Icons.Default.CloudOff,
            label = "Works Offline",
            caption = "No internet needed, use anytime",
            modifier = Modifier.weight(1f)
        )
        FeatureBadge(
            icon = Icons.Default.FlashOn,
            label = "Powered Locally",
            caption = "Fast, secure, and built for your phone",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureBadge(icon: ImageVector, label: String, caption: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PrivacyBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrivacyBannerStart, PrivacyBannerEnd),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "100% Private. 100% Local.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "No servers. No tracking. Your conversations never leave your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "Offline AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
