package dev.harness.pocket

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.harness.pocket.data.ApiException
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.ApprovalRequest
import dev.harness.pocket.data.ApprovalUpdate
import dev.harness.pocket.data.AskAnswer
import dev.harness.pocket.data.AssistantMessage
import dev.harness.pocket.data.Attachment
import dev.harness.pocket.data.AuthException
import dev.harness.pocket.data.AutomationChange
import dev.harness.pocket.data.AutomationTask
import dev.harness.pocket.data.ChatDraft
import dev.harness.pocket.data.ChatImage
import dev.harness.pocket.data.ChatState
import dev.harness.pocket.data.Connection
import dev.harness.pocket.data.ComputerUseState
import dev.harness.pocket.data.ConnectionInput
import dev.harness.pocket.data.EventParser
import dev.harness.pocket.data.HarnessApi
import dev.harness.pocket.data.ImageMime
import dev.harness.pocket.data.ModelChoice
import dev.harness.pocket.data.Prefs
import dev.harness.pocket.data.PromptPart
import dev.harness.pocket.data.SessionLiveState
import dev.harness.pocket.data.SessionSummary
import dev.harness.pocket.data.ToolItem
import dev.harness.pocket.data.UserMessage
import dev.harness.pocket.data.WorkspaceInfo
import dev.harness.pocket.data.WorkspaceSnapshot
import dev.harness.pocket.data.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Base64
import java.util.LinkedHashMap
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val api = HarnessApi()
    private val imageCache = LinkedHashMap<String, ChatImage>()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var previewJob: Job? = null
    private var pageSeq = -1
    private var lastModelChoice: ModelChoice? = null
    private var approvalObserverStarted = false

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
            startApprovalObserver()
            refreshSavedConnection(saved)
            loadSessions()
            loadCatalogs()
        } else {
            _state.value = AppState(
                restoring = false,
                savedBase = prefs.lastBase()
            )
        }
    }

    /**
     * Older app versions connected with `interactions=none`, which cannot
     * receive tool approval requests. Re-run the handshake silently with the
     * stored token so an upgraded install picks up an answer-enabled cookie.
     */
    private fun refreshSavedConnection(saved: Connection) {
        viewModelScope.launch {
            try {
                val fresh = api.connect(saved.baseUrl, saved.token)
                prefs.saveConnection(fresh)
                _state.update { it.copy(connection = fresh, savedBase = fresh.baseUrl) }
                api.stopApprovalStream()
                api.startApprovalStream()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the existing cookie; normal requests may still work.
            }
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
                        sessionsError = null,
                        approvals = emptyList(),
                        approvalBusyId = null,
                        approvalError = null
                    )
                }
                startApprovalObserver()
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
                loadPromptPreviews(sessions)
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

    private fun startApprovalObserver() {
        if (!approvalObserverStarted) {
            approvalObserverStarted = true
            viewModelScope.launch {
                api.approvalUpdates.collect { update ->
                    when (update) {
                        is ApprovalUpdate.Asked -> _state.update { st ->
                            if (st.approvals.any { it.eventId == update.request.eventId }) {
                                st
                            } else {
                                st.copy(
                                    approvals = st.approvals + update.request,
                                    approvalError = null
                                )
                            }
                        }
                        is ApprovalUpdate.Finished -> _state.update { st ->
                            st.copy(
                                approvals = st.approvals.filterNot {
                                    it.eventId == update.eventId
                                },
                                approvalBusyId = st.approvalBusyId
                                    .takeUnless { it == update.eventId },
                                approvalError = null
                            )
                        }
                    }
                }
            }
        }
        api.startApprovalStream()
    }

    fun answerApproval(request: ApprovalRequest, allow: Boolean) {
        if (_state.value.approvalBusyId != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(approvalBusyId = request.eventId, approvalError = null)
            }
            try {
                api.answerApproval(request.eventId, allow)
                _state.update { st ->
                    st.copy(
                        approvals = st.approvals.filterNot {
                            it.eventId == request.eventId
                        },
                        approvalBusyId = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        approvalBusyId = null,
                        approvalError = friendlyError(e)
                    )
                }
            }
        }
    }

    private fun loadPromptPreviews(sessions: List<SessionSummary>) {
        previewJob?.cancel()
        val missing = sessions.asSequence()
            .filter { !it.blank && it.sessionId.isNotBlank() }
            .filter { it.sessionId !in _state.value.lastPrompts }
            .take(20)
            .toList()
        if (missing.isEmpty()) return
        previewJob = viewModelScope.launch {
            for (chunk in missing.chunked(3)) {
                val fetched = chunk.map { session ->
                    async {
                        try {
                            Triple(
                                session.sessionId,
                                api.latestPrompt(session).orEmpty(),
                                true
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Triple(session.sessionId, "", false)
                        }
                    }
                }.awaitAll()
                _state.update { st ->
                    val next = st.lastPrompts.toMutableMap()
                    fetched.forEach { (id, text, ok) ->
                        if (ok) next[id] = text
                    }
                    st.copy(lastPrompts = next)
                }
            }
        }
    }

    private fun normalizePreview(text: String?): String? = text
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.take(120)

    fun setDraft(text: String) {
        if (text.length > 12_000) return
        val sessionId = _state.value.activeSession?.sessionId ?: return
        _state.update { st ->
            val draft = st.drafts[sessionId] ?: ChatDraft()
            st.copy(drafts = st.drafts + (sessionId to draft.copy(text = text)))
        }
    }

    private fun updateDraftAttachment(
        sessionId: String,
        id: String,
        transform: (Attachment) -> Attachment
    ) {
        _state.update { st ->
            val draft = st.drafts[sessionId] ?: return@update st
            val attachments = draft.attachments.map {
                if (it.id == id) transform(it) else it
            }
            val nextDraft = draft.copy(attachments = attachments)
            st.copy(
                drafts = st.drafts + (sessionId to nextDraft),
                chat = if (st.activeSession?.sessionId == sessionId) {
                    st.chat.copy(attachments = attachments)
                } else {
                    st.chat
                }
            )
        }
    }

    fun openSession(session: SessionSummary) {
        pageSeq = -1
        _state.update {
            val draft = it.drafts[session.sessionId] ?: ChatDraft()
            it.copy(
                activeSession = session,
                automationsVisible = false,
                chat = ChatState(
                    loading = true,
                    title = session.title,
                    running = session.running,
                    asOfSeq = session.asOfSeq,
                    model = session.model ?: lastModelChoice,
                    permission = session.permission,
                    questions = session.questions,
                    attachments = draft.attachments,
                    usage = session.usage,
                    lastPromptAt = session.lastPromptAt
                )
            )
        }
        startPolling(session.sessionId, reset = true)
        refreshComputerUse()
        viewModelScope.launch {
            val live = loadLiveState(session.sessionId)
            if (live != null && _state.value.activeSession?.sessionId == session.sessionId) {
                _state.update { st ->
                    st.copy(
                        chat = st.chat.copy(
                            questions = if (live.questions.isNotEmpty()) {
                                live.questions
                            } else {
                                st.chat.questions
                            },
                            queued = mergeQueue(live.inbox, st.chat.queued),
                            context = live.context ?: st.chat.context
                        )
                    )
                }
            }
        }
    }

    fun newSession() = createSession(null)

    fun newSessionInWorkspace(workspace: WorkspaceInfo) = createSession(workspace)

    private fun createSession(explicitWorkspace: WorkspaceInfo?) {
        if (_state.value.creating) return
        val workspace = explicitWorkspace ?: _state.value.selectedWorkspace
        _state.update {
            it.copy(
                creating = true,
                sessionsError = null,
                selectedWorkspace = explicitWorkspace ?: it.selectedWorkspace
            )
        }
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
                    val workspaces = if (workspace == null) {
                        it.workspaces
                    } else {
                        it.workspaces.map { group ->
                            if (group.workspaceId != workspace.workspaceId) group
                            else group.copy(
                                sessionIds = listOf(id) + group.sessionIds.filterNot { sid -> sid == id }
                            )
                        }
                    }
                    it.copy(
                        creating = false,
                        sessions = listOf(summary) + it.sessions.filterNot { s -> s.sessionId == id },
                        workspaces = workspaces
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
        if (current.chat.sending) return
        if (current.chat.attachments.any { it.uploading }) {
            _state.update { it.copy(chat = it.chat.copy(error = "文件还在上传，请稍候再发送")) }
            return
        }
        val attachments = current.chat.attachments.filter { it.ready }
        if (clean.isEmpty() && attachments.isEmpty()) return
        val wasRunning = current.chat.running
        val sessionId = session.sessionId

        val requestId = UUID.randomUUID().toString()
        val parts = attachments.mapNotNull { attachment ->
            if (attachment.isImage) {
                val bytes = attachment.bytes ?: return@mapNotNull null
                PromptPart(
                    type = "image",
                    mediaType = ImageMime.detect(bytes) ?: attachment.mime ?: "image/png",
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
            queued = wasRunning,
            attachments = attachments.filterNot { it.isImage }.map { it.name },
            images = attachments.filter { it.isImage && it.bytes != null }.map { attachment ->
                val bytes = attachment.bytes!!
                ChatImage(
                    attachmentId = "pending:${attachment.id}",
                    mediaType = ImageMime.detect(bytes) ?: attachment.mime ?: "image/png",
                    name = attachment.name,
                    data = Base64.getEncoder().encodeToString(bytes)
                )
            }
        )
        val preview = normalizePreview(clean)
        _state.update {
            val draft = it.drafts[sessionId] ?: ChatDraft()
            it.copy(
                drafts = it.drafts + (sessionId to draft.copy(text = "")),
                chat = it.chat.copy(
                    pending = if (wasRunning) it.chat.pending else pending,
                    queued = if (wasRunning) it.chat.queued + pending else it.chat.queued,
                    sending = true,
                    error = null
                ),
                lastPrompts = if (preview != null) {
                    it.lastPrompts + (session.sessionId to preview)
                } else {
                    it.lastPrompts
                }
            )
        }

        viewModelScope.launch {
            try {
                api.prompt(session.sessionId, clean, requestId, parts)
                if (!wasRunning) {
                    TaskMonitorService.start(
                        getApplication(),
                        session.sessionId,
                        session.title?.takeIf { it.isNotBlank() } ?: "Harness 任务",
                        session.asOfSeq
                    )
                }
                _state.update {
                    val draft = it.drafts[sessionId] ?: ChatDraft()
                    it.copy(
                        drafts = it.drafts + (sessionId to draft.copy(attachments = emptyList())),
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
                    it.copy(
                        chat = it.chat.copy(
                            sending = false,
                            pending = null,
                            queued = it.chat.queued.filterNot { q -> q.key == pending.key }
                        )
                    )
                }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update { st ->
                    val draft = st.drafts[sessionId] ?: ChatDraft()
                    st.copy(
                        drafts = st.drafts + (
                            sessionId to draft.copy(
                                text = draft.text.ifBlank { clean }
                            )
                        ),
                        chat = st.chat.copy(
                            sending = false,
                            pending = null,
                            queued = st.chat.queued.filterNot { q -> q.key == pending.key },
                            error = friendlyError(e),
                            loadError = false
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

    fun refreshComputerUse() {
        if (_state.value.connection == null) return
        viewModelScope.launch {
            try {
                val view = api.computerUse()
                _state.update { st ->
                    st.copy(chat = st.chat.copy(computerUse = view))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { st ->
                    st.copy(
                        chat = st.chat.copy(
                            computerUse = ComputerUseState(
                                available = false,
                                error = friendlyError(e)
                            )
                        )
                    )
                }
            }
        }
    }

    fun toggleComputerUse() {
        val chat = _state.value.chat
        if (chat.computerUseBusy) return
        val target = !chat.computerUse.enabled
        _state.update { st ->
            st.copy(
                chat = st.chat.copy(
                    computerUse = st.chat.computerUse.copy(
                        enabled = target,
                        error = null
                    ),
                    computerUseBusy = true
                )
            )
        }
        viewModelScope.launch {
            try {
                val view = api.setComputerUse(target)
                _state.update { st ->
                    st.copy(
                        chat = st.chat.copy(
                            computerUse = view,
                            computerUseBusy = false
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update { st ->
                    st.copy(
                        chat = st.chat.copy(
                            computerUse = st.chat.computerUse.copy(
                                enabled = !target,
                                error = e.message
                            ),
                            computerUseBusy = false
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { st ->
                    st.copy(
                        chat = st.chat.copy(
                            computerUse = st.chat.computerUse.copy(
                                enabled = !target,
                                error = friendlyError(e)
                            ),
                            computerUseBusy = false
                        )
                    )
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

    suspend fun loadImage(attachmentId: String): ChatImage? {
        imageCache[attachmentId]?.let { return it }
        val sessionId = _state.value.activeSession?.sessionId ?: return null
        val image = try {
            api.readAttachment(sessionId, attachmentId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null
        if (imageCache.size >= 64) {
            imageCache.keys.firstOrNull()?.let { imageCache.remove(it) }
        }
        imageCache[attachmentId] = image
        return image
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
        val sessionId = session.sessionId
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { readFile(uri) }
                if (file.bytes.size > 20 * 1024 * 1024) {
                    throw IllegalArgumentException("文件不能超过 20 MB")
                }
                val existing = _state.value.drafts[sessionId]?.attachments
                    ?: _state.value.chat.attachments.takeIf {
                        _state.value.activeSession?.sessionId == sessionId
                    }
                    ?: emptyList()
                if (existing.any { it.name == file.name && it.size == file.bytes.size.toLong() }) {
                    return@launch
                }
                val mime = file.mime?.lowercase()
                val extension = file.name.substringAfterLast('.', "").lowercase()
                val imageMime = when (extension) {
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    else -> null
                }
                val effectiveMime = ImageMime.detect(file.bytes) ?: when {
                    mime?.startsWith("image/") == true -> mime
                    imageMime != null -> imageMime
                    else -> file.mime
                }
                val isImage = ImageMime.supportedImage(effectiveMime)
                val attachment = Attachment(
                    id = UUID.randomUUID().toString(),
                    name = file.name,
                    size = file.bytes.size.toLong(),
                    mime = effectiveMime,
                    isImage = isImage,
                    bytes = if (isImage) file.bytes else null,
                    uploading = !isImage
                )
                _state.update { st ->
                    val draft = st.drafts[sessionId] ?: ChatDraft()
                    val nextDraft = draft.copy(attachments = draft.attachments + attachment)
                    st.copy(
                        drafts = st.drafts + (sessionId to nextDraft),
                        chat = if (st.activeSession?.sessionId == sessionId) {
                            st.chat.copy(attachments = nextDraft.attachments)
                        } else {
                            st.chat
                        }
                    )
                }
                if (!isImage) {
                    try {
                        val receipt = api.uploadFile(sessionId, file.bytes, file.name)
                        updateDraftAttachment(sessionId, attachment.id) {
                            it.copy(receiptId = receipt.receiptId, uploading = false)
                        }
                    } catch (e: Exception) {
                        updateDraftAttachment(sessionId, attachment.id) {
                            it.copy(uploading = false, error = friendlyError(e))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_state.value.activeSession?.sessionId == sessionId) {
                    _state.update {
                        it.copy(chat = it.chat.copy(error = friendlyError(e)))
                    }
                }
            }
        }
    }

    fun removeAttachment(id: String) {
        val sessionId = _state.value.activeSession?.sessionId ?: return
        _state.update { st ->
            val draft = st.drafts[sessionId] ?: ChatDraft()
            val attachments = draft.attachments.filterNot { it.id == id }
            st.copy(
                drafts = st.drafts + (sessionId to draft.copy(attachments = attachments)),
                chat = st.chat.copy(attachments = attachments)
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

    fun loadEarlier() {
        val current = _state.value
        val session = current.activeSession ?: return
        if (current.chat.loadingEarlier || !current.chat.hasMoreHistory) return
        val historyItems = current.chat.items
            .filter { it is UserMessage || it is AssistantMessage || it is ToolItem }
        val firstSeq = historyItems.minOfOrNull { it.seq } ?: return
        val historyBoundary = firstSeq
        viewModelScope.launch {
            _state.update { it.copy(chat = it.chat.copy(loadingEarlier = true, error = null)) }
            try {
                val page = api.page(
                    session.sessionId,
                    current.chat.asOfSeq,
                    firstSeq,
                    session.subagentParentId
                )
                val older = EventParser.parse(page.records)
                _state.update { st ->
                    val merged = (older + st.chat.items).distinctBy { it.key }
                    st.copy(
                        chat = st.chat.copy(
                            items = merged,
                            hasMoreHistory = page.hasMore,
                            loadingEarlier = false,
                            historyToolCutoffSeq = st.chat.historyToolCutoffSeq
                                ?: historyBoundary
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _state.update { it.copy(chat = it.chat.copy(loadingEarlier = false)) }
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        chat = it.chat.copy(
                            loadingEarlier = false,
                            error = friendlyError(e)
                        )
                    )
                }
            }
        }
    }

    fun refreshChat() {
        val session = _state.value.activeSession ?: return
        startPolling(session.sessionId, reset = true)
    }

    fun openAutomations() {
        _state.update {
            it.copy(
                automationsVisible = true,
                resourcesVisible = false,
                automationsActionError = null
            )
        }
        refreshAutomations()
    }

    fun closeAutomations() {
        _state.update { it.copy(automationsVisible = false) }
    }

    fun refreshAutomations() {
        if (_state.value.automationsLoading) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    automationsLoading = true,
                    automationsError = null,
                    automationsActionError = null
                )
            }
            try {
                val tasks = api.scheduleCatalog()
                _state.update {
                    it.copy(automations = tasks, automationsLoading = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(automationsLoading = false, automationsError = friendlyError(e))
                }
            }
        }
    }

    fun deleteAutomation(task: AutomationTask) {
        if (_state.value.automationsBusyId != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(automationsBusyId = task.id, automationsActionError = null)
            }
            try {
                api.deleteAutomation(task)
                _state.update { st ->
                    st.copy(
                        automations = st.automations.filterNot { it.id == task.id },
                        automationsBusyId = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        automationsBusyId = null,
                        automationsActionError = friendlyError(e)
                    )
                }
            }
        }
    }

    fun updateAutomation(task: AutomationTask, change: AutomationChange) {
        if (_state.value.automationsBusyId != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(automationsBusyId = task.id, automationsActionError = null)
            }
            try {
                api.updateAutomation(task, change)
                _state.update { it.copy(automationsBusyId = null) }
                refreshAutomations()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                handleAuthFailure(e.message ?: "登录已过期，请重新连接")
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        automationsBusyId = null,
                        automationsActionError = friendlyError(e)
                    )
                }
            }
        }
    }

    fun openResources() {
        _state.update { it.copy(resourcesVisible = true, automationsVisible = false) }
        refreshResources()
    }

    fun closeResources() {
        _state.update { it.copy(resourcesVisible = false) }
    }

    fun refreshResources() {
        if (_state.value.resourcesLoading) return
        viewModelScope.launch {
            _state.update { it.copy(resourcesLoading = true, resourcesError = null) }
            try {
                val status = api.resourceStatus()
                _state.update {
                    it.copy(resourceStatus = status, resourcesLoading = false)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(resourcesLoading = false, resourcesError = friendlyError(e))
                }
            }
        }
    }

    fun setResourceLimit(maxConcurrent: Int) {
        if (_state.value.resourcesBusy) return
        viewModelScope.launch {
            _state.update { it.copy(resourcesBusy = true, resourcesError = null) }
            try {
                val status = api.setResourceLimit(maxConcurrent)
                _state.update {
                    it.copy(
                        resourceStatus = status,
                        resourcesBusy = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(resourcesBusy = false, resourcesError = friendlyError(e))
                }
            }
        }
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

    private suspend fun loadLiveState(sessionId: String): SessionLiveState? {
        return try {
            api.sessionLiveState(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun mergeQueue(server: List<UserMessage>, local: List<UserMessage>): List<UserMessage> {
        val serverKeys = server.map { it.key }.toSet()
        val serverRpcIds = server.mapNotNull { it.rpcId }.toSet()
        val localOnly = local.filter { item ->
            item.key !in serverKeys &&
                !item.key.startsWith("inbox:") &&
                (item.rpcId == null || item.rpcId !in serverRpcIds)
        }
        return server + localOnly
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
                                    questions = if (row.questions.isNotEmpty()) {
                                        row.questions
                                    } else {
                                        st.chat.questions
                                    },
                                    usage = row.usage ?: st.chat.usage,
                                    lastPromptAt = row.lastPromptAt ?: st.chat.lastPromptAt
                                )
                            )
                        }
                        if (first || row.asOfSeq != pageSeq) {
                            val page = api.page(
                                sessionId,
                                row.asOfSeq,
                                null,
                                row.subagentParentId
                            )
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
                            val echoedRpcIds = parsed.filterIsInstance<UserMessage>()
                                .mapNotNull { it.rpcId }
                                .toSet()
                            val live = loadLiveState(sessionId)
                            _state.update { st ->
                                val remainingQueued = st.chat.queued.filterNot {
                                    it.rpcId != null && it.rpcId in echoedRpcIds
                                }
                                val nextQueued = live?.let { server ->
                                    mergeQueue(
                                        server.inbox.filterNot { item ->
                                            item.rpcId != null && item.rpcId in echoedRpcIds
                                        },
                                        remainingQueued
                                    )
                                } ?: remainingQueued
                                val oldestParsed = parsed.minOfOrNull { it.seq }
                                val older = st.chat.items.filter { item ->
                                    oldestParsed == null || item.seq < oldestParsed
                                }
                                val mergedItems = (older + parsed)
                                    .distinctBy { it.key }
                                    .sortedBy { it.seq }
                                val keptOlder = mergedItems.size > parsed.size
                                val preview = normalizePreview(
                                    mergedItems.filterIsInstance<UserMessage>()
                                        .lastOrNull()
                                        ?.text
                                )
                                st.copy(
                                    chat = st.chat.copy(
                                        loading = false,
                                        items = mergedItems,
                                        pending = stillPending,
                                        queued = nextQueued,
                                        hasMoreHistory = if (keptOlder) {
                                            st.chat.hasMoreHistory
                                        } else {
                                            page.hasMore
                                        },
                                        running = row.running,
                                        title = st.chat.title ?: row.title,
                                        error = null,
                                        context = live?.context ?: st.chat.context,
                                        asOfSeq = row.asOfSeq
                                    ),
                                    lastPrompts = if (preview != null) {
                                        st.lastPrompts + (sessionId to preview)
                                    } else {
                                        st.lastPrompts
                                    }
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
                        it.copy(
                            chat = it.chat.copy(
                                loading = false,
                                error = friendlyError(e),
                                loadError = true
                            )
                        )
                    }
                    wait = 4000L
                }
                delay(wait)
            }
        }
    }
}
