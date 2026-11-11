package dev.harness.pocket.data

import org.json.JSONArray
import org.json.JSONObject

object EventParser {

    fun parse(records: JSONArray): List<ChatItem> {
        val out = ArrayList<ChatItem>()
        val toolIndex = HashMap<String, Int>()
        val turnStarts = HashMap<Int, Long>()
        val turnUsage = HashMap<Int, TokenUsage>()

        for (i in 0 until records.length()) {
            val record = records.optJSONObject(i) ?: continue
            if (record.optString("type") != "event") continue
            val event = record.optJSONObject("event") ?: continue
            val type = event.optString("type")
            val seq = event.optInt("seq", i)
            val data = event.optJSONObject("data") ?: JSONObject()

            when (type) {
                "turn/start" -> {
                    val turn = data.optInt("turn", 0)
                    val startedAt = event.optLong("time", 0L)
                    if (startedAt > 0) {
                        turnStarts[turn] = startedAt
                        out.add(TurnStart("turn-start:$seq", seq, turn, startedAt))
                    }
                }

                "user/message" -> {
                    val message = data.optJSONObject("message") ?: data
                    val source = message.optJSONObject("source")
                    if (source?.optString("kind") != "user") continue
                    val content = message.optJSONArray("content")
                    val text = plainText(content)
                    val attachments = attachmentLabels(content)
                    val images = imagesOf(content)
                    if (text.isBlank() && attachments.isEmpty() && images.isEmpty()) continue
                    if (text.trimStart().startsWith("/permission ")) continue
                    out.add(
                        UserMessage(
                            key = "user:$seq",
                            seq = seq,
                            text = text,
                            rpcId = source.optString("rpcId").takeIf { it.isNotBlank() },
                            attachments = attachments,
                            images = images
                        )
                    )
                }

                "assistant/message" -> {
                    val message = data.optJSONObject("message") ?: continue
                    val content = message.optJSONArray("content") ?: continue
                    val textBuffer = StringBuilder()
                    val images = ArrayList<ChatImage>()

                    fun flushText() {
                        val text = textBuffer.toString().trim()
                        if (text.isNotBlank() || images.isNotEmpty()) {
                            out.add(
                                AssistantMessage(
                                    key = "assistant:$seq:${out.size}",
                                    seq = seq,
                                    text = text,
                                    images = images.toList()
                                )
                            )
                        }
                        textBuffer.setLength(0)
                        images.clear()
                    }

                    for (j in 0 until content.length()) {
                        val block = content.optJSONObject(j) ?: continue
                        when (block.optString("type")) {
                            "text" -> textBuffer.append(block.optString("text"))
                            "image" -> imageBlock(block)?.let { images.add(it) }
                            "file" -> {
                                if (textBuffer.isNotEmpty()) textBuffer.append('\n')
                                textBuffer.append("[文件]")
                            }
                            "tool-call" -> {
                                flushText()
                                val callId = block.optString("id")
                                    .ifBlank { "tool-$seq-${out.size}" }
                                val name = block.optString("name").ifBlank { "tool" }
                                if (name == "ask_user_question") {
                                    // Rendered by the live question card instead.
                                    continue
                                }
                                val input = block.opt("arguments")?.toString().orEmpty()
                                toolIndex[callId] = out.size
                                out.add(
                                    ToolItem(
                                        key = "tool:$callId",
                                        seq = seq,
                                        name = name,
                                        input = input,
                                        result = null,
                                        isError = false,
                                        done = false
                                    )
                                )
                            }
                        }
                    }
                    flushText()
                    val turn = data.optInt("turn", 0)
                    val usage = data.optJSONObject("usage")?.let { parseUsage(it) }
                    if (usage != null) {
                        turnUsage[turn] = (turnUsage[turn] ?: TokenUsage()).plus(usage)
                    }
                }

                "tool/result" -> {
                    val message = data.optJSONObject("message") ?: continue
                    val source = message.optJSONObject("source")
                    val callId = source?.optString("callId")
                        ?.takeIf { it.isNotBlank() }
                        ?: message.optString("toolCallId").takeIf { it.isNotBlank() }
                        ?: continue
                    val index = toolIndex[callId] ?: continue
                    if (index !in out.indices) continue
                    val old = out[index] as? ToolItem ?: continue
                    val content = message.optJSONArray("content")
                    out[index] = old.copy(
                        result = blocksText(content),
                        isError = message.optBoolean("isError", false),
                        done = true,
                        images = imagesOf(content)
                    )
                }

                "turn/end" -> {
                    val reason = data.optJSONObject("reason")
                    val failed = reason?.optString("kind") == "error"
                    if (failed) {
                        val error = reason?.optJSONObject("error")
                        val message = friendly(error?.optString("message"))
                            .ifBlank { "本轮执行失败" }
                        out.add(
                            NoticeItem(
                                key = "error:$seq",
                                seq = seq,
                                text = message,
                                isError = true
                            )
                        )
                    }
                    val turn = data.optInt("turn", 0)
                    val started = turnStarts[turn]
                    val ended = event.optLong("time", 0L)
                    if (started != null && ended > started) {
                        out.add(
                            TurnTime(
                                key = "turn-time:$seq",
                                seq = seq,
                                turn = turn,
                                durationMs = ended - started,
                                usage = turnUsage[turn],
                                failed = failed
                            )
                        )
                    }
                }

                "step/end" -> {
                    val error = data.optJSONObject("error") ?: continue
                    val message = friendly(error.optString("message"))
                    if (message.isNotBlank()) {
                        out.add(NoticeItem("step-error:$seq", seq, message, true))
                    }
                }
            }
        }
        return out
    }

    private fun parseUsage(json: JSONObject): TokenUsage = TokenUsage(
        uncachedInputTokens = json.optLong("uncachedInputTokens", 0L),
        outputTokens = json.optLong("outputTokens", 0L),
        cacheReadTokens = json.optLong("cacheReadTokens", 0L),
        cacheWriteTokens = json.optLong("cacheWriteTokens", 0L)
    )

    private fun friendly(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val nested = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
            .find(raw)
            ?.groupValues
            ?.get(1)
        return (nested ?: raw)
            .replace("\\n", " ")
            .replace("\\\"", "\"")
            .trim()
    }

    private fun plainText(blocks: JSONArray?): String {
        if (blocks == null) return ""
        val builder = StringBuilder()
        for (i in 0 until blocks.length()) {
            val block = blocks.optJSONObject(i) ?: continue
            if (block.optString("type") == "text") builder.append(block.optString("text"))
        }
        return builder.toString().trim()
    }

    private fun attachmentLabels(blocks: JSONArray?): List<String> {
        if (blocks == null) return emptyList()
        val result = ArrayList<String>()
        for (i in 0 until blocks.length()) {
            val block = blocks.optJSONObject(i) ?: continue
            if (block.optString("type") == "file") {
                result.add(
                    block.optString("name").takeIf { it.isNotBlank() }
                        ?: block.optString("fileName").takeIf { it.isNotBlank() }
                        ?: "附件"
                )
            }
        }
        return result
    }

    private fun imagesOf(blocks: JSONArray?): List<ChatImage> {
        if (blocks == null) return emptyList()
        val result = ArrayList<ChatImage>()
        for (i in 0 until blocks.length()) {
            val block = blocks.optJSONObject(i) ?: continue
            if (block.optString("type") == "image") {
                imageBlock(block)?.let { result.add(it) }
            }
        }
        return result.distinctBy { it.attachmentId }
    }

    private fun imageBlock(block: JSONObject): ChatImage? {
        val attachment = block.optJSONObject("attachment")
        if (attachment != null) {
            val id = attachment.optString("attachmentId")
            if (id.isBlank()) return null
            return ChatImage(
                attachmentId = id,
                mediaType = attachment.optString("mediaType").ifBlank { "image/png" },
                width = attachment.optInt("width"),
                height = attachment.optInt("height"),
                name = attachment.optString("name").takeIf { it.isNotBlank() }
            )
        }
        val inline = block.optString("data")
        if (inline.isBlank()) return null
        return ChatImage(
            attachmentId = "inline:${inline.hashCode()}",
            mediaType = block.optString("mediaType").ifBlank { "image/png" },
            width = block.optInt("width"),
            height = block.optInt("height"),
            name = block.optString("name").takeIf { it.isNotBlank() },
            data = inline
        )
    }

    private fun blocksText(blocks: JSONArray?): String {
        if (blocks == null) return ""
        val builder = StringBuilder()
        for (i in 0 until blocks.length()) {
            val block = blocks.optJSONObject(i) ?: continue
            when (block.optString("type")) {
                "text" -> builder.append(block.optString("text"))
                "file" -> {
                    if (builder.isNotEmpty()) builder.append('\n')
                    builder.append("[文件]")
                }
            }
        }
        return builder.toString().trim()
    }
}
