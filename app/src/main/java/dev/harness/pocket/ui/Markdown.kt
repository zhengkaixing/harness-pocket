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
    data class Bullet(val text: String, val depth: Int = 0) : MdBlock
    data class Ordered(val number: String, val text: String, val depth: Int = 0) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String, val language: String?) : MdBlock
    data class Image(val alt: String, val source: String) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    object Divider : MdBlock
}

object MarkdownParser {
    private val orderedLine = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
    private val bulletLine = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val imageLine = Regex("^!\\[([^\\]]*)]\\(([^)]+)\\)\\s*$")
    private val separatorCell = Regex("^:?-{2,}:?$")

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

                imageLine.matches(trimmed) -> {
                    flushParagraph()
                    val match = imageLine.matchEntire(trimmed)!!
                    out.add(
                        MdBlock.Image(
                            alt = match.groupValues[1].trim(),
                            source = match.groupValues[2].trim().trim('<', '>')
                        )
                    )
                    index++
                }

                isTableStart(lines, index) -> {
                    flushParagraph()
                    val header = splitRow(lines[index])
                    index += 2
                    val rows = ArrayList<List<String>>()
                    while (index < lines.size) {
                        val row = lines[index].trim()
                        if (row.isEmpty() || !row.contains('|')) break
                        if (row.startsWith("```")) break
                        rows.add(splitRow(row))
                        index++
                    }
                    out.add(MdBlock.Table(header, rows))
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

                bulletLine.matches(line) -> {
                    flushParagraph()
                    val match = bulletLine.matchEntire(line)!!
                    out.add(
                        MdBlock.Bullet(
                            text = match.groupValues[2].trim(),
                            depth = depthOf(match.groupValues[1])
                        )
                    )
                    index++
                }

                orderedLine.matches(line) -> {
                    flushParagraph()
                    val match = orderedLine.matchEntire(line)!!
                    out.add(
                        MdBlock.Ordered(
                            number = match.groupValues[2],
                            text = match.groupValues[3].trim(),
                            depth = depthOf(match.groupValues[1])
                        )
                    )
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

    private fun depthOf(indent: String): Int =
        (indent.replace("\t", "  ").length / 2).coerceIn(0, 4)

    private fun isTableStart(lines: List<String>, index: Int): Boolean {
        val header = lines[index].trim()
        if (!header.contains('|') || header.startsWith("```")) return false
        val separator = lines.getOrNull(index + 1)?.trim() ?: return false
        if (!separator.contains('-') || !separator.contains('|')) return false
        val headerCells = splitRow(header)
        val separatorCells = splitRow(separator)
        return headerCells.size >= 2 &&
            separatorCells.size == headerCells.size &&
            separatorCells.all { separatorCell.matches(it) }
    }

    private fun splitRow(line: String): List<String> {
        var text = line.trim()
        if (text.startsWith("|")) text = text.substring(1)
        if (text.endsWith("|")) text = text.dropLast(1)
        val cells = ArrayList<String>()
        val cell = StringBuilder()
        var escaped = false
        for (ch in text) {
            when {
                escaped -> {
                    cell.append(ch)
                    escaped = false
                }
                ch == '\\' -> escaped = true
                ch == '|' -> {
                    cells.add(cell.toString().trim())
                    cell.setLength(0)
                }
                else -> cell.append(ch)
            }
        }
        cells.add(cell.toString().trim())
        return cells
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
