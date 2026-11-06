package dev.harness.pocket

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.harness.pocket.data.ApiException
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.AskAnswer
import dev.harness.pocket.data.Attachment
import dev.harness.pocket.data.AuthException
import dev.harness.pocket.data.ChatState
import dev.harness.pocket.data.ConnectionInput
import dev.harness.pocket.data.EventParser
import dev.harness.pocket.data.HarnessApi
import dev.harness.pocket.data.ModelChoice
import dev.harness.pocket.data.Prefs
import dev.harness.pocket.data.PromptPart
import dev.harness.pocket.data.SessionSummary
import dev.harness.pocket.data.UserMessage
import dev.harness.pocket.data.WorkspaceInfo
import dev.harness.pocket.data.WorkspaceSnapshot
import dev.harness.pocket.data.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val api = HarnessApi()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var pageSeq = -1
    private var lastModelChoice: ModelChoice? = null

    init {
        val saved = prefs.loadConnection()
        if (saved != null) {
            api.setConnection(saved)
            _state.value = AppState(
                restoring = false,
                savedBase = saved.baseUrl,
                connection = saved,
                workspaces = prefs.loadWorkspaces()
            )
            loadSessions()
            loadCatalogs()
        } else {
            _state.value = AppState(
                restoring = false,
                savedBase = prefs.lastBase()
            )
        }
    }

    fun connect(server: String, token: String) {
        if (_state.value.connecting) return
        _state.update { it.copy(connecting = true, connectError = null) }
        viewModelScope.launch {
            try {
                val parsed = ConnectionInput.parse(server, token)
                val connection = api.connect(parsed.baseUrl, parsed.token)
                prefs.saveConnection(connection)
                _state.update {
                    it.copy(
                        connection = connection,
                        savedBase = connection.baseUrl,
                        connecting = false,
                        connectError = null,
                        sessions = emptyList(),
                        sessionsError = null
                    )
                }
                loadSessions()
                loadCatalogs()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(connecting = false, connectError = friendlyError(e))
                }
            }
        }
    }

    fun loadSessions() {
        if (_state.value.connection == null) return
        viewModelScope.launch {
            _state.update { it.copy(sessionsLoading = true, sessionsError = null) }
            try {
                val sessions = api.listSessions().sortedByDescending { it.updatedAt }
                _state.update { it.copy(sessions = sessions, sessionsLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(sessionsLoading = false, sessionsError = friendlyError(e))
                }
            }
        }
    }

    fun refreshSessions() = loadSessions()

    fun openSession(session: SessionSummary) {
        pageSeq = -1
        _state.update {
            it.copy(
                activeSession = session,
                chat = ChatState(
                    loading = true,
                    title = session.title,
                    running = session.running,
                    asOfSeq = session.asOfSeq,
                    model = session.model ?: lastModelChoice,
                    permission = session.permission,
                    questions = session.questions
                )
            )
        }
        startPolling(session.sessionId, reset = true)
    }

    fun newSession() {
        if (_state.value.creating) return
        val workspace = _state.value.selectedWorkspace
        _state.update { it.copy(creating = true, sessionsError = null) }
        viewModelScope.launch {
            try {
                val id = api.createSession(workspaceId = workspace?.workspaceId)
                val choice = lastModelChoice
                if (choice != null) {
                    runCatching {
                        api.selectModel(id, choice.provider, choice.model, choice.effort)
                    }
                }
                val summary = SessionSummary(
                    sessionId = id,
                    title = null,
                    updatedAt = System.currentTimeMillis(),
                    running = false,
                    cwd = workspace?.path,
                    asOfSeq = 0,
                    blank = true,
                    model = choice,
                    permission = _state.value.permissionCatalog?.defaultPreset
                )
                _state.update {
                    it.copy(
                        creating = false,
                        sessions = listOf(summary) + it.sessions.filterNot { s -> s.sessionId == id }
                    )
                }
                openSession(summary)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update { it.copy(creating = false) }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(creating = false, sessionsError = friendlyError(e))
                }
            }
        }
    }

    fun send(text: String) {
        val clean = text.trim()
        val current = _state.value
        val session = current.activeSession ?: return
        if (current.chat.sending || current.chat.running) return
        if (current.chat.attachments.any { it.uploading }) {
            _state.update { it.copy(chat = it.chat.copy(error = "文件还在上传，请稍候再发送")) }
            return
        }
        val attachments = current.chat.attachments.filter { it.ready }
        if (clean.isEmpty() && attachments.isEmpty()) return

        val requestId = UUID.randomUUID().toString()
        val parts = attachments.mapNotNull { attachment ->
            if (attachment.isImage) {
                val bytes = attachment.bytes ?: return@mapNotNull null
                PromptPart(
                    type = "image",
                    mediaType = attachment.mime ?: "image/png",
                    data = Base64.getEncoder().encodeToString(bytes),
                    name = attachment.name
                )
            } else {
                val receipt = attachment.receiptId ?: return@mapNotNull null
                PromptPart(type = "file", receiptId = receipt, name = attachment.name)
            }
        }
        val pending = UserMessage(
            key = "pending:$requestId",
            seq = Int.MAX_VALUE,
            text = clean,
            rpcId = requestId,
            pending = true,
            attachments = attachments.map { it.name }
        )
        _state.update {
            it.copy(chat = it.chat.copy(pending = pending, sending = true, error = null))
        }

        viewModelScope.launch {
            try {
                api.prompt(session.sessionId, clean, requestId, parts)
                TaskMonitorService.start(
                    getApplication(),
                    session.sessionId,
                    session.title?.takeIf { it.isNotBlank() } ?: "Harness 任务",
                    session.asOfSeq
                )
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            sending = false,
                            running = true,
                            attachments = emptyList()
                        )
                    )
                }
                startPolling(session.sessionId, reset = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update {
                    it.copy(chat = it.chat.copy(sending = false, pending = null))
                }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            sending = false,
                            pending = null,
                            error = friendlyError(e)
                        )
                    )
                }
            }
        }
    }

    fun loadCatalogs() {
        if (_state.value.connection == null) return
        viewModelScope.launch {
            _state.update { it.copy(modelsLoading = true) }
            try {
                val catalog = api.modelCatalog()
                lastModelChoice = lastModelChoice ?: catalog.defaultChoice
                _state.update { it.copy(modelCatalog = catalog, modelsLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(modelsLoading = false, sessionsError = friendlyError(e))
                }
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(permissionsLoading = true) }
            try {
                val catalog = api.permissionCatalog()
                _state.update {
                    it.copy(permissionCatalog = catalog, permissionsLoading = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(permissionsLoading = false, sessionsError = friendlyError(e))
                }
            }
        }
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        if (_state.value.connection == null) return
        viewModelScope.launch {
            _state.update { it.copy(workspacesLoading = true, workspacesError = null) }
            try {
                var snapshot: WorkspaceSnapshot? = null
                var lastError: Exception? = null
                for (attempt in 0 until 3) {
                    try {
                        val candidate = api.listWorkspaces()
                        if (candidate.workspaces.isNotEmpty()) {
                            snapshot = candidate
                            break
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                    }
                    if (attempt < 2) delay(350L * (attempt + 1))
                }
                val loaded = snapshot ?: throw (lastError ?: ApiException("工作区列表为空"))
                prefs.saveWorkspaces(loaded.workspaces)
                _state.update { st ->
                    val selected = st.selectedWorkspace
                        ?.let { old ->
                            loaded.workspaces.firstOrNull { it.workspaceId == old.workspaceId }
                        }
                    st.copy(
                        workspaces = loaded.workspaces,
                        workspacesLoading = false,
                        selectedWorkspace = selected,
                        pinnedSessionIds = loaded.pinnedSessionIds,
                        archivedSessionIds = loaded.archivedSessionIds
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(workspacesLoading = false, workspacesError = friendlyError(e))
                }
            }
        }
    }

    fun refreshWorkspaces() = loadWorkspaces()

    fun moveWorkspace(workspaceId: String, beforeWorkspaceId: String?) {
        val current = _state.value.workspaces
        if (_state.value.workspacesLoading) return
        if (current.none { it.workspaceId == workspaceId }) return
        if (workspaceId == beforeWorkspaceId) return
        val optimistic = reorderWorkspaces(current, workspaceId, beforeWorkspaceId)
        _state.update {
            it.copy(workspaces = optimistic, workspacesLoading = true, workspacesError = null)
        }
        viewModelScope.launch {
            try {
                val ids = api.moveWorkspace(workspaceId, beforeWorkspaceId)
                val byId = current.associateBy { it.workspaceId }
                val ordered = ids.mapNotNull { byId[it] }
                val extras = optimistic.filterNot { workspace -> ids.contains(workspace.workspaceId) }
                _state.update {
                    it.copy(
                        workspaces = ordered + extras,
                        workspacesLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update { it.copy(workspaces = current, workspacesLoading = false) }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        workspaces = current,
                        workspacesLoading = false,
                        workspacesError = friendlyError(e)
                    )
                }
            }
        }
    }

    private fun reorderWorkspaces(
        current: List<WorkspaceInfo>,
        workspaceId: String,
        beforeWorkspaceId: String?
    ): List<WorkspaceInfo> {
        val moving = current.firstOrNull { it.workspaceId == workspaceId } ?: return current
        val rest = current.filterNot { it.workspaceId == workspaceId }.toMutableList()
        if (beforeWorkspaceId == null) {
            rest.add(moving)
        } else {
            val index = rest.indexOfFirst { it.workspaceId == beforeWorkspaceId }
            if (index < 0) rest.add(moving) else rest.add(index, moving)
        }
        return rest
    }

    fun togglePin(session: SessionSummary) {
        val id = session.sessionId
        val wasPinned = _state.value.isPinned(id)
        viewModelScope.launch {
            try {
                val ids = if (wasPinned) api.unpinSession(id) else api.pinSession(id)
                _state.update { it.copy(pinnedSessionIds = ids) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update { it.copy(sessionsError = friendlyError(e)) }
            }
        }
    }

    fun archiveSession(session: SessionSummary) {
        val id = session.sessionId
        viewModelScope.launch {
            try {
                val ids = api.archiveSession(id, stopActivity = session.running)
                val wasActive = _state.value.activeSession?.sessionId == id
                _state.update {
                    it.copy(
                        archivedSessionIds = ids,
                        pinnedSessionIds = it.pinnedSessionIds.filterNot { pin -> pin == id }
                    )
                }
                if (wasActive) closeSession() else loadSessions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update { it.copy(sessionsError = friendlyError(e)) }
            }
        }
    }

    fun unarchiveSession(session: SessionSummary) {
        viewModelScope.launch {
            try {
                val ids = api.unarchiveSession(session.sessionId)
                _state.update { it.copy(archivedSessionIds = ids) }
                loadSessions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update { it.copy(sessionsError = friendlyError(e)) }
            }
        }
    }

    fun selectWorkspace(workspace: WorkspaceInfo?) {
        _state.update { it.copy(selectedWorkspace = workspace) }
    }

    fun createWorkspace(path: String) {
        val clean = path.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(workspacesLoading = true, workspacesError = null) }
            try {
                val workspace = api.createWorkspace(clean)
                _state.update { st ->
                    st.copy(
                        workspaces = (listOf(workspace) + st.workspaces)
                            .distinctBy { it.workspaceId },
                        workspacesLoading = false,
                        selectedWorkspace = workspace
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(workspacesLoading = false, workspacesError = friendlyError(e))
                }
            }
        }
    }

    fun selectModel(choice: ModelChoice) {
        val session = _state.value.activeSession ?: return
        if (_state.value.chat.modelBusy) return
        _state.update { it.copy(chat = it.chat.copy(modelBusy = true, error = null)) }
        viewModelScope.launch {
            try {
                val selected = api.selectModel(
                    session.sessionId,
                    choice.provider,
                    choice.model,
                    choice.effort
                )
                lastModelChoice = selected
                _state.update { st ->
                    st.copy(
                        activeSession = st.activeSession?.copy(model = selected),
                        sessions = st.sessions.map {
                            if (it.sessionId == session.sessionId) it.copy(model = selected) else it
                        },
                        chat = st.chat.copy(model = selected, modelBusy = false)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update { it.copy(chat = it.chat.copy(modelBusy = false)) }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(chat = it.chat.copy(modelBusy = false, error = friendlyError(e)))
                }
            }
        }
    }

    fun selectPermission(value: String) {
        val session = _state.value.activeSession ?: return
        val chat = _state.value.chat
        if (chat.permissionBusy) return
        val previous = chat.permission
        _state.update {
            it.copy(
                chat = it.chat.copy(
                    permissionBusy = true,
                    error = null
                )
            )
        }
        viewModelScope.launch {
            try {
                api.executeCommand(session.sessionId, "/permission $value")
                _state.update { st ->
                    st.copy(
                        activeSession = st.activeSession?.copy(permission = value),
                        sessions = st.sessions.map {
                            if (it.sessionId == session.sessionId) it.copy(permission = value) else it
                        },
                        chat = st.chat.copy(permission = value, permissionBusy = false)
                    )
                }
                loadSessions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update {
                    it.copy(chat = it.chat.copy(permissionBusy = false, permission = previous))
                }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            permissionBusy = false,
                            permission = previous,
                            error = friendlyError(e)
                        )
                    )
                }
            }
        }
    }

    fun answerQuestions(callId: String, answers: List<AskAnswer>) {
        val session = _state.value.activeSession ?: return
        if (_state.value.chat.questionBusy) return
        _state.update { it.copy(chat = it.chat.copy(questionBusy = true, error = null)) }
        viewModelScope.launch {
            try {
                val accepted = api.answerQuestions(session.sessionId, callId, answers)
                if (!accepted) {
                    throw ApiException("这个问题已经结束，无法再提交")
                }
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            questionBusy = false,
                            questions = it.chat.questions.filterNot { batch -> batch.callId == callId }
                        )
                    )
                }
                loadSessions()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update { it.copy(chat = it.chat.copy(questionBusy = false)) }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(chat = it.chat.copy(questionBusy = false, error = friendlyError(e)))
                }
            }
        }
    }

    fun attachFile(uri: Uri) {
        val session = _state.value.activeSession
        if (session == null) {
            _state.update { it.copy(chat = it.chat.copy(error = "请先打开一个会话")) }
            return
        }
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { readFile(uri) }
                if (file.bytes.size > 20 * 1024 * 1024) {
                    throw IllegalArgumentException("文件不能超过 20 MB")
                }
                val mime = file.mime?.lowercase()
                val isImage = mime in setOf(
                    "image/png", "image/jpeg", "image/webp", "image/gif"
                )
                val attachment = Attachment(
                    id = UUID.randomUUID().toString(),
                    name = file.name,
                    size = file.bytes.size.toLong(),
                    mime = file.mime,
                    isImage = isImage,
                    bytes = if (isImage) file.bytes else null,
                    uploading = !isImage
                )
                _state.update {
                    it.copy(chat = it.chat.copy(attachments = it.chat.attachments + attachment))
                }
                if (!isImage) {
                    try {
                        val receipt = api.uploadFile(session.sessionId, file.bytes, file.name)
                        updateAttachment(attachment.id) {
                            it.copy(receiptId = receipt.receiptId, uploading = false)
                        }
                    } catch (e: Exception) {
                        updateAttachment(attachment.id) {
                            it.copy(uploading = false, error = friendlyError(e))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(chat = it.chat.copy(error = friendlyError(e)))
                }
            }
        }
    }

    fun removeAttachment(id: String) {
        _state.update { st ->
            st.copy(chat = st.chat.copy(attachments = st.chat.attachments.filterNot { it.id == id }))
        }
    }

    private fun updateAttachment(id: String, transform: (Attachment) -> Attachment) {
        _state.update { st ->
            st.copy(
                chat = st.chat.copy(
                    attachments = st.chat.attachments.map {
                        if (it.id == id) transform(it) else it
                    }
                )
            )
        }
    }

    private fun readFile(uri: Uri): ReadFile {
        val resolver = getApplication<Application>().contentResolver
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "附件"
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val value = cursor.getString(index)
                    if (!value.isNullOrBlank()) name = value
                }
            }
        }
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("无法读取这个文件")
        return ReadFile(name, resolver.getType(uri), bytes)
    }

    private data class ReadFile(
        val name: String,
        val mime: String?,
        val bytes: ByteArray
    )

    fun stop() {
        val session = _state.value.activeSession ?: return
        viewModelScope.launch {
            try {
                api.cancel(session.sessionId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The turn may already have finished; the next poll will reconcile state.
            }
        }
    }

    fun rename(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val session = _state.value.activeSession ?: return
        viewModelScope.launch {
            try {
                api.rename(session.sessionId, clean)
                _state.update { st ->
                    st.copy(
                        sessions = st.sessions.map {
                            if (it.sessionId == session.sessionId) it.copy(title = clean) else it
                        },
                        activeSession = st.activeSession?.copy(title = clean),
                        chat = st.chat.copy(title = clean)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(chat = it.chat.copy(error = friendlyError(e)))
                }
            }
        }
    }

    fun refreshChat() {
        val session = _state.value.activeSession ?: return
        startPolling(session.sessionId, reset = true)
    }

    fun closeSession() {
        pollJob?.cancel()
        pollJob = null
        pageSeq = -1
        _state.update {
            it.copy(activeSession = null, chat = ChatState(loading = false))
        }
        loadSessions()
    }

    fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        pageSeq = -1
        prefs.clearConnection()
        api.setConnection(null)
        _state.value = AppState(
            restoring = false,
            savedBase = _state.value.savedBase
        )
    }

    fun dismissConnectError() {
        _state.update { it.copy(connectError = null) }
    }

    private fun handleAuthFailure(message: String) {
        pollJob?.cancel()
        pollJob = null
        prefs.clearConnection()
        api.setConnection(null)
        _state.update {
            AppState(
                restoring = false,
                savedBase = it.savedBase,
                connectError = message
            )
        }
    }

    private fun startPolling(sessionId: String, reset: Boolean) {
        pollJob?.cancel()
        if (reset) pageSeq = -1
        pollJob = viewModelScope.launch {
            var first = reset
            while (isActive && _state.value.activeSession?.sessionId == sessionId) {
                var wait = 2500L
                try {
                    val sessions = api.listSessions().sortedByDescending { it.updatedAt }
                    val row = sessions.firstOrNull { it.sessionId == sessionId }
                    if (row != null) {
                        _state.update { st ->
                            st.copy(
                                sessions = sessions,
                                activeSession = row,
                                chat = st.chat.copy(
                                    title = st.chat.title ?: row.title,
                                    running = row.running,
                                    questions = row.questions
                                )
                            )
                        }
                        if (first || row.asOfSeq != pageSeq) {
                            val page = api.page(sessionId, row.asOfSeq)
                            val pendingAttachments = _state.value.chat.pending
                                ?.attachments
                                .orEmpty()
                            val pendingRpcId = _state.value.chat.pending?.rpcId
                            val parsed = EventParser.parse(page.records).map { item ->
                                if (item is UserMessage &&
                                    item.rpcId != null &&
                                    item.rpcId == pendingRpcId &&
                                    item.attachments.isEmpty() &&
                                    pendingAttachments.isNotEmpty()
                                ) {
                                    item.copy(attachments = pendingAttachments)
                                } else {
                                    item
                                }
                            }
                            val oldPending = _state.value.chat.pending
                            val stillPending = oldPending?.takeIf { pending ->
                                parsed.none { item ->
                                    item is UserMessage && item.rpcId == pending.rpcId
                                }
                            }
                            _state.update { st ->
                                st.copy(
                                    chat = st.chat.copy(
                                        loading = false,
                                        items = parsed,
                                        pending = stillPending,
                                        running = row.running,
                                        title = st.chat.title ?: row.title,
                                        error = null,
                                        asOfSeq = row.asOfSeq
                                    )
                                )
                            }
                            pageSeq = row.asOfSeq
                        }
                        wait = if (row.running) 750L else 2500L
                    }
                    first = false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: AuthException) {
                    handleAuthFailure(e.message ?: "登录已过期，请重新连接")
                    return@launch
                } catch (e: Exception) {
                    _state.update {
                        it.copy(chat = it.chat.copy(loading = false, error = friendlyError(e)))
                    }
                    wait = 4000L
                }
                delay(wait)
            }
        }
    }
}
