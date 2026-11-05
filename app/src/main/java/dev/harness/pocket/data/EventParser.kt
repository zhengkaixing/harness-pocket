package dev.harness.pocket.data

import org.json.JSONArray
import org.json.JSONObject

object EventParser {

    fun parse(records: JSONArray): List<ChatItem> {
        val out = ArrayList<ChatItem>()
        val toolIndex = HashMap<String, Int>()

        for (i in 0 until records.length()) {
            val record = records.optJSONObject(i) ?: continue
            if (record.optString("type") != "event") continue
            val event = record.optJSONObject("event") ?: continue
            val type = event.optString("type")
            val seq = event.optInt("seq", i)
            val data = event.optJSONObject("data") ?: JSONObject()

            when (type) {
                "user/message" -> {
                    val message = data.optJSONObject("message") ?: data
                    val source = message.optJSONObject("source")
                    if (source?.optString("kind") != "user") continue
                    val content = message.optJSONArray("content")
                    val text = plainText(content)
                    val attachments = attachmentLabels(content)
                    if (text.isBlank() && attachments.isEmpty()) continue
                    if (text.trimStart().startsWith("/permission ")) continue
                    out.add(
                        UserMessage(
                            key = "user:$seq",
                            seq = seq,
                            text = text,
                            rpcId = source.optString("rpcId").takeIf { it.isNotBlank() },
                            attachments = attachments
                        )
                    )
                }

                "assistant/message" -> {
                    val message = data.optJSONObject("message") ?: continue
                    val content = message.optJSONArray("content") ?: continue
                    val textBuffer = StringBuilder()

                    fun flushText() {
                        val text = textBuffer.toString().trim()
                        if (text.isNotBlank()) {
                            out.add(
                                AssistantMessage(
                                    key = "assistant:$seq:${out.size}",
                                    seq = seq,
                                    text = text
                                )
                            )
                        }
                        textBuffer.setLength(0)
                    }

                    for (j in 0 until content.length()) {
                        val block = content.optJSONObject(j) ?: continue
                        when (block.optString("type")) {
                            "text" -> textBuffer.append(block.optString("text"))
                            "image" -> {
                                if (textBuffer.isNotEmpty()) textBuffer.append('\n')
                                textBuffer.append("[图片]")
                            }
                            "file" -> {
                                if (textBuffer.isNotEmpty()) textBuffer.append('\n')
                                textBuffer.append("[文件]")
                            }
                            "tool-call" -> {
                                flushText()
                                val callId = block.optString("id")
                                    .ifBlank { "tool-$seq-${out.size}" }
                                val name = block.optString("name").ifBlank { "tool" }
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
                    out[index] = old.copy(
                        result = blocksText(message.optJSONArray("content")),
                        isError = message.optBoolean("isError", false),
                        done = true
                    )
                }

                "turn/end" -> {
                    val reason = data.optJSONObject("reason") ?: continue
                    if (reason.optString("kind") == "error") {
                        val error = reason.optJSONObject("error")
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
            when (block.optString("type")) {
                "image" -> result.add(
                    block.optString("name").takeIf { it.isNotBlank() } ?: "图片"
                )
                "file" -> result.add(
                    block.optString("name").takeIf { it.isNotBlank() }
                        ?: block.optString("fileName").takeIf { it.isNotBlank() }
                        ?: "附件"
                )
            }
        }
        return result
    }

    private fun blocksText(blocks: JSONArray?): String {
        if (blocks == null) return ""
        val builder = StringBuilder()
        for (i in 0 until blocks.length()) {
            val block = blocks.optJSONObject(i) ?: continue
            when (block.optString("type")) {
                "text" -> builder.append(block.optString("text"))
                "image" -> {
                    if (builder.isNotEmpty()) builder.append('\n')
                    builder.append("[图片]")
                }
                "file" -> {
                    if (builder.isNotEmpty()) builder.append('\n')
                    builder.append("[文件]")
                }
            }
        }
        return builder.toString().trim()
    }
}
