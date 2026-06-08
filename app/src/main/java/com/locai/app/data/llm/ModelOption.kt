package com.locai.app.data.llm

/**
 * On-device models the user can choose from in Setup. Both are Qwen2.5-Instruct checkpoints
 * pre-converted to MediaPipe's `.task` format by the LiteRT team and released under Apache-2.0 —
 * openly downloadable with no account, sign-in, or access token (unlike Google's gated Gemma
 * weights), much like pulling a model with Ollama. Because they share the Qwen family, both use
 * the same ChatML prompt format (see [PromptBuilder]).
 */
enum class ModelOption(
    val id: String,
    val displayName: String,
    val description: String,
    val fileName: String,
    val downloadUrl: String,
    val approxSizeBytes: Long
) {
    FAST(
        id = "qwen2.5-0.5b-instruct",
        displayName = "Fast & Light",
        description = "Qwen2.5 0.5B-Instruct — quick replies, runs smoothly on virtually any phone.",
        fileName = "qwen2.5-0.5b-instruct-q8.task",
        downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/" +
            "Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        approxSizeBytes = 521L * 1024 * 1024
    ),
    QUALITY(
        id = "qwen2.5-1.5b-instruct",
        displayName = "Best Quality",
        description = "Qwen2.5 1.5B-Instruct — noticeably sharper, more capable answers; needs a " +
            "more powerful phone and a longer one-time download.",
        fileName = "qwen2.5-1.5b-instruct-q8.task",
        downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/" +
            "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        approxSizeBytes = 1525L * 1024 * 1024
    );

    companion object {
        val DEFAULT = FAST

        fun byId(id: String?): ModelOption = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
