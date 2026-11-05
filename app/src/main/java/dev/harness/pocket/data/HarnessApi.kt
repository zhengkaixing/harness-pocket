package dev.harness.pocket.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

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
            result.add(
                SessionSummary(
                    sessionId = row.optString("sessionId"),
                    title = title,
                    updatedAt = row.optLong("updatedAt", 0L),
                    running = row.optBoolean("running", false),
                    cwd = cwd,
                    asOfSeq = projections?.optInt("asOfSeq", 0) ?: 0,
                    blank = row.optBoolean("blank", false)
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

    suspend fun createSession(): String {
        val value = rpc("session/create", JSONObject().put("request", JSONObject()))
        val id = value.optString("sessionId")
        if (id.isBlank()) throw ApiException("服务器没有返回会话 ID")
        return id
    }

    suspend fun prompt(sessionId: String, text: String, requestId: String): String {
        val content = JSONArray().put(JSONObject().put("type", "text").put("text", text))
        val request = JSONObject()
            .put("requestId", requestId)
            .put("sessionId", sessionId)
            .put("mode", "queue")
            .put("content", content)
            .put("clientTimeZone", TimeZone.getDefault().id)
        rpc("session/prompt", JSONObject().put("request", request))
        return requestId
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
