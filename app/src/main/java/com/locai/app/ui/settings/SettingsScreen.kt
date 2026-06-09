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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.llm.GeneratorModelOption
import com.locai.app.data.llm.VisionModelOption
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
    val context = LocalContext.current

    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmDeleteModel by remember { mutableStateOf(false) }
    var confirmDeleteVisionModel by remember { mutableStateOf(false) }
    var confirmDeleteGeneratorModel by remember { mutableStateOf(false) }

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

            SectionCard(title = "Image understanding (optional)") {
                val approxMb = VisionModelOption.APPROX_SIZE_BYTES / (1024 * 1024)
                Text(
                    text = when {
                        uiState.visionModelOnDisk ->
                            "Downloaded · ${uiState.visionModelSizeMb} MB cached on this device"
                        uiState.isVisionDownloading -> {
                            val downloadedMb = uiState.visionDownloadedBytes / (1024 * 1024)
                            if (uiState.visionTotalBytes > 0) {
                                val totalMb = uiState.visionTotalBytes / (1024 * 1024)
                                "Downloading… $downloadedMb / $totalMb MB"
                            } else {
                                "Downloading… $downloadedMb MB"
                            }
                        }
                        else -> "Not downloaded — about $approxMb MB"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Optional extra download that lets you attach a photo in chat and ask " +
                        "questions about it — useful on a reasonably capable phone. Everything " +
                        "still runs fully on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                uiState.visionErrorMessage?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                when {
                    uiState.visionModelOnDisk -> OutlinedButton(
                        onClick = { confirmDeleteVisionModel = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete image understanding model") }

                    uiState.isVisionDownloading -> {
                        val fraction = if (uiState.visionTotalBytes > 0) {
                            uiState.visionDownloadedBytes.toFloat() / uiState.visionTotalBytes
                        } else null
                        if (fraction != null) {
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    else -> Button(onClick = { viewModel.downloadVisionModel(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Download image understanding")
                    }
                }
            }

            SectionCard(title = "Image generation (optional)") {
                Text(
                    text = if (uiState.generatorModelInstalled) "Model found — ready to generate images"
                           else "Model not found in expected location",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = GeneratorModelOption.DEVICE_REQUIREMENTS,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Generates images from text prompts entirely on this device — no internet " +
                        "or cloud required. Requires a converted Stable Diffusion v1.5 model " +
                        "(${GeneratorModelOption.APPROX_SIZE}). To install: run Google's MediaPipe " +
                        "SD conversion script, then copy the output directory to: " +
                        "Android/data/com.locai.app/files/models/${GeneratorModelOption.DIR_NAME}/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.generatorModelInstalled) {
                    OutlinedButton(
                        onClick = { confirmDeleteGeneratorModel = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete image generation model") }
                } else {
                    Button(
                        onClick = viewModel::refreshGeneratorModelInfo,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Check for model") }
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

    if (confirmDeleteVisionModel) {
        AlertDialog(
            onDismissRequest = { confirmDeleteVisionModel = false },
            title = { Text("Delete the image understanding model?") },
            text = { Text("You'll need to download it again to attach photos in chat.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteVisionModel = false
                    viewModel.deleteVisionModel()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteVisionModel = false }) { Text("Cancel") } }
        )
    }

    if (confirmDeleteGeneratorModel) {
        AlertDialog(
            onDismissRequest = { confirmDeleteGeneratorModel = false },
            title = { Text("Delete image generation model?") },
            text = { Text("The model directory will be removed. You will need to reinstall it manually to generate images again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteGeneratorModel = false
                    viewModel.deleteGeneratorModel()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteGeneratorModel = false }) { Text("Cancel") } }
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
