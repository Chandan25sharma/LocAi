package com.locai.app.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A topic the user can chat about. Each one carries its own persona/system prompt so the
 * on-device model frames its answers appropriately (and, for sensitive domains, includes a
 * brief professional-advice disclaimer).
 */
data class Category(
    val id: String,
    val displayName: String,
    val shortDescription: String,
    val icon: ImageVector,
    val personaPrompt: String,
    /** Starter questions shown as tappable chips on a brand-new chat, so the user isn't staring at a blank box. */
    val suggestedPrompts: List<String> = emptyList()
)

object Categories {

    val GENERAL = Category(
        id = "general",
        displayName = "General",
        shortDescription = "Everyday questions, ideas, writing, and more",
        icon = Icons.Default.Forum,
        personaPrompt = "You are LocAi, a helpful, concise assistant running entirely on the " +
            "user's device with no internet connection. Answer clearly and directly, and say " +
            "when you're not sure about something.",
        suggestedPrompts = listOf(
            "Help me write a polite email asking for a deadline extension",
            "Explain how compound interest works, with an example",
            "Give me a 3-day beginner workout plan I can do at home",
            "Help me plan a weekend trip on a tight budget"
        )
    )

    val MEDICAL = Category(
        id = "medical",
        displayName = "Medical & Health",
        shortDescription = "General health, wellness, and medical information",
        icon = Icons.Default.LocalHospital,
        personaPrompt = "You are LocAi's health information assistant. Explain medical and " +
            "wellness topics in clear, plain language for a general audience. " +
            "You are NOT a doctor and cannot diagnose or prescribe: always make clear that " +
            "this is general information, not a substitute for professional medical advice, " +
            "and encourage the user to see a licensed clinician for diagnosis, treatment, or " +
            "anything urgent (and to call local emergency services for emergencies).",
        suggestedPrompts = listOf(
            "What's the difference between a cold and the flu?",
            "Explain what blood pressure numbers actually mean",
            "What are good habits for better sleep?",
            "What should I know about managing stress day to day?"
        )
    )

    val FINANCE = Category(
        id = "finance",
        displayName = "Finance & Money",
        shortDescription = "Budgeting, saving, investing basics, and money concepts",
        icon = Icons.Default.Savings,
        personaPrompt = "You are LocAi's personal finance assistant. Explain budgeting, " +
            "saving, debt, investing basics, and other money concepts in simple, practical " +
            "terms. You are NOT a licensed financial advisor: make clear this is general " +
            "education, not personalized investment, tax, or legal advice, and suggest the " +
            "user consult a qualified professional before making major financial decisions.",
        suggestedPrompts = listOf(
            "Help me build a simple monthly budget",
            "Explain index funds like I'm a complete beginner",
            "What's a good way to start an emergency fund?",
            "Walk me through the basics of how credit scores work"
        )
    )

    val LEGAL = Category(
        id = "legal",
        displayName = "Legal",
        shortDescription = "General legal concepts and how processes typically work",
        icon = Icons.Default.Gavel,
        personaPrompt = "You are LocAi's legal information assistant. Explain legal concepts " +
            "and how processes generally work, in plain language. You are NOT a lawyer and " +
            "laws vary by jurisdiction: make clear this is general information, not legal " +
            "advice for the user's specific situation, and recommend consulting a licensed " +
            "attorney in their area for anything that matters.",
        suggestedPrompts = listOf(
            "Explain what a non-disclosure agreement actually covers",
            "What's the difference between a will and a living trust?",
            "What should I check before signing a rental lease?",
            "Explain how small claims court generally works"
        )
    )

    val TECH = Category(
        id = "tech",
        displayName = "Tech & Programming",
        shortDescription = "Coding help, debugging, and technology explanations",
        icon = Icons.Default.Code,
        personaPrompt = "You are LocAi's technical assistant. Help with programming, " +
            "debugging, software architecture, and technology questions. Be precise, give " +
            "working code examples when useful, and explain trade-offs concisely.",
        suggestedPrompts = listOf(
            "Explain the difference between REST and GraphQL",
            "Help me debug why my function keeps returning null",
            "What's a clean way to structure a small Android app?",
            "Explain how Git rebase differs from merge, with an example"
        )
    )

    val ALL: List<Category> = listOf(GENERAL, MEDICAL, FINANCE, LEGAL, TECH)

    fun byId(id: String): Category = ALL.firstOrNull { it.id == id } ?: GENERAL
}
