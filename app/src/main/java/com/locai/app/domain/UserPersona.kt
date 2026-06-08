package com.locai.app.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector
import com.locai.app.data.llm.ModelOption

/**
 * Who's using LocAi, captured once during onboarding. This isn't a "mode" the user switches —
 * it just nudges the model's tone/depth and the recommended model size, so answers fit the
 * person asking without them having to spell that out every time.
 */
enum class UserPersona(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val recommendedModel: ModelOption,
    val promptHint: String
) {
    DEVELOPER(
        id = "developer",
        displayName = "Developer",
        description = "Coding, debugging, and technical deep-dives",
        icon = Icons.Default.Code,
        recommendedModel = ModelOption.QUALITY,
        promptHint = "This user is a developer: feel free to use precise technical language, " +
            "code snippets, and assume familiarity with programming concepts."
    ),
    STUDENT(
        id = "student",
        displayName = "Student",
        description = "Learning new topics with clear, patient explanations",
        icon = Icons.Default.School,
        recommendedModel = ModelOption.FAST,
        promptHint = "This user is a student: explain things step by step, define any jargon " +
            "you use, and favor clarity over brevity."
    ),
    GENERAL(
        id = "general",
        displayName = "General use",
        description = "Everyday questions, planning, and writing help",
        icon = Icons.Default.Person,
        recommendedModel = ModelOption.FAST,
        promptHint = "This user wants practical, everyday help: keep answers simple, " +
            "concrete, and to the point."
    );

    companion object {
        val DEFAULT = GENERAL
        fun byId(id: String?): UserPersona? = entries.firstOrNull { it.id == id }
    }
}
