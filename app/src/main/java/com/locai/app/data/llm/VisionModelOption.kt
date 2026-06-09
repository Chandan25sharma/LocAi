package com.locai.app.data.llm

/**
 * The optional on-device model that adds image understanding (attach a photo, ask about it).
 *
 * Qwen2.5-VL — the model the feature is conceptually named after — has no existing on-device
 * `.task`/`.litertlm` conversion; MediaPipe's documented vision-modality support targets the
 * Gemma multimodal family instead. This is the openly-licensed (Apache-2.0, no account or access
 * token needed) Gemma 4 E2B build from the same `litert-community` org as the base Qwen2.5
 * models, so the "no sign-in, just download" experience stays identical — it just happens to be
 * a different underlying model than the one named in the feature request, because that's what's
 * actually deployable on-device today.
 */
object VisionModelOption {
    const val ID = "gemma-4-e2b-vision"
    const val DISPLAY_NAME = "Image Understanding"
    const val DESCRIPTION = "Gemma 4 E2B (multimodal) — lets you attach a photo in chat and ask " +
        "questions about it. Optional extra download; needs a reasonably capable phone."
    const val FILE_NAME = "gemma-4-e2b-it-vision.litertlm"
    const val DOWNLOAD_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/" +
        "resolve/main/gemma-4-E2B-it.litertlm"
    const val APPROX_SIZE_BYTES = 2_600L * 1024 * 1024
}
