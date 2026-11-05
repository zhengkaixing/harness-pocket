package dev.harness.pocket.data

data class Connection(
    val baseUrl: String,
    val token: String,
    val cookie: String
)

data class ModelEffort(
    val id: String,
    val name: String,
    val description: String? = null
)

data class AiModel(
    val id: String,
    val name: String,
    val description: String? = null,
    val efforts: List<ModelEffort> = emptyList(),
    val defaultEffort: String? = null
)

data class ModelGroup(
    val id: String,
    val name: String,
    val models: List<AiModel>
)

data class ModelChoice(
    val provider: String,
    val model: String,
    val effort: String? = null
)

data class ModelCatalog(
    val defaultChoice: ModelChoice? = null,
    val groups: List<ModelGroup> = emptyList(),
    val failures: List<String> = emptyList()
) {
    fun find(provider: String, model: String): AiModel? =
        groups.firstOrNull { it.id == provider }?.models?.firstOrNull { it.id == model }

    fun displayName(choice: ModelChoice?): String {
        if (choice == null) return "默认模型"
        val found = find(choice.provider, choice.model)
        val modelName = found?.name ?: choice.model
        val effortName = found?.efforts?.firstOrNull { it.id == choice.effort }?.name
        return if (effortName.isNullOrBlank()) modelName else "$modelName · $effortName"
    }
}

data class PermissionOption(
    val value: String,
    val name: String
)

data class PermissionCatalog(
    val options: List<PermissionOption> = emptyList(),
    val defaultPreset: String? = null
) {
    fun label(value: String?): String = when (value) {
        "read-only" -> "仅可查看"
        "workspace-write" -> "工作区内修改"
        "danger-full-access" -> "完全权限"
        else -> options.firstOrNull { it.value == value }?.name ?: "默认权限"
    }

    fun description(value: String?): String = when (value) {
        "read-only" -> "可以读取文件，但不能修改"
        "workspace-write" -> "只能在当前工作区内修改"
        "danger-full-access" -> "可以修改任意文件并执行命令"
        else -> "跟随 Harness 的默认设置"
    }
}

data class WorkspaceInfo(
    val workspaceId: String,
    val path: String,
    val title: String
)

data class WorkspaceSnapshot(
    val workspaces: List<WorkspaceInfo> = emptyList(),
    val pinnedSessionIds: List<String> = emptyList(),
    val archivedSessionIds: List<String> = emptyList()
)

data class SessionSummary(
    val sessionId: String,
    val title: String?,
    val updatedAt: Long,
    val running: Boolean,
    val cwd: String?,
    val asOfSeq: Int,
    val blank: Boolean,
    val model: ModelChoice? = null,
    val permission: String? = null
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
    val pending: Boolean = false,
    val attachments: List<String> = emptyList()
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

data class Attachment(
    val id: String,
    val name: String,
    val size: Long,
    val mime: String?,
    val isImage: Boolean,
    val bytes: ByteArray? = null,
    val receiptId: String? = null,
    val uploading: Boolean = false,
    val error: String? = null
) {
    val ready: Boolean
        get() = error == null && !uploading && (receiptId != null || (isImage && bytes != null))
}

data class UploadReceipt(
    val receiptId: String,
    val name: String,
    val bytes: Long
)

data class PromptPart(
    val type: String,
    val text: String? = null,
    val mediaType: String? = null,
    val data: String? = null,
    val receiptId: String? = null,
    val name: String? = null
)

data class ChatState(
    val loading: Boolean = true,
    val items: List<ChatItem> = emptyList(),
    val pending: UserMessage? = null,
    val running: Boolean = false,
    val sending: Boolean = false,
    val title: String? = null,
    val error: String? = null,
    val asOfSeq: Int = 0,
    val model: ModelChoice? = null,
    val permission: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val modelBusy: Boolean = false,
    val permissionBusy: Boolean = false
) {
    val allItems: List<ChatItem>
        get() = if (pending != null) items + pending else items
}

data class AppState(
    val restoring: Boolean = true,
    val savedBase: String = "http://127.0.0.1:8080",
    val connection: Connection? = null,
    val connecting: Boolean = false,
    val connectError: String? = null,
    val sessions: List<SessionSummary> = emptyList(),
    val sessionsLoading: Boolean = false,
    val sessionsError: String? = null,
    val creating: Boolean = false,
    val activeSession: SessionSummary? = null,
    val chat: ChatState = ChatState(),
    val modelCatalog: ModelCatalog? = null,
    val modelsLoading: Boolean = false,
    val permissionCatalog: PermissionCatalog? = null,
    val permissionsLoading: Boolean = false,
    val workspaces: List<WorkspaceInfo> = emptyList(),
    val workspacesLoading: Boolean = false,
    val workspacesError: String? = null,
    val selectedWorkspace: WorkspaceInfo? = null,
    val pinnedSessionIds: List<String> = emptyList(),
    val archivedSessionIds: List<String> = emptyList()
) {
    fun isPinned(sessionId: String): Boolean = pinnedSessionIds.contains(sessionId)
    fun isArchived(sessionId: String): Boolean = archivedSessionIds.contains(sessionId)
}
