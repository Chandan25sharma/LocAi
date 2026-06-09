package com.locai.app.data.llm

/**
 * The optional on-device Stable Diffusion v1.5 model for text-to-image generation.
 *
 * MediaPipe's ImageGenerator requires a *directory* of converted TFLite components (text encoder,
 * diffusion model, decoder, tokenizer) rather than a single downloadable file. Google's official
 * guide describes how to obtain or convert these files using their provided Python script —
 * there is no canonical single-URL pre-packaged bundle that can be downloaded automatically
 * the way the Qwen2.5 and Gemma weights can.
 *
 * The Settings card therefore surfaces installation instructions rather than a download button.
 * Once the user places the converted files in [DIR_NAME] inside `Context.filesDir/models/`,
 * the "Check for model" action verifies [REQUIRED_FILES] are all present and marks the
 * feature ready.
 */
object GeneratorModelOption {
    const val ID = "stable-diffusion-v1"
    const val DISPLAY_NAME = "Image Generation (Stable Diffusion v1.5)"
    const val DIR_NAME = "imagegen"

    val REQUIRED_FILES = listOf(
        "text_encoder.tflite",
        "diffusion_model.tflite",
        "decoder.tflite",
        "tokenizer_vocab.tflite"
    )

    const val APPROX_SIZE = "~1.5–2 GB"
    const val DEVICE_REQUIREMENTS =
        "Recommended: 8 GB+ RAM. Generation takes 30 seconds to 2+ minutes per image."
}
