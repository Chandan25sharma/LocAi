package com.locai.app.ui.imagegen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locai.app.LocAiContainer
import com.locai.app.data.llm.GeneratorModelOption
import com.locai.app.ui.LambdaViewModelFactory
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerationScreen(
    container: LocAiContainer,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel: ImageGenerationViewModel = viewModel(
        factory = LambdaViewModelFactory { ImageGenerationViewModel(container) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.savedToGallery) {
        if (uiState.savedToGallery) viewModel.acknowledgedSave()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Image") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isModelInstalled) {
                ModelNotInstalledCard(onOpenSettings = onOpenSettings)
            } else {
                OutlinedTextField(
                    value = uiState.prompt,
                    onValueChange = viewModel::onPromptChanged,
                    label = { Text("Describe the image") },
                    placeholder = { Text("A serene mountain lake at sunset, photorealistic") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isGenerating,
                    minLines = 3,
                    maxLines = 6
                )

                IterationsSlider(
                    value = uiState.iterations,
                    onValueChange = viewModel::onIterationsChanged,
                    enabled = !uiState.isGenerating
                )

                Text(
                    text = GeneratorModelOption.DEVICE_REQUIREMENTS,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = viewModel::generate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isGenerating && uiState.prompt.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(if (uiState.isGenerating) "Generating…" else "Generate")
                }

                when {
                    uiState.isGenerating -> GeneratingProgress(isLoading = uiState.isLoading)
                    uiState.generatedImage != null -> GeneratedImageResult(
                        uiState = uiState,
                        onSave = { viewModel.saveToGallery(context) },
                        onRegenerate = viewModel::generate
                    )
                }

                uiState.errorMessage?.let { msg ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::dismissError) { Text("Dismiss") }
                        }
                    }
                }
            }
        }

        if (uiState.savedToGallery) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp), contentAlignment = Alignment.BottomCenter) {
                Snackbar { Text("Saved to gallery") }
            }
        }
    }
}

@Composable
private fun ModelNotInstalledCard(onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Image generation model not installed", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "This feature requires a converted Stable Diffusion v1.5 model " +
                    "(${GeneratorModelOption.APPROX_SIZE}) placed in app storage. " +
                    GeneratorModelOption.DEVICE_REQUIREMENTS,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "See Settings → Image generation for installation instructions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Go to Settings")
            }
        }
    }
}

@Composable
private fun IterationsSlider(value: Int, onValueChange: (Int) -> Unit, enabled: Boolean) {
    Column {
        Text(
            text = "Quality steps: $value  (more = better quality, slower)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 5f..50f,
            steps = 8,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GeneratingProgress(isLoading: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Text(
            text = if (isLoading) "Loading model…" else "Generating image — this can take a while…",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Keep the screen on. Generation can take 30 seconds to several minutes depending on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GeneratedImageResult(
    uiState: ImageGenerationUiState,
    onSave: () -> Unit,
    onRegenerate: () -> Unit
) {
    val bitmap = uiState.generatedImage ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(modifier = Modifier.height(4.dp))
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Generated image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onRegenerate, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Regenerate")
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Save")
            }
        }
    }
}
