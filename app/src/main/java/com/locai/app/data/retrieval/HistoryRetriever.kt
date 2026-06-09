package com.locai.app.data.retrieval

import com.locai.app.data.db.MessageDao
import com.locai.app.data.db.MessageEntity
import com.locai.app.data.db.MessageRole

data class RetrievedExchange(
    val question: String,
    val answer: String,
    val timestamp: Long,
    val score: Double
)

/**
 * LocAi's on-device "memory": instead of (infeasible) on-device fine-tuning, this scores the
 * user's own past Q&A pairs in the same category against their new question using lexical
 * token overlap (Jaccard similarity), blended with a recency bonus, and surfaces the most
 * relevant ones for [com.locai.app.data.llm.PromptBuilder] to fold into the prompt. Fully
 * offline, no embedding model required — this is what makes answers feel like they build on
 * what the user has asked before.
 */
class HistoryRetriever(private val messageDao: MessageDao) {

    suspend fun retrieve(
        categoryId: String,
        excludeConversationId: Long,
        query: String,
        maxResults: Int = DEFAULT_MAX_RESULTS,
        minSimilarity: Double = DEFAULT_MIN_SIMILARITY
    ): List<RetrievedExchange> {
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()

        val pastMessages = messageDao.getRecentForCategory(
            categoryId = categoryId,
            excludeConversationId = excludeConversationId,
            limit = MAX_SCANNED_MESSAGES
        )
        val exchanges = pairUpExchanges(pastMessages)
        if (exchanges.isEmpty()) return emptyList()

        val newest = exchanges.maxOf { it.timestamp }.toDouble()
        val oldest = exchanges.minOf { it.timestamp }.toDouble()
        val span = (newest - oldest).coerceAtLeast(1.0)

        // Topical relevance gates inclusion; recency only re-ranks among matches that already
        // share real vocabulary with the new question. Recency must never be able to surface an
        // unrelated past exchange on its own — that's what previously made a brand-new topic
        // drag a completely different earlier conversation into the prompt as "context", and the
        // model would keep steering replies back toward that old topic.
        //
        // A bare similarity ratio isn't enough either: short questions tokenize down to just one
        // or two meaningful words after stopword removal, so sharing a SINGLE incidental word
        // ("today", "phone", ...) with a past question can already push Jaccard past the floor —
        // surfacing a completely unrelated old exchange as if it were "relevant context". Require
        // a minimum number of actually-shared meaningful words too, so a real topical overlap is
        // needed, not just a coincidence of small token sets.
        return exchanges
            .mapNotNull { exchange ->
                val pastTokens = tokenize(exchange.question)
                val shared = queryTokens.intersect(pastTokens)
                if (shared.size < MIN_SHARED_TOKENS) return@mapNotNull null
                val similarity = shared.size.toDouble() / queryTokens.union(pastTokens).size
                if (similarity < minSimilarity) return@mapNotNull null
                val recency = (exchange.timestamp - oldest) / span
                val score = similarity * SIMILARITY_WEIGHT + recency * RECENCY_WEIGHT
                exchange to score
            }
            .sortedByDescending { (_, score) -> score }
            .take(maxResults)
            .map { (exchange, score) ->
                RetrievedExchange(exchange.question, exchange.answer, exchange.timestamp, score)
            }
    }

    /** Past messages come back newest-first; walk oldest-to-newest pairing user -> assistant turns. */
    private fun pairUpExchanges(messages: List<MessageEntity>): List<RawExchange> {
        val ordered = messages.sortedBy { it.timestamp }
        val result = mutableListOf<RawExchange>()
        var pendingQuestion: MessageEntity? = null
        for (message in ordered) {
            when (message.role) {
                MessageRole.USER -> pendingQuestion = message
                MessageRole.ASSISTANT -> {
                    val question = pendingQuestion
                    if (question != null && question.conversationId == message.conversationId) {
                        result += RawExchange(question.content, message.content, message.timestamp)
                        pendingQuestion = null
                    }
                }
            }
        }
        return result
    }

    private data class RawExchange(val question: String, val answer: String, val timestamp: Long)

    private fun tokenize(text: String): Set<String> =
        text.lowercase()
            .split(TOKEN_DELIMITERS)
            .map { it.trim() }
            .filter { it.length > 2 && it !in STOPWORDS }
            .toSet()

    companion object {
        private const val DEFAULT_MAX_RESULTS = 3
        private const val DEFAULT_MIN_SIMILARITY = 0.2
        private const val MIN_SHARED_TOKENS = 2
        private const val MAX_SCANNED_MESSAGES = 200
        private const val SIMILARITY_WEIGHT = 0.85
        private const val RECENCY_WEIGHT = 0.15

        private val TOKEN_DELIMITERS = Regex("[^a-z0-9]+")
        private val STOPWORDS = setOf(
            "the", "and", "for", "are", "was", "were", "with", "that", "this", "what",
            "when", "where", "which", "how", "why", "does", "did", "can", "could",
            "would", "should", "about", "your", "you", "have", "has", "had", "not",
            "but", "they", "them", "their", "from", "into", "than", "then", "there"
        )
    }
}
