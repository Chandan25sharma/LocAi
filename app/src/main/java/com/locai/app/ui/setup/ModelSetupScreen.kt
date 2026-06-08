package com.locai.app.ui.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.llm.ModelOption
import com.locai.app.ui.LambdaViewModelFactory
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

    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) onModelReady()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Welcome to LocAi", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "LocAi runs a small AI model entirely on your phone. Nothing you type — and " +
                "nothing it answers — ever leaves this device, except for this one-time download " +
                "of the model itself. The models below are openly licensed (Apache 2.0): no " +
                "account, sign-in, or access token needed — just pick one and download it, the " +
                "same way you'd pull a model with Ollama.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(text = "Choose a model", style = MaterialTheme.typography.titleMedium)

        uiState.availableModels.forEach { option ->
            ModelOptionCard(
                option = option,
                selected = uiState.selectedModel == option,
                enabled = !uiState.isDownloading && !uiState.isReady,
                onSelect = { viewModel.onModelSelected(option) }
            )
        }

        Button(
            onClick = viewModel::startDownload,
            enabled = !uiState.isDownloading && !uiState.isReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.isDownloading) "Downloading…"
                else "Download ${uiState.selectedModel.displayName} " +
                    "(~${megabytes(uiState.selectedModel.approxSizeBytes)} MB, one time)"
            )
        }

        if (uiState.isDownloading) {
            val total = uiState.totalBytes
            if (total > 0) {
                val fraction = (uiState.downloadedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    text = "${megabytes(uiState.downloadedBytes)} MB / ${megabytes(total)} MB " +
                        "(${(fraction * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "${megabytes(uiState.downloadedBytes)} MB downloaded…",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (uiState.isReady) {
            Row {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("Model ready — entering LocAi…")
            }
        }

        uiState.errorMessage?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect, enabled = enabled)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${option.displayName} · ~${megabytes(option.approxSizeBytes)} MB",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(text = option.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun megabytes(bytes: Long): Int = (bytes / (1024 * 1024)).toInt()
