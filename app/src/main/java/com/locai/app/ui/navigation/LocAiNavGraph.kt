package com.locai.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.locai.app.LocAiContainer
import com.locai.app.ui.categories.CategoryScreen
import com.locai.app.ui.chat.ChatScreen
import com.locai.app.ui.history.HistoryScreen
import com.locai.app.ui.setup.ModelSetupScreen
import com.locai.app.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first

private const val ARG_CONVERSATION_ID = "conversationId"
private const val ARG_CATEGORY_ID = "categoryId"

object LocAiDestinations {
    const val ROUTER = "router"
    const val SETUP = "setup"
    const val CATEGORIES = "categories"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val CHAT = "chat/{$ARG_CONVERSATION_ID}/{$ARG_CATEGORY_ID}"

    fun chatRoute(conversationId: Long, categoryId: String) = "chat/$conversationId/$categoryId"
}

@Composable
fun LocAiNavGraph(
    container: LocAiContainer,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = LocAiDestinations.ROUTER) {

        // Decides, once, whether the user still needs to download the model or can go
        // straight into the app (the model is cached in app-internal storage between launches).
        composable(LocAiDestinations.ROUTER) {
            LaunchedEffect(Unit) {
                val ready = container.preferences.isModelDownloaded.first() && container.activeModelFile().exists()
                val destination = if (ready) LocAiDestinations.CATEGORIES else LocAiDestinations.SETUP
                navController.navigate(destination) {
                    popUpTo(LocAiDestinations.ROUTER) { inclusive = true }
                }
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        composable(LocAiDestinations.SETUP) {
            ModelSetupScreen(
                container = container,
                onModelReady = {
                    navController.navigate(LocAiDestinations.CATEGORIES) {
                        popUpTo(LocAiDestinations.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(LocAiDestinations.CATEGORIES) {
            CategoryScreen(
                container = container,
                onOpenChat = { conversationId, categoryId ->
                    navController.navigate(LocAiDestinations.chatRoute(conversationId, categoryId))
                },
                onOpenHistory = { navController.navigate(LocAiDestinations.HISTORY) },
                onOpenSettings = { navController.navigate(LocAiDestinations.SETTINGS) }
            )
        }

        composable(
            route = LocAiDestinations.CHAT,
            arguments = listOf(
                navArgument(ARG_CONVERSATION_ID) { type = NavType.LongType },
                navArgument(ARG_CATEGORY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong(ARG_CONVERSATION_ID) ?: return@composable
            val categoryId = backStackEntry.arguments?.getString(ARG_CATEGORY_ID) ?: return@composable
            ChatScreen(
                container = container,
                conversationId = conversationId,
                categoryId = categoryId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(LocAiDestinations.HISTORY) {
            HistoryScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onOpenConversation = { conversationId, categoryId ->
                    navController.navigate(LocAiDestinations.chatRoute(conversationId, categoryId))
                }
            )
        }

        composable(LocAiDestinations.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onModelReset = {
                    navController.navigate(LocAiDestinations.SETUP) {
                        popUpTo(LocAiDestinations.CATEGORIES) { inclusive = true }
                    }
                }
            )
        }
    }
}
