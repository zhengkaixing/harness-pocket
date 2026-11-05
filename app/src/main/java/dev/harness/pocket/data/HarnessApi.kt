package dev.harness.pocket.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

open class ApiException(message: String, val code: String? = null) : Exception(message)

class AuthException(message: String) : ApiException(message)

class HarnessApi {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val connectMutex = Mutex()

    @Volatile
    private var connection: Connection? = null

    fun setConnection(value: Connection?) {
        connection = value
    }

    fun currentConnection(): Connection? = connection

    suspend fun connect(baseUrl: String, token: String): Connection = withContext(Dispatchers.IO) {
        connectMutex.withLock { openConnection(baseUrl, token) }
    }

    private fun openConnection(baseUrl: String, token: String): Connection {
        val base = baseUrl.trimEnd('/')
        val encoded = URLEncoder.encode(token, "UTF-8")
        val request = Request.Builder()
            .url("$base/connect?token=$encoded&interactions=none")
            .get()
            .header("Accept", "text/html,application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code != 302 && response.code != 303) {
                val body = response.body?.string().orEmpty()
                val message = when (response.code) {
                    400 -> "连接声明失败：服务器返回 400，请重新复制链接"
                    401, 403 -> "令牌无效或已过期，请重新复制带 token 的链接"
                    else -> "连接失败：HTTP ${response.code}" +
                        if (body.isNotBlank()) " · ${body.take(160)}" else ""
                }
                throw ApiException(message)
            }

            val cookie = response.headers("Set-Cookie")
                .asSequence()
                .map { it.substringBefore(';').trim() }
                .firstOrNull { it.startsWith("dsh-auth-") }
                ?: throw ApiException("服务器没有返回登录 Cookie，请确认地址是 dsh web 服务")

            return Connection(base, token, cookie).also { connection = it }
        }
    }

    suspend fun listSessions(): List<SessionSummary> {
        val value = rpc("session/list", JSONObject().put("_request", JSONObject()))
        val items = value.optJSONArray("items") ?: JSONArray()
        val result = ArrayList<SessionSummary>(items.length())
        for (i in 0 until items.length()) {
            val row = items.optJSONObject(i) ?: continue
            val projections = row.optJSONObject("projections")
            val values = projections?.optJSONObject("values")
            val title = if (values != null && !values.isNull("title")) {
                values.optString("title").takeIf { it.isNotBlank() }
            } else {
                null
            }
            val cwd = if (row.isNull("cwd")) null else row.optString("cwd").takeIf { it.isNotBlank() }
            val modelObject = values?.optJSONObject("modelSelection")?.let {
                it.optJSONObject("next") ?: it.optJSONObject("lastUsed")
            }
            val model = modelObject?.let {
                val provider = it.optString("provider")
                val name = it.optString("model")
                if (provider.isBlank() || name.isBlank()) null
                else ModelChoice(
                    provider = provider,
                    model = name,
                    effort = it.optString("reasoningEffort").takeIf { value -> value.isNotBlank() }
                )
            }
            val permission = values?.optJSONObject("permissions")
                ?.optString("currentValue")
                ?.takeIf { it.isNotBlank() }
            result.add(
                SessionSummary(
                    sessionId = row.optString("sessionId"),
                    title = title,
                    updatedAt = row.optLong("updatedAt", 0L),
                    running = row.optBoolean("running", false),
                    cwd = cwd,
                    asOfSeq = projections?.optInt("asOfSeq", 0) ?: 0,
                    blank = row.optBoolean("blank", false),
                    model = model,
                    permission = permission
                )
            )
        }
        return result.filter { it.sessionId.isNotBlank() }
    }

    suspend fun page(sessionId: String, throughSeq: Int): PageResult {
        val request = JSONObject()
            .put("address", JSONObject().put("kind", "session").put("sessionId", sessionId))
            .put("throughSeq", throughSeq)
            .put("maxMessages", 80)
            .put("turnWindow", JSONObject().put("minMessages", 24).put("minTurns", 4))
        val value = rpc("session/page", JSONObject().put("request", request))
        return PageResult(
            records = value.optJSONArray("records") ?: JSONArray(),
            hasMore = value.optBoolean("hasMore", false)
        )
    }

    suspend fun createSession(workspaceId: String? = null, cwd: String? = null): String {
        val request = JSONObject()
        when {
            !workspaceId.isNullOrBlank() -> request.put("workspaceId", workspaceId)
            !cwd.isNullOrBlank() -> request.put("cwd", cwd)
        }
        val value = rpc("session/create", JSONObject().put("request", request))
        val id = value.optString("sessionId")
        if (id.isBlank()) throw ApiException("服务器没有返回会话 ID")
        return id
    }

    suspend fun prompt(
        sessionId: String,
        text: String,
        requestId: String,
        parts: List<PromptPart> = emptyList()
    ): String {
        val content = JSONArray()
        if (text.isNotBlank()) {
            content.put(JSONObject().put("type", "text").put("text", text))
        }
        parts.forEach { part ->
            val item = JSONObject().put("type", part.type)
            when (part.type) {
                "text" -> item.put("text", part.text.orEmpty())
                "image" -> {
                    item.put("mediaType", part.mediaType.orEmpty())
                    item.put("data", part.data.orEmpty())
                    if (!part.name.isNullOrBlank()) item.put("name", part.name)
                }
                "file" -> item.put("receiptId", part.receiptId.orEmpty())
            }
            content.put(item)
        }
        if (content.length() == 0) throw ApiException("消息内容不能为空")

        val request = JSONObject()
            .put("requestId", requestId)
            .put("sessionId", sessionId)
            .put("mode", "queue")
            .put("content", content)
            .put("clientTimeZone", TimeZone.getDefault().id)
        rpc("session/prompt", JSONObject().put("request", request))
        return requestId
    }

    suspend fun uploadFile(sessionId: String, bytes: ByteArray, name: String): UploadReceipt {
        val encoded = Base64.getEncoder().encodeToString(bytes)
        val request = JSONObject().put("data", encoded).put("name", name)
        val value = rpc(
            "fileUploads/upload",
            JSONObject().put("agentId", sessionId).put("request", request)
        )
        val receiptId = value.optString("receiptId")
        if (receiptId.isBlank()) throw ApiException("文件上传没有返回 receipt")
        val file = value.optJSONObject("file")
        return UploadReceipt(
            receiptId = receiptId,
            name = file?.optString("name")?.takeIf { it.isNotBlank() } ?: name,
            bytes = file?.optLong("bytes", bytes.size.toLong()) ?: bytes.size.toLong()
        )
    }

    suspend fun modelCatalog(): ModelCatalog {
        val value = rpc("session/modelCatalog", JSONObject())
        val groups = ArrayList<ModelGroup>()
        val rawGroups = value.optJSONArray("groups") ?: JSONArray()
        for (i in 0 until rawGroups.length()) {
            val group = rawGroups.optJSONObject(i) ?: continue
            val models = ArrayList<AiModel>()
            val rawModels = group.optJSONArray("models") ?: JSONArray()
            for (j in 0 until rawModels.length()) {
                val model = rawModels.optJSONObject(j) ?: continue
                val efforts = ArrayList<ModelEffort>()
                val reasoning = model.optJSONObject("reasoning")
                val rawEfforts = reasoning?.optJSONArray("efforts") ?: JSONArray()
                for (k in 0 until rawEfforts.length()) {
                    val effort = rawEfforts.optJSONObject(k) ?: continue
                    efforts.add(
                        ModelEffort(
                            id = effort.optString("id"),
                            name = effort.optString("name"),
                            description = effort.optString("description").takeIf { it.isNotBlank() }
                        )
                    )
                }
                models.add(
                    AiModel(
                        id = model.optString("id"),
                        name = model.optString("name").ifBlank { model.optString("id") },
                        description = model.optString("description").takeIf { it.isNotBlank() },
                        efforts = efforts,
                        defaultEffort = reasoning?.optString("defaultEffort")?.takeIf { it.isNotBlank() }
                    )
                )
            }
            groups.add(
                ModelGroup(
                    id = group.optString("id"),
                    name = group.optString("name").ifBlank { group.optString("id") },
                    models = models
                )
            )
        }
        val failures = ArrayList<String>()
        val rawFailures = value.optJSONArray("failures") ?: JSONArray()
        for (i in 0 until rawFailures.length()) {
            val failure = rawFailures.optJSONObject(i)
            failures.add(
                failure?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: rawFailures.optString(i)
            )
        }
        val rawDefault = value.optJSONObject("default")
        val defaultChoice = rawDefault?.let {
            val provider = it.optString("provider")
            val model = it.optString("model")
            if (provider.isBlank() || model.isBlank()) null
            else ModelChoice(provider, model, it.optString("reasoningEffort").takeIf { e -> e.isNotBlank() })
        }
        return ModelCatalog(defaultChoice, groups, failures)
    }

    suspend fun permissionCatalog(): PermissionCatalog {
        val value = rpc("permissionPresets/catalog", JSONObject())
        val options = ArrayList<PermissionOption>()
        val raw = value.optJSONArray("options") ?: JSONArray()
        for (i in 0 until raw.length()) {
            val option = raw.optJSONObject(i) ?: continue
            options.add(
                PermissionOption(
                    value = option.optString("value"),
                    name = option.optString("name").ifBlank { option.optString("value") }
                )
            )
        }
        return PermissionCatalog(
            options = options,
            defaultPreset = value.optString("defaultPreset").takeIf { it.isNotBlank() }
        )
    }

    suspend fun selectModel(
        sessionId: String,
        provider: String,
        model: String,
        effort: String?
    ): ModelChoice {
        val request = JSONObject()
            .put("sessionId", sessionId)
            .put("provider", provider)
            .put("model", model)
        if (!effort.isNullOrBlank()) request.put("reasoningEffort", effort)
        val value = rpc("session/selectModel", JSONObject().put("request", request))
        val selected = value.optJSONObject("selected")
        return ModelChoice(
            provider = selected?.optString("provider")?.takeIf { it.isNotBlank() } ?: provider,
            model = selected?.optString("model")?.takeIf { it.isNotBlank() } ?: model,
            effort = selected?.optString("reasoningEffort")?.takeIf { it.isNotBlank() } ?: effort
        )
    }

    suspend fun executeCommand(sessionId: String, line: String): String {
        val args = JSONObject()
            .put("agentId", sessionId)
            .put("line", line)
            .put("submittedAttachments", JSONArray())
        val value = rpc("commands/execute", args)
        return value.optJSONObject("result")?.optString("text").orEmpty()
    }

    suspend fun createWorkspace(path: String): WorkspaceInfo {
        val value = rpc(
            "workspace/create",
            JSONObject().put("request", JSONObject().put("path", path))
        )
        return parseWorkspace(value.optJSONObject("workspace"))
    }

    suspend fun pinSession(sessionId: String): List<String> {
        val request = JSONObject().put("sessionId", sessionId)
        val value = rpc("workspace/pinSession", JSONObject().put("request", request))
        return jsonStrings(value.optJSONArray("pinnedSessionIds"))
    }

    suspend fun unpinSession(sessionId: String): List<String> {
        val request = JSONObject().put("sessionId", sessionId)
        val value = rpc("workspace/unpinSession", JSONObject().put("request", request))
        return jsonStrings(value.optJSONArray("pinnedSessionIds"))
    }

    suspend fun archiveSession(sessionId: String, stopActivity: Boolean = true): List<String> {
        val request = JSONObject().put("sessionId", sessionId)
        if (stopActivity) request.put("stopActivity", true)
        val value = rpc("workspace/archiveSession", JSONObject().put("request", request))
        return jsonStrings(value.optJSONArray("archivedSessionIds"))
    }

    suspend fun unarchiveSession(sessionId: String): List<String> {
        val request = JSONObject().put("sessionId", sessionId)
        val value = rpc("workspace/unarchiveSession", JSONObject().put("request", request))
        return jsonStrings(value.optJSONArray("archivedSessionIds"))
    }

    suspend fun listWorkspaces(): WorkspaceSnapshot = withTimeout(20_000) {
        suspendCancellableCoroutine { continuation ->
            val conn = connection ?: run {
                continuation.resumeWithException(AuthException("尚未连接"))
                return@suspendCancellableCoroutine
            }
            val wsBase = conn.baseUrl
                .replaceFirst("https://", "wss://")
                .replaceFirst("http://", "ws://")
            val request = Request.Builder()
                .url("$wsBase/api/remote.mux")
                .header("Cookie", conn.cookie)
                .header("Origin", conn.baseUrl)
                .build()
            val resumed = AtomicBoolean(false)
            val streamId = "workspace-${UUID.randomUUID()}"
            val socket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val open = JSONObject()
                        .put("type", "open")
                        .put("streamId", streamId)
                        .put("endpoint", "workspace/follow")
                        .put("payload", JSONObject().put("args", JSONObject()))
                    webSocket.send(open.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val message = runCatching { JSONObject(text) }.getOrNull() ?: return
                    if (message.optString("type") != "item") return
                    val frame = message.optJSONObject("value") ?: return
                    if (frame.optString("type") != "baseline") return
                    val payload = frame.optJSONObject("value") ?: return
                    val items = payload.optJSONArray("items") ?: JSONArray()
                    val result = ArrayList<WorkspaceInfo>()
                    for (i in 0 until items.length()) {
                        runCatching { parseWorkspace(items.optJSONObject(i)) }
                            .getOrNull()
                            ?.let { result.add(it) }
                    }
                    if (resumed.compareAndSet(false, true)) {
                        continuation.resume(
                            WorkspaceSnapshot(
                                workspaces = result,
                                pinnedSessionIds = jsonStrings(payload.optJSONArray("pinnedSessionIds")),
                                archivedSessionIds = jsonStrings(payload.optJSONArray("archivedSessionIds"))
                            )
                        )
                        webSocket.close(1000, "done")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (resumed.compareAndSet(false, true)) {
                        continuation.resumeWithException(
                            ApiException("工作区加载失败：${t.message ?: "连接中断"}")
                        )
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (resumed.compareAndSet(false, true)) {
                        continuation.resumeWithException(ApiException("工作区连接已关闭"))
                    }
                }
            })
            continuation.invokeOnCancellation { socket.cancel() }
        }
    }

    private fun jsonStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val values = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val value = array.optString(i)
            if (value.isNotBlank()) values.add(value)
        }
        return values
    }

    private fun parseWorkspace(objectValue: JSONObject?): WorkspaceInfo {
        val obj = objectValue ?: throw ApiException("服务器没有返回工作区信息")
        val id = obj.optString("workspaceId")
        if (id.isBlank()) throw ApiException("服务器没有返回工作区 ID")
        return WorkspaceInfo(
            workspaceId = id,
            path = obj.optString("path"),
            title = obj.optString("title").ifBlank { obj.optString("path") }
        )
    }

    suspend fun cancel(sessionId: String) {
        rpc(
            "session/cancel",
            JSONObject().put("request", JSONObject().put("sessionId", sessionId))
        )
    }

    suspend fun rename(sessionId: String, title: String) {
        rpc(
            "session/rename",
            JSONObject().put(
                "request",
                JSONObject().put("sessionId", sessionId).put("title", title)
            )
        )
    }

    private suspend fun rpc(method: String, args: JSONObject): JSONObject =
        withContext(Dispatchers.IO) { rpcBlocking(method, args, true) }

    private suspend fun rpcBlocking(method: String, args: JSONObject, allowRetry: Boolean): JSONObject {
        val conn = connection ?: throw AuthException("尚未连接")
        val rpcId = UUID.randomUUID().toString()
        val body = JSONObject()
            .put("type", "client-request")
            .put("rpcId", rpcId)
            .put("method", method)
            .put("payload", JSONObject().put("args", args))

        val request = Request.Builder()
            .url("${conn.baseUrl}/api/$method")
            .header("Cookie", conn.cookie)
            .header("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                if (!allowRetry) throw AuthException("登录已过期，请重新连接")
                val renewed = connectMutex.withLock {
                    val current = connection
                    if (current == null || current.cookie == conn.cookie) {
                        openConnection(conn.baseUrl, conn.token)
                    } else {
                        current
                    }
                }
                if (renewed == null) throw AuthException("登录已过期，请重新连接")
                return rpcBlocking(method, args, false)
            }

            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(
                    "HTTP ${response.code}" + if (text.isNotBlank()) " · ${text.take(180)}" else ""
                )
            }

            val envelope = try {
                JSONObject(text)
            } catch (_: Exception) {
                throw ApiException("服务器返回了无法解析的数据")
            }
            if (envelope.optString("rpcId") != rpcId) {
                throw ApiException("响应不匹配，请重试")
            }
            val result = envelope.optJSONObject("result")
                ?: throw ApiException("服务器响应缺少 result")
            if (!result.optBoolean("ok")) {
                val error = result.optJSONObject("error")
                val code = error?.optString("code").orEmpty()
                val message = error?.optString("message").orEmpty().ifBlank { "请求失败" }
                if (code.contains("auth", ignoreCase = true) ||
                    code.contains("unauthorized", ignoreCase = true)
                ) {
                    throw AuthException(message)
                }
                throw ApiException(message, code)
            }
            return result.optJSONObject("value") ?: JSONObject()
        }
    }
}
