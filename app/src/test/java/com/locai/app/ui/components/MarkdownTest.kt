package com.locai.app.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {

    @Test
    fun splitsFencedCodeAndKeepsUnclosedFenceAsCode() {
        val blocks = splitCodeBlocks("Intro\n```kotlin\nval x = 1\n```\nAfter\n```\nstreaming")
        assertEquals(
            listOf(
                MdBlock.Prose("Intro"),
                MdBlock.Code("val x = 1", "kotlin"),
                MdBlock.Prose("After"),
                MdBlock.Code("streaming", "")
            ),
            blocks
        )
    }

    @Test
    fun stripsInlineMarkersHeadingsAndBullets() {
        val out = proseToAnnotated("# Title\n- **bold** and *it* and `code`\n2 * 3 = 6", Color.Gray)
        assertEquals("Title\n•  bold and it and code\n2 * 3 = 6", out.text)
        assertEquals(4, out.spanStyles.size) // heading, bold, italic, code — "2 * 3" stays plain
    }
}
