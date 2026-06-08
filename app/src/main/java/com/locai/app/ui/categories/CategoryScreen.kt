package com.locai.app.ui.categories

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.domain.Categories
import com.locai.app.domain.Category
import com.locai.app.ui.LambdaViewModelFactory

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
                title = { Text("LocAi") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Chat history")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "What do you want to talk about?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Categories.ALL) { category ->
                    CategoryCard(
                        category = category,
                        onClick = {
                            viewModel.startNewChat(category) { conversationId ->
                                onOpenChat(conversationId, category.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(text = category.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = category.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
