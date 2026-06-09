package com.locai.app.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.llm.ModelOption
import com.locai.app.domain.UserPersona
import com.locai.app.ui.LambdaViewModelFactory
import com.locai.app.ui.components.LocAiLogo
import com.locai.app.ui.components.LocAiMascot
import com.locai.app.ui.theme.AccentAmber
import com.locai.app.ui.theme.AccentGreen
import com.locai.app.ui.theme.AccentViolet
import com.locai.app.ui.theme.PrivacyBannerEnd
import com.locai.app.ui.theme.PrivacyBannerStart
import kotlin.math.roundToInt

@Composable
fun ModelSetupScreen(
    container: LocAiContainer,
    onModelReady: () -> Unit
) {
    val viewModel: ModelSetupViewModel = viewModel(
        factory = LambdaViewModelFactory { ModelSetupViewModel(container) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) onModelReady()
    }
    // Auto-start download when reaching the MODEL step (handles both new users and returning users
    // whose download hadn't finished — the foreground service keeps it alive across minimisation).
    LaunchedEffect(uiState.step) {
        if (uiState.step == OnboardingStep.MODEL && !uiState.isReady && !uiState.isDownloading && uiState.errorMessage == null) {
            viewModel.startDownload(context)
        }
    }

    when (uiState.step) {
        OnboardingStep.WELCOME -> WelcomeStep(uiState = uiState, viewModel = viewModel, context = context)
        OnboardingStep.MODEL -> ModelStep(uiState = uiState, viewModel = viewModel, context = context)
    }
}

@Composable
private fun WelcomeStep(uiState: ModelSetupUiState, viewModel: ModelSetupViewModel, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LocAiLogo(size = 40.dp)
        LocAiMascot()
        Text(
            text = "Welcome to LocAi",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Smart answers, private conversations — everything stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(text = "Let's personalize your experience", style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "What should I call you?", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = uiState.userName,
                    onValueChange = viewModel::onNameChanged,
                    placeholder = { Text("Your name (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "How will you mainly use LocAi?", style = MaterialTheme.typography.labelLarge)
                UserPersona.entries.forEach { persona ->
                    PersonaCard(
                        persona = persona,
                        selected = uiState.selectedPersona == persona,
                        onSelect = { viewModel.onPersonaSelected(persona) }
                    )
                }
            }

            Button(
                onClick = { viewModel.continueFromWelcome(context) },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(listOf(PrivacyBannerStart, PrivacyBannerEnd)),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Text("Continue", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% private · 100% local — nothing you share here ever leaves this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun personaAccent(persona: UserPersona): Color = when (persona) {
    UserPersona.DEVELOPER -> AccentViolet
    UserPersona.STUDENT -> AccentGreen
    UserPersona.GENERAL -> AccentAmber
}

@Composable
private fun PersonaCard(persona: UserPersona, selected: Boolean, onSelect: () -> Unit) {
    val accent = personaAccent(persona)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        border = if (selected) BorderStroke(1.5.dp, accent) else null,
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f))
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = persona.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = persona.displayName, style = MaterialTheme.typography.titleSmall)
                Text(text = persona.description, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = accent
                )
            }
        }
    }
}

@Composable
private fun ModelStep(uiState: ModelSetupUiState, viewModel: ModelSetupViewModel, context: android.content.Context) {
    if (uiState.isReady) {
        GettingReadyStep(uiState = uiState)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val greeting = if (uiState.userName.isNotBlank()) "Hi ${uiState.userName}! " else ""
        Text(text = "Setting up your AI", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "${greeting}Based on how you'll use LocAi, we picked \"${uiState.selectedModel.displayName}\" " +
                "for you — it's downloading automatically below. Everything is openly licensed " +
                "(Apache 2.0): no account, sign-in, or access token needed, the same way you'd " +
                "pull a model with Ollama. Nothing you type — or LocAi answers — ever leaves " +
                "this device, except for this one-time download.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(text = "Model", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Switching here restarts the download with the new pick.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        uiState.availableModels.forEach { option ->
            ModelOptionCard(
                option = option,
                selected = uiState.selectedModel == option,
                enabled = !uiState.isDownloading && !uiState.isReady,
                onSelect = { viewModel.onModelSelected(context, option) }
            )
        }

        if (uiState.errorMessage != null) {
            Button(onClick = { viewModel.startDownload(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("Retry download")
            }
        }

        if (uiState.isDownloading || (!uiState.isReady && uiState.errorMessage == null)) {
            DownloadProgressCard(uiState = uiState)
        }

        uiState.errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DownloadProgressCard(uiState: ModelSetupUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PrivacyBannerStart, PrivacyBannerEnd)))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Downloading ${uiState.selectedModel.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            val total = uiState.totalBytes
            if (uiState.isDownloading && total > 0) {
                val fraction = (uiState.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Text(
                    text = "${megabytes(uiState.downloadedBytes)} MB / ${megabytes(total)} MB " +
                        "(${(fraction * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            } else if (uiState.isDownloading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Text(
                    text = "${megabytes(uiState.downloadedBytes)} MB downloaded…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            } else {
                Text(
                    text = "Preparing to download (~${megabytes(uiState.selectedModel.approxSizeBytes)} MB, one time)…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Text(
                text = "This happens only once — LocAi runs fully offline afterwards.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun GettingReadyStep(uiState: ModelSetupUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LocAiMascot()
        Text(
            text = "Your AI is getting ready 🎉",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Please wait while we set everything up on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ReadyFeatureRow(icon = Icons.Default.Lock, accent = AccentViolet, title = "100% Private", subtitle = "Nothing ever leaves this device")
            ReadyFeatureRow(icon = Icons.Default.WifiOff, accent = AccentGreen, title = "No Internet Needed", subtitle = "Works completely offline")
            ReadyFeatureRow(icon = Icons.Default.Memory, accent = AccentAmber, title = "Optimized For Your Device", subtitle = "Tuned to run smoothly on your phone")
        }

        Spacer(modifier = Modifier.size(8.dp))

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Preparing your AI", style = MaterialTheme.typography.labelLarge)
                Text(text = "Ready", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Take your time, you're almost there — entering LocAi…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReadyFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = accent)
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModelOptionCard(
    option: ModelOption,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    val accent = if (option == ModelOption.FAST) AccentViolet else AccentGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        border = if (selected) BorderStroke(1.5.dp, accent) else null,
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f))
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${option.displayName} · ~${megabytes(option.approxSizeBytes)} MB",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (selected) {
                        Surface(shape = RoundedCornerShape(50), color = accent) {
                            Text(
                                text = "SELECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(text = option.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun megabytes(bytes: Long): Int = (bytes / (1024 * 1024)).toInt()
