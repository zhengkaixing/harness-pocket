package dev.harness.pocket.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val text: String, val level: Int) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String, val language: String?) : MdBlock
    object Divider : MdBlock
}

object MarkdownParser {
    private val ordered = Regex("^\\d+[.)]\\s+")

    fun parse(source: String): List<MdBlock> {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val out = ArrayList<MdBlock>()
        val paragraph = StringBuilder()
        var index = 0

        fun flushParagraph() {
            val text = paragraph.toString().trim()
            if (text.isNotEmpty()) out.add(MdBlock.Paragraph(text))
            paragraph.setLength(0)
        }

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            when {
                trimmed.startsWith("```") -> {
                    flushParagraph()
                    val language = trimmed.removePrefix("```").trim().ifBlank { null }
                    index++
                    val code = StringBuilder()
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        code.append(lines[index]).append('\n')
                        index++
                    }
                    if (index < lines.size) index++
                    out.add(MdBlock.Code(code.toString().trimEnd(), language))
                }

                trimmed.isEmpty() -> {
                    flushParagraph()
                    index++
                }

                trimmed == "---" || trimmed == "***" -> {
                    flushParagraph()
                    out.add(MdBlock.Divider)
                    index++
                }

                trimmed.startsWith("#") -> {
                    flushParagraph()
                    val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                    out.add(MdBlock.Heading(trimmed.drop(level).trim(), level))
                    index++
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    flushParagraph()
                    out.add(MdBlock.Bullet(trimmed.substring(2).trim()))
                    index++
                }

                ordered.containsMatchIn(trimmed) -> {
                    flushParagraph()
                    out.add(MdBlock.Bullet(ordered.replaceFirst(trimmed, "")))
                    index++
                }

                trimmed.startsWith("> ") -> {
                    flushParagraph()
                    out.add(MdBlock.Quote(trimmed.substring(2).trim()))
                    index++
                }

                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append('\n')
                    paragraph.append(line)
                    index++
                }
            }
        }
        flushParagraph()
        return out
    }
}

@Composable
fun inlineMarkdown(text: String): AnnotatedString = remember(text) {
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }

                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0xFFF0F2F7),
                                color = Color(0xFF3E4B66)
                            )
                        )
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append('`')
                        i++
                    }
                }

                text[i] == '[' -> {
                    val close = text.indexOf("](", i)
                    val end = if (close > i) text.indexOf(')', close + 2) else -1
                    if (close > i && end > close) {
                        pushStyle(SpanStyle(color = Blue))
                        append(text.substring(i + 1, close))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }

                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
