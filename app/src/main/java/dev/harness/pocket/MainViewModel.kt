package dev.harness.pocket

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.AuthException
import dev.harness.pocket.data.ChatState
import dev.harness.pocket.data.ConnectionInput
import dev.harness.pocket.data.EventParser
import dev.harness.pocket.data.HarnessApi
import dev.harness.pocket.data.Prefs
import dev.harness.pocket.data.SessionSummary
import dev.harness.pocket.data.UserMessage
import dev.harness.pocket.data.friendlyError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val api = HarnessApi()

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var pageSeq = -1

    init {
        val saved = prefs.loadConnection()
        if (saved != null) {
            api.setConnection(saved)
            _state.value = AppState(
                restoring = false,
                savedBase = saved.baseUrl,
                connection = saved
            )
            loadSessions()
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
                    asOfSeq = session.asOfSeq
                )
            )
        }
        startPolling(session.sessionId, reset = true)
    }

    fun newSession() {
        if (_state.value.creating) return
        _state.update { it.copy(creating = true, sessionsError = null) }
        viewModelScope.launch {
            try {
                val id = api.createSession()
                val summary = SessionSummary(
                    sessionId = id,
                    title = null,
                    updatedAt = System.currentTimeMillis(),
                    running = false,
                    cwd = null,
                    asOfSeq = 0,
                    blank = true
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
        if (clean.isEmpty()) return
        val current = _state.value
        val session = current.activeSession ?: return
        if (current.chat.sending || current.chat.running) return

        val requestId = UUID.randomUUID().toString()
        val pending = UserMessage(
            key = "pending:$requestId",
            seq = Int.MAX_VALUE,
            text = clean,
            rpcId = requestId,
            pending = true
        )
        _state.update {
            it.copy(chat = it.chat.copy(pending = pending, sending = true, error = null))
        }

        viewModelScope.launch {
            try {
                api.prompt(session.sessionId, clean, requestId)
                _state.update {
                    it.copy(chat = it.chat.copy(sending = false, running = true))
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
                                    running = row.running
                                )
                            )
                        }
                        if (first || row.asOfSeq != pageSeq) {
                            val page = api.page(sessionId, row.asOfSeq)
                            val parsed = EventParser.parse(page.records)
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
