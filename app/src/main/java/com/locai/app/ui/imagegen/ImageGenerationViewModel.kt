package com.locai.app.ui.imagegen

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locai.app.LocAiContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImageGenerationUiState(
    val prompt: String = "",
    val iterations: Int = 20,
    val isModelInstalled: Boolean = false,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generatedImage: Bitmap? = null,
    val errorMessage: String? = null,
    val savedToGallery: Boolean = false
)

class ImageGenerationViewModel(private val container: LocAiContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ImageGenerationUiState(
            isModelInstalled = container.imageGeneratorManager.isInstalled(container.generatorModelDir)
        )
    )
    val uiState: StateFlow<ImageGenerationUiState> = _uiState.asStateFlow()

    fun onPromptChanged(text: String) {
        _uiState.update { it.copy(prompt = text, errorMessage = null) }
    }

    fun onIterationsChanged(value: Int) {
        _uiState.update { it.copy(iterations = value) }
    }

    fun generate() {
        val current = _uiState.value
        if (current.prompt.isBlank() || current.isGenerating || !current.isModelInstalled) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, isLoading = true, errorMessage = null, generatedImage = null, savedToGallery = false) }
            try {
                container.imageGeneratorManager.ensureLoaded(container.generatorModelDir).onFailure { e ->
                    throw IllegalStateException("Couldn't load the generation model: ${e.message}")
                }
                _uiState.update { it.copy(isLoading = false) }

                val seed = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
                container.imageGeneratorManager.generate(
                    prompt = current.prompt.trim(),
                    iterations = current.iterations,
                    seed = seed
                ).fold(
                    onSuccess = { bitmap -> _uiState.update { it.copy(generatedImage = bitmap) } },
                    onFailure = { e -> _uiState.update { it.copy(errorMessage = "Generation failed: ${e.message}") } }
                )
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = t.message ?: "Unknown error") }
            } finally {
                _uiState.update { it.copy(isGenerating = false, isLoading = false) }
            }
        }
    }

    fun saveToGallery(context: Context) {
        val bitmap = _uiState.value.generatedImage ?: return
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { writeBitmapToGallery(context, bitmap) }
            if (saved) {
                _uiState.update { it.copy(savedToGallery = true) }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not save image to gallery") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun acknowledgedSave() {
        _uiState.update { it.copy(savedToGallery = false) }
    }

    private fun writeBitmapToGallery(context: Context, bitmap: Bitmap): Boolean = runCatching {
        val fileName = "LocAi_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LocAi")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@runCatching false

        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    }.getOrDefault(false)
}
