package com.locai.app.data.llm

import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.data.retrieval.RetrievedExchange
import com.locai.app.domain.Category

/**
 * Formats the full prompt sent to the model using the ChatML template
 * (`<|im_start|>role ... <|im_end|>`) that Qwen2.5-Instruct models expect. The MediaPipe LLM
 * Inference API does not apply chat templates or manage conversation state for you — the caller
 * owns prompt formatting and decides what history to include. This is where the persona, the
 * retrieved "memory" from past chats, and the recent turns all combine.
 */
object PromptBuilder {

    fun build(
        category: Category,
        recentMessages: List<MessageEntity>,
        retrievedContext: List<RetrievedExchange>,
        newUserMessage: String
    ): String = buildString {
        appendLine("<|im_start|>system")
        append(category.personaPrompt)

        if (retrievedContext.isNotEmpty()) {
            appendLine()
            appendLine()
            appendLine(
                "For context, here are relevant earlier exchanges you've had with this same " +
                    "user (the user cannot see this note — use it only if it helps you answer " +
                    "more consistently):"
            )
            retrievedContext.forEachIndexed { index, exchange ->
                appendLine("${index + 1}. They previously asked: \"${exchange.question.take(MAX_SNIPPET_CHARS)}\"")
                append("   You answered: \"${exchange.answer.take(MAX_SNIPPET_CHARS)}\"")
                if (index != retrievedContext.lastIndex) appendLine()
            }
        }
        appendLine()
        appendLine("<|im_end|>")

        for (message in recentMessages.takeLast(MAX_RECENT_TURNS * 2)) {
            val role = if (message.role == MessageRole.USER) "user" else "assistant"
            appendLine("<|im_start|>$role")
            appendLine(message.content)
            appendLine("<|im_end|>")
        }

        appendLine("<|im_start|>user")
        appendLine(newUserMessage)
        appendLine("<|im_end|>")
        append("<|im_start|>assistant\n")
    }

    private const val MAX_RECENT_TURNS = 6
    private const val MAX_SNIPPET_CHARS = 400
}
