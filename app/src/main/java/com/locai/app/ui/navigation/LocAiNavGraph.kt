package com.locai.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.locai.app.LocAiContainer
import com.locai.app.ui.home.HomeChatScreen
import com.locai.app.ui.imagegen.ImageGenerationScreen
import com.locai.app.ui.setup.ModelSetupScreen
import com.locai.app.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first

object LocAiDestinations {
    const val ROUTER = "router"
    const val SETUP = "setup"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CREATE_IMAGE = "create_image"
}

@Composable
fun LocAiNavGraph(
    container: LocAiContainer,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = LocAiDestinations.ROUTER) {

        composable(LocAiDestinations.ROUTER) {
            LaunchedEffect(Unit) {
                val ready = container.preferences.isModelDownloaded.first() && container.activeModelFile().exists()
                val destination = if (ready) LocAiDestinations.HOME else LocAiDestinations.SETUP
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
                    navController.navigate(LocAiDestinations.HOME) {
                        popUpTo(LocAiDestinations.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(LocAiDestinations.HOME) {
            HomeChatScreen(
                container = container,
                onOpenSettings = { navController.navigate(LocAiDestinations.SETTINGS) },
                onCreateImage = { navController.navigate(LocAiDestinations.CREATE_IMAGE) }
            )
        }

        composable(LocAiDestinations.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onModelReset = {
                    navController.navigate(LocAiDestinations.SETUP) {
                        popUpTo(LocAiDestinations.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(LocAiDestinations.CREATE_IMAGE) {
            ImageGenerationScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(LocAiDestinations.SETTINGS) }
            )
        }
    }
}
