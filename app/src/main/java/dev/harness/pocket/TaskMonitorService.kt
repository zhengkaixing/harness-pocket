package dev.harness.pocket

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.harness.pocket.data.HarnessApi
import dev.harness.pocket.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class TaskMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var ntfyJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ntfyOnly = intent?.getBooleanExtra(EXTRA_NTFY_ONLY, false) == true
        if (ntfyOnly) {
            val topic = Prefs(this).ntfyTopic()
            if (topic.isBlank()) {
                stopSelf()
                return START_NOT_STICKY
            }
            startForeground(ONGOING_ID, ntfyOnlyNotification())
            if (ntfyJob == null) ntfyJob = scope.launch { subscribeNtfy(topic) }
            return START_STICKY
        }
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Harness 任务"
        val baseline = intent.getIntExtra(EXTRA_BASELINE_SEQ, -1)

        startForeground(ONGOING_ID, ongoingNotification(sessionId, title))
        val topic = Prefs(this).ntfyTopic()
        if (topic.isNotBlank() && ntfyJob == null) {
            ntfyJob = scope.launch { subscribeNtfy(topic) }
        }
        monitorJob?.cancel()
        monitorJob = scope.launch {
            monitor(sessionId, title, baseline)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ntfyJob?.cancel()
        monitorJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private suspend fun monitor(sessionId: String, title: String, baseline: Int) {
        val connection = Prefs(this).loadConnection() ?: return
        val api = HarnessApi()
        api.setConnection(connection)

        var sawRunning = false
        var idleTicks = 0
        var elapsed = 0L
        var currentTitle = title
        while (scope.isActive && elapsed < MAX_WAIT_MS) {
            val sessions = runCatching { api.listSessions() }.getOrNull()
            val row = sessions?.firstOrNull { it.sessionId == sessionId }
            if (row != null) {
                row.title?.takeIf { it.isNotBlank() }?.let { currentTitle = it }
                if (row.running) {
                    sawRunning = true
                    idleTicks = 0
                }
                if (row.asOfSeq > baseline) {
                    val page = runCatching {
                        api.page(sessionId, row.asOfSeq, parentSessionId = row.parentSessionId)
                    }.getOrNull()
                    val end = page?.records?.let { findTurnEnd(it, baseline) }
                    if (end != null) {
                        notifyDone(sessionId, currentTitle, end)
                        return
                    }
                    if (!row.running) {
                        idleTicks++
                        if (sawRunning || idleTicks >= 2) {
                            notifyDone(sessionId, currentTitle, Completion(true, null))
                            return
                        }
                    }
                }
            }
            delay(POLL_MS)
            elapsed += POLL_MS
        }
    }


    private suspend fun subscribeNtfy(topic: String) {
        val server = Prefs(this).ntfyServer().trimEnd('/')
        val url = "$server/${URLEncoder.encode(topic, "UTF-8")}/json"
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder().url(url).header("Accept", "application/x-ndjson").build()
        val call = client.newCall(request)
        try {
            call.execute().use { response ->
                val source = response.body?.source() ?: return
                while (scope.isActive && !source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    val objectValue = runCatching { JSONObject(line) }.getOrNull() ?: continue
                    if (objectValue.optString("event") != "message") continue
                    val title = objectValue.optString("title").ifBlank { "Harness" }
                    val body = objectValue.optString("message").ifBlank { "收到一条远程任务通知" }
                    val click = objectValue.optString("click")
                    val sessionId = runCatching {
                        android.net.Uri.parse(click).getQueryParameter("sessionId")
                    }.getOrNull()
                    notifyRemote(title, body, sessionId)
                }
            }
        } catch (_: Exception) {
            // ntfy is best-effort; keep the foreground polling fallback alive.
        }
    }

    private fun notifyRemote(title: String, body: String, sessionId: String?) {
        val manager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent(sessionId.orEmpty()))
            .build()
        manager.notify(COMPLETION_ID + 900 + abs(body.hashCode() % 100), notification)
    }

    private fun findTurnEnd(records: JSONArray, baseline: Int): Completion? {
        for (i in 0 until records.length()) {
            val event = records.optJSONObject(i)?.optJSONObject("event") ?: continue
            if (event.optString("type") != "turn/end") continue
            if (event.optInt("seq", 0) <= baseline) continue
            val reason = event.optJSONObject("data")?.optJSONObject("reason")
            val kind = reason?.optString("kind").orEmpty()
            if (kind == "error") {
                val message = reason?.optJSONObject("error")?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: "任务执行失败"
                return Completion(false, message)
            }
            if (kind == "cancelled" || kind == "canceled") {
                return Completion(false, "任务已停止", cancelled = true)
            }
            return Completion(true, null)
        }
        return null
    }

    private fun ntfyOnlyNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Harness 后台推送已开启")
            .setContentText("正在监听 ntfy 主题")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent(""))
            .build()
    }

    private fun ongoingNotification(sessionId: String, title: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Harness 正在执行任务")
            .setContentText(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent(sessionId))
            .build()
    }

    private fun notifyDone(sessionId: String, title: String, completion: Completion) {
        val manager = getSystemService(NotificationManager::class.java)
        val heading = when {
            completion.cancelled -> "Harness 任务已停止"
            completion.ok -> "Harness 任务完成"
            else -> "Harness 任务失败"
        }
        val body = when {
            completion.cancelled -> "「${title.take(40)}」已停止"
            completion.ok -> "「${title.take(40)}」已生成回复"
            else -> friendlyError(completion.message)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(heading)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent(sessionId))
            .build()
        manager.notify(COMPLETION_ID + abs(sessionId.hashCode() % 1000), notification)
    }

    private fun friendlyError(message: String?): String {
        if (message.isNullOrBlank()) return "任务遇到错误，请打开 App 查看详情"
        val nested = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
            .find(message)
            ?.groupValues
            ?.get(1)
        val text = (nested ?: message)
            .replace("\\n", " ")
            .replace("\\\"", "\"")
            .trim()
        return text.take(180).ifBlank { "任务遇到错误，请打开 App 查看详情" }
    }

    private fun openIntent(sessionId: String): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            this,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                "任务运行中",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Harness 正在执行任务时显示"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE,
                "任务完成提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Harness 任务结束后提醒"
            }
        )
    }

    private data class Completion(
        val ok: Boolean,
        val message: String?,
        val cancelled: Boolean = false
    )

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_TITLE = "session_title"
        const val EXTRA_BASELINE_SEQ = "baseline_seq"
        const val EXTRA_NTFY_ONLY = "ntfy_only"

        private const val CHANNEL_ONGOING = "harness_task_running"
        private const val CHANNEL_DONE = "harness_task_done"
        private const val ONGOING_ID = 9101
        private const val COMPLETION_ID = 9200
        private const val POLL_MS = 4_000L
        private const val MAX_WAIT_MS = 20 * 60 * 1_000L

        fun startNtfy(context: Context) {
            val intent = Intent(context, TaskMonitorService::class.java).apply {
                putExtra(EXTRA_NTFY_ONLY, true)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun start(context: Context, sessionId: String, title: String, baselineSeq: Int) {
            val intent = Intent(context, TaskMonitorService::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BASELINE_SEQ, baselineSeq)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
