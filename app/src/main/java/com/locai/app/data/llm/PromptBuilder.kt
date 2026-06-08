package com.locai.app.data.llm

import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole
import com.locai.app.data.retrieval.RetrievedExchange
import com.locai.app.domain.Category
import com.locai.app.domain.UserPersona

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
        newUserMessage: String,
        userName: String? = null,
        userPersona: UserPersona? = null
    ): String = buildString {
        appendLine("<|im_start|>system")
        appendLine(GENERAL_GUIDANCE)
        if (!userName.isNullOrBlank() || userPersona != null) {
            appendLine()
            if (!userName.isNullOrBlank()) {
                append("The user's name is $userName — address them by it naturally now and ")
                append("then, the way a person who knows them would, not in every message. ")
            }
            userPersona?.let { append(it.promptHint) }
        }
        appendLine()
        appendLine()
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

    /**
     * Prepended ahead of every persona so the model never deflects: small instruct models
     * tend to give vague, generic, or partial replies when a question strays from the
     * current topic's framing — this keeps answers direct and complete regardless of category.
     */
    private const val GENERAL_GUIDANCE =
        "No matter what topic this conversation is framed around, always give the user a " +
            "direct, complete, and specific answer to exactly what they asked — never refuse, " +
            "deflect, or claim a question is 'outside your scope'. If it strays from this " +
            "topic's specialty, just answer it well using your general knowledge, then return " +
            "to the specialty framing only where it's actually relevant. Address every part of " +
            "the question, be concrete rather than generic, and keep going until the answer is " +
            "actually useful — a short, vague, or half-finished reply is worse than a longer, " +
            "complete one."

    private const val MAX_RECENT_TURNS = 6
    private const val MAX_SNIPPET_CHARS = 400
}
