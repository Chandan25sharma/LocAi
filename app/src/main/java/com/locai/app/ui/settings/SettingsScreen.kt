package com.locai.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.ui.LambdaViewModelFactory
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: LocAiContainer,
    onBack: () -> Unit,
    onModelReset: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(factory = LambdaViewModelFactory { SettingsViewModel(container) })
    val uiState by viewModel.uiState.collectAsState()

    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmDeleteModel by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.modelDeleted) {
        if (uiState.modelDeleted) onModelReset()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionCard(title = "On-device model") {
                Text(
                    text = if (uiState.modelOnDisk) {
                        "${uiState.modelDisplayName} (${uiState.modelFileName}) · ${uiState.modelSizeMb} MB cached on this device"
                    } else {
                        "No model is currently cached on this device."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Everything runs locally — re-downloading is the only operation that " +
                        "uses the network.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { confirmDeleteModel = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete model & set up again")
                }
            }

            SectionCard(title = "Generation settings") {
                LabeledSlider(
                    label = "Temperature (creativity)",
                    value = uiState.temperature,
                    valueRange = 0.1f..1.5f,
                    valueLabel = String.format("%.2f", uiState.temperature),
                    onValueChange = viewModel::setTemperature
                )
                LabeledSlider(
                    label = "Top-K (vocabulary breadth)",
                    value = uiState.topK.toFloat(),
                    valueRange = 1f..100f,
                    valueLabel = uiState.topK.toString(),
                    onValueChange = { viewModel.setTopK(it.roundToInt()) }
                )
                LabeledSlider(
                    label = "Max reply length (tokens)",
                    value = uiState.maxTokens.toFloat(),
                    valueRange = 128f..2048f,
                    valueLabel = uiState.maxTokens.toString(),
                    onValueChange = { viewModel.setMaxTokens((it / 64).roundToInt() * 64) }
                )
            }

            SectionCard(title = "Your data") {
                Text(
                    text = "All conversations are stored only on this device. Deleting them here " +
                        "removes them permanently — there is no cloud copy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { confirmClearHistory = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear all chat history")
                }
            }
        }
    }

    if (confirmDeleteModel) {
        AlertDialog(
            onDismissRequest = { confirmDeleteModel = false },
            title = { Text("Delete the on-device model?") },
            text = { Text("You'll need to re-download it (~555 MB) before LocAi can answer again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteModel = false
                    viewModel.deleteModel()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteModel = false }) { Text("Cancel") } }
        )
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear all chat history?") },
            text = { Text("Every conversation in every topic will be permanently deleted from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearHistory = false
                    viewModel.clearHistory()
                }) { Text("Clear everything") }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(text = "$label: $valueLabel", style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}
