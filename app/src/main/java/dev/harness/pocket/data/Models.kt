package dev.harness.pocket.data

data class Connection(
    val baseUrl: String,
    val token: String,
    val cookie: String
)

data class SessionSummary(
    val sessionId: String,
    val title: String?,
    val updatedAt: Long,
    val running: Boolean,
    val cwd: String?,
    val asOfSeq: Int,
    val blank: Boolean
)

sealed interface ChatItem {
    val key: String
    val seq: Int
}

data class UserMessage(
    override val key: String,
    override val seq: Int,
    val text: String,
    val rpcId: String? = null,
    val pending: Boolean = false
) : ChatItem

data class AssistantMessage(
    override val key: String,
    override val seq: Int,
    val text: String
) : ChatItem

data class ToolItem(
    override val key: String,
    override val seq: Int,
    val name: String,
    val input: String,
    val result: String?,
    val isError: Boolean,
    val done: Boolean
) : ChatItem

data class NoticeItem(
    override val key: String,
    override val seq: Int,
    val text: String,
    val isError: Boolean
) : ChatItem

data class PageResult(
    val records: org.json.JSONArray,
    val hasMore: Boolean
)

data class ChatState(
    val loading: Boolean = true,
    val items: List<ChatItem> = emptyList(),
    val pending: UserMessage? = null,
    val running: Boolean = false,
    val sending: Boolean = false,
    val title: String? = null,
    val error: String? = null,
    val asOfSeq: Int = 0
) {
    val allItems: List<ChatItem>
        get() = if (pending != null) items + pending else items
}

data class AppState(
    val restoring: Boolean = true,
    val savedBase: String = "http://8.130.48.9:18080",
    val connection: Connection? = null,
    val connecting: Boolean = false,
    val connectError: String? = null,
    val sessions: List<SessionSummary> = emptyList(),
    val sessionsLoading: Boolean = false,
    val sessionsError: String? = null,
    val creating: Boolean = false,
    val activeSession: SessionSummary? = null,
    val chat: ChatState = ChatState()
)
