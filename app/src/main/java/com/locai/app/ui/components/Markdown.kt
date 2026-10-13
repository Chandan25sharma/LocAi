package com.locai.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * Small markdown renderer for model replies: fenced code blocks, headings, bullets, **bold**,
 * *italic* and `inline code`. Covers what small on-device models actually emit.
 * ponytail: no tables/links/nesting — swap in a markdown library if replies start needing them.
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { splitCodeBlocks(text) }
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> CodeBlock(block)
                is MdBlock.Prose -> Text(
                    text = remember(block.text, codeBg) { proseToAnnotated(block.text, codeBg) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CodeBlock(block: MdBlock.Code) {
    val clipboard = LocalClipboardManager.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.language.ifEmpty { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { clipboard.setText(AnnotatedString(block.code)) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(16.dp))
                }
            }
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = block.code,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current,
                    softWrap = false,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

sealed interface MdBlock {
    data class Prose(val text: String) : MdBlock
    data class Code(val code: String, val language: String) : MdBlock
}

/** Splits on ``` fences. An unclosed fence (reply still streaming) runs to the end as code. */
fun splitCodeBlocks(md: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val prose = StringBuilder()
    var code: StringBuilder? = null
    var language = ""

    fun flushProse() {
        val t = prose.toString().trim('\n')
        if (t.isNotBlank()) blocks += MdBlock.Prose(t)
        prose.clear()
    }

    md.lines().forEach { line ->
        val fence = line.trimStart().startsWith("```")
        when {
            fence && code == null -> {
                flushProse()
                language = line.trimStart().removePrefix("```").trim()
                code = StringBuilder()
            }
            fence -> {
                blocks += MdBlock.Code(code.toString().trimEnd('\n'), language)
                code = null
            }
            code != null -> code!!.append(line).append('\n')
            else -> prose.append(line).append('\n')
        }
    }
    code?.let { blocks += MdBlock.Code(it.toString().trimEnd('\n'), language) }
    flushProse()
    return blocks
}

private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
private val BULLET = Regex("""^(\s*)[-*+]\s+(.*)$""")
private val INLINE = Regex("""`([^`]+)`|\*\*(.+?)\*\*|__(.+?)__|\*(\S(?:.*?\S)?)\*""")

fun proseToAnnotated(text: String, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { i, raw ->
        if (i > 0) append('\n')
        val heading = HEADING.matchEntire(raw)
        val bullet = BULLET.matchEntire(raw)
        when {
            heading != null -> {
                val size = if (heading.groupValues[1].length <= 2) 1.2.em else 1.05.em
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size)) {
                    appendInline(heading.groupValues[2], codeBackground)
                }
            }
            bullet != null -> {
                append(bullet.groupValues[1])
                append("•  ")
                appendInline(bullet.groupValues[2], codeBackground)
            }
            else -> appendInline(raw, codeBackground)
        }
    }
}

private fun AnnotatedString.Builder.appendInline(line: String, codeBackground: Color) {
    var last = 0
    INLINE.findAll(line).forEach { m ->
        append(line.substring(last, m.range.first))
        val (code, bold, bold2, italic) = m.destructured
        when {
            code.isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) { append(code) }
            bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(bold, codeBackground) }
            bold2.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(bold2, codeBackground) }
            else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
        }
        last = m.range.last + 1
    }
    append(line.substring(last))
}
