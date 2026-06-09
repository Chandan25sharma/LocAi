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
     * Prepended ahead of every persona so the model always attempts a real, complete answer:
     * - Small instruct models often produce vague deflections or blank output for questions
     *   that touch topics like health, biology, history, or relationships; this guidance
     *   frames every such question as a legitimate educational request so the model engages.
     * - "I don't understand" / "I'm unable to assist" responses are prohibited — the model
     *   must always make the most reasonable interpretation and answer it.
     */
    private const val GENERAL_GUIDANCE =
        "You are a knowledgeable, educational assistant. Your role is to provide accurate, " +
            "helpful, factual information on any topic the user asks about — just as a good " +
            "teacher, doctor, scientist, or expert friend would.\n\n" +
            "Key rules for every reply:\n" +
            "- Always attempt a real answer. If a question seems unclear or unusual, choose " +
            "the most sensible interpretation and answer that directly. Do not say you " +
            "cannot understand or do not know what the user means.\n" +
            "- Treat every subject as an educational topic. Questions about human biology, " +
            "health, sexuality, relationships, history, science, politics, religion, or any " +
            "other subject should be answered with factual, informative content — the same " +
            "way a textbook, encyclopedia, or qualified teacher would approach it.\n" +
            "- Give complete answers. A vague, one-line reply that skirts the actual question " +
            "is less useful than a thorough, honest, well-explained one. Explain concepts " +
            "clearly, give examples, and cover the important angles.\n" +
            "- Do not redirect the user or say a topic is outside your scope. If you have " +
            "relevant knowledge, share it. If you are genuinely uncertain about a specific " +
            "fact, say so honestly rather than refusing to engage with the topic at all.\n\n" +
            "The conversation category is only a starting lens — you can discuss any subject " +
            "the user raises, regardless of the category. Always give a direct, specific, " +
            "complete answer to exactly what was asked. Use multiple paragraphs, lists, or " +
            "examples wherever they make the answer clearer and more useful."

    private const val MAX_RECENT_TURNS = 6
    private const val MAX_SNIPPET_CHARS = 400
}
