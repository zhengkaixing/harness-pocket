package dev.harness.pocket.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.AutomationTask
import dev.harness.pocket.data.SessionSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ActiveGreen = Color(0xFF168A55)
private val ActiveGreenBg = Color(0xFFEAF7F0)

@Composable
fun AutomationScreen(
    state: AppState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSession: (SessionSummary) -> Unit
) {
    BackHandler(onBack = onBack)
    HarnessBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftIconButton(Icons.Rounded.ArrowBack, onBack, "返回")
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("自动化任务", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "由会话中的定时提醒创建",
                        color = Muted,
                        fontSize = 10.5.sp
                    )
                }
                if (state.automationsLoading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = Blue,
                        strokeWidth = 2.dp
                    )
                } else {
                    SoftIconButton(Icons.Rounded.Refresh, onRefresh, "刷新自动化任务")
                }
            }

            when {
                state.automationsLoading && state.automations.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Blue,
                            strokeWidth = 2.4.dp
                        )
                    }
                }

                state.automationsError != null && state.automations.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        ErrorCard(state.automationsError, onRefresh)
                    }
                }

                state.automations.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        EmptyCard()
                    }
                }

                else -> {
                    val activeCount = state.automations.count { it.active }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 30.dp)
                    ) {
                        item(key = "summary") {
                            Text(
                                "${state.automations.size} 个任务 · $activeCount 个已开启",
                                color = Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                            )
                        }
                        state.automationsError?.let { error ->
                            item(key = "error") {
                                ErrorCard(error, onRefresh)
                            }
                        }
                        items(state.automations, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                session = state.sessions.firstOrNull {
                                    it.sessionId == task.sessionId
                                },
                                onOpen = {
                                    state.sessions.firstOrNull {
                                        it.sessionId == task.sessionId
                                    }?.let(onOpenSession)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: AutomationTask,
    session: SessionSummary?,
    onOpen: () -> Unit
) {
    val linkable = session != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = linkable, onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (task.active) ActiveGreenBg else Color(0xFFF1F2F4)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .then(
                                    Modifier.background(
                                        if (task.active) ActiveGreen else Color(0xFF98A0AB)
                                    )
                                )
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (task.active) "已开启" else "未运行",
                            color = if (task.active) ActiveGreen else Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    nextRunLabel(task),
                    color = if (task.active) InkSoft else Muted,
                    fontSize = 10.5.sp
                )
                if (linkable) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "打开原会话",
                        tint = Muted,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                task.title,
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (task.prompt.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    task.prompt,
                    color = InkSoft,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    ruleLabel(task),
                    color = Muted,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                if (session != null) {
                    "原会话 · ${session.title?.takeIf { it.isNotBlank() } ?: "未命名对话"}"
                } else {
                    "原会话 · ${task.sessionId.take(12)}"
                },
                color = Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F4FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("还没有自动化任务", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "在会话中让 Harness 创建定时提醒后，任务会显示在这里。",
                color = Muted,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF1F2),
        border = BorderStroke(1.dp, Color(0xFFF6C9CE))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("自动化任务读取失败", color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = InkSoft, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "点击重试",
                color = Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onRetry)
            )
        }
    }
}

private fun nextRunLabel(task: AutomationTask): String {
    if (!task.active) return "已结束"
    val formatted = formatInstant(task.scheduledAt) ?: return "等待下一次"
    return "下次 $formatted"
}

private fun ruleLabel(task: AutomationTask): String = when (task.kind) {
    "after" -> "延时 ${duration(task.afterSeconds ?: 0)}"
    "every" -> "每 ${duration(task.everySeconds ?: 0)}"
    "at" -> "单次 ${formatInstant(task.scheduledAt) ?: ""}".trim()
    "daily" -> "每天 ${shortTime(task.time)}"
    "weekly" -> {
        val days = task.weekdays.mapNotNull { weekdays[it] }.joinToString("、")
        "每周${days.ifBlank { "?" }} ${shortTime(task.time)}"
    }
    "cron" -> "Cron ${task.expression.orEmpty()}"
    else -> task.kind.ifBlank { "定时" }
}

private val weekdays = mapOf(
    1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日"
)

private fun duration(seconds: Long): String {
    if (seconds <= 0) return "0 秒"
    return when {
        seconds % 86_400 == 0L -> "${seconds / 86_400} 天"
        seconds % 3_600 == 0L -> "${seconds / 3_600} 小时"
        seconds % 60 == 0L -> "${seconds / 60} 分钟"
        else -> "$seconds 秒"
    }
}

private fun shortTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val today = raw.substringBefore('.').let {
        if (it.length >= 5) it.substring(0, 5) else it
    }
    return today
}

private fun formatInstant(raw: String): String? {
    if (raw.isBlank()) return null
    return runCatching {
        DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA)
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(raw))
    }.getOrNull()
}
