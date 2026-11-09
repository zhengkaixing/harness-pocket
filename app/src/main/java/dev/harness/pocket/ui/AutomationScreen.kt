package dev.harness.pocket.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.AutomationChange
import dev.harness.pocket.data.AutomationTask
import dev.harness.pocket.data.SessionSummary
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val ActiveGreen = Color(0xFF168A55)
private val ActiveGreenBg = Color(0xFFEAF7F0)
private val DeleteRedBg = Color(0xFFE5484D)

@Composable
fun AutomationScreen(
    state: AppState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
    onDelete: (AutomationTask) -> Unit,
    onUpdate: (AutomationTask, AutomationChange) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<AutomationTask?>(null) }
    val selected = state.automations.firstOrNull { it.id == selectedId }

    BackHandler {
        if (selected != null) selectedId = null else onBack()
    }

    HarnessBackground {
        if (selected != null) {
            AutomationDetail(
                task = selected,
                session = state.sessions.firstOrNull { it.sessionId == selected.sessionId },
                busy = state.automationsBusyId == selected.id,
                actionError = state.automationsActionError,
                onBack = { selectedId = null },
                onOpenSession = onOpenSession,
                onUpdate = onUpdate,
                onDelete = { pendingDelete = selected }
            )
        } else {
            AutomationList(
                state = state,
                selectedId = selectedId,
                onSelected = { selectedId = it.id },
                onBack = onBack,
                onRefresh = onRefresh,
                onDeleteRequest = { pendingDelete = it }
            )
        }
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("删除自动化任务", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "删除后任务会停止，且无法恢复。关联会话和已发送的消息不会删除。",
                    color = InkSoft,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(task)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Red)
                ) {
                    Text("删除", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消", color = Muted)
                }
            }
        )
    }
}

@Composable
private fun AutomationList(
    state: AppState,
    selectedId: String?,
    onSelected: (AutomationTask) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteRequest: (AutomationTask) -> Unit
) {
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
                Text("由会话中的定时提醒创建", color = Muted, fontSize = 10.5.sp)
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
                    state.automationsActionError?.let { error ->
                        item(key = "action-error") {
                            ActionErrorText(error)
                        }
                    }
                    items(state.automations, key = { it.id }) { task ->
                        val session = state.sessions.firstOrNull {
                            it.sessionId == task.sessionId
                        }
                        SwipeableTaskCard(
                            task = task,
                            session = session,
                            selected = task.id == selectedId,
                            busy = state.automationsBusyId == task.id,
                            onOpen = { onSelected(task) },
                            onDeleteRequest = { onDeleteRequest(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableTaskCard(
    task: AutomationTask,
    session: SessionSummary?,
    selected: Boolean,
    busy: Boolean,
    onOpen: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val density = LocalDensity.current
    val maxDrag = with(density) { 104.dp.toPx() }
    var drag by remember(task.id) { mutableStateOf(0f) }
    val offset by animateFloatAsState(drag, label = "automation-swipe")

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
    ) {
        Row(
            Modifier
                .matchParentSize()
                .background(DeleteRedBg)
                .padding(start = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("删除", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .pointerInput(task.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (drag > maxDrag * 0.55f) onDeleteRequest()
                            drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            drag = (drag + amount).coerceIn(0f, maxDrag)
                        }
                    )
                }
        ) {
            TaskCard(
                task = task,
                session = session,
                selected = selected,
                busy = busy,
                onOpen = onOpen
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: AutomationTask,
    session: SessionSummary?,
    selected: Boolean,
    busy: Boolean,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (selected) Blue else Line)
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
                                .background(if (task.active) ActiveGreen else Color(0xFF98A0AB))
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
                if (busy) {
                    CircularProgressIndicator(
                        Modifier.size(14.dp),
                        color = Blue,
                        strokeWidth = 1.7.dp
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    nextRunLabel(task),
                    color = if (task.active) InkSoft else Muted,
                    fontSize = 10.5.sp
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "查看任务详情",
                    tint = Muted,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(16.dp)
                )
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
private fun AutomationDetail(
    task: AutomationTask,
    session: SessionSummary?,
    busy: Boolean,
    actionError: String?,
    onBack: () -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
    onUpdate: (AutomationTask, AutomationChange) -> Unit,
    onDelete: () -> Unit
) {
    var showRepeat by remember { mutableStateOf(false) }
    var showZones by remember { mutableStateOf(false) }
    val editable = task.active && !busy && task.kind in setOf("daily", "weekly", "every", "cron")

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
                Text("任务详情", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("规则与关联会话", color = Muted, fontSize = 10.5.sp)
            }
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = Blue,
                    strokeWidth = 2.dp
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Line)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (task.active) ActiveGreenBg else Color(0xFFF1F2F4)
                        ) {
                            Text(
                                if (task.active) "已开启" else "未运行",
                                color = if (task.active) ActiveGreen else Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            nextRunLabel(task),
                            color = if (task.active) InkSoft else Muted,
                            fontSize = 10.5.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        task.title,
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 27.sp
                    )
                    if (task.prompt.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "任务内容",
                            color = Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            task.prompt,
                            color = InkSoft,
                            fontSize = 13.sp,
                            lineHeight = 21.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("运行时间", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Line)
            ) {
                Column {
                    DetailRow(
                        icon = Icons.Rounded.Repeat,
                        label = "重复",
                        value = ruleLabel(task),
                        enabled = editable,
                        onClick = { showRepeat = true }
                    )
                    DetailDivider()
                    if (task.kind in setOf("daily", "weekly", "cron")) {
                        DetailRow(
                            icon = Icons.Rounded.Public,
                            label = "时区",
                            value = timeZoneLabel(task.timeZone ?: ZoneId.systemDefault().id),
                            enabled = editable,
                            onClick = { showZones = true }
                        )
                        DetailDivider()
                    }
                    DetailRow(
                        icon = Icons.Rounded.AccessTime,
                        label = "下次运行",
                        value = nextRunLabel(task).removePrefix("下次 "),
                        enabled = false,
                        onClick = {}
                    )
                }
            }
            if (!task.active) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "任务已结束，不能再修改重复或时区。",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            if (task.kind in setOf("at", "after")) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "单次任务没有可修改的重复规则。",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = session != null) {
                        session?.let(onOpenSession)
                    },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("关联会话", color = Muted, fontSize = 10.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            session?.title?.takeIf { it.isNotBlank() } ?: "关联会话不可用",
                            color = if (session != null) Ink else Muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (session != null) {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "进入关联会话",
                            tint = Muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (actionError != null) {
                Spacer(Modifier.height(10.dp))
                ActionErrorText(actionError)
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !busy, onClick = onDelete)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = Red,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("删除任务", color = Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showRepeat) {
        AlertDialog(
            onDismissRequest = { showRepeat = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("修改重复", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    repeatChoices(task).forEach { (label, change) ->
                        Text(
                            label,
                            color = Ink,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !busy) {
                                    showRepeat = false
                                    onUpdate(task, change)
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeat = false }) {
                    Text("取消", color = Muted)
                }
            }
        )
    }

    if (showZones) {
        AlertDialog(
            onDismissRequest = { showZones = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("修改时区", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    timeZoneChoices(task).forEach { (zone, label) ->
                        Text(
                            label,
                            color = if (zone == task.timeZone) Blue else Ink,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !busy) {
                                    showZones = false
                                    if (zone != task.timeZone) {
                                        onUpdate(task, timeZoneChange(task, zone))
                                    }
                                }
                                .padding(vertical = 11.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showZones = false }) {
                    Text("取消", color = Muted)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(58.dp))
        Text(
            value,
            color = if (enabled) Ink else InkSoft,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (enabled) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DetailDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 39.dp)
            .height(1.dp)
            .background(Line.copy(alpha = 0.7f))
    )
}

@Composable
private fun ActionErrorText(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF1F2),
        border = BorderStroke(1.dp, Color(0xFFF6C9CE))
    ) {
        Text(
            message,
            color = Red,
            fontSize = 11.5.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        )
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
                textAlign = TextAlign.Center
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

private fun repeatChoices(task: AutomationTask): List<Pair<String, AutomationChange>> {
    val time = taskTime(task)
    val zone = task.timeZone ?: ZoneId.systemDefault().id
    val days = task.weekdays.ifEmpty { listOf(taskWeekday(task)) }
    return listOf(
        "每天" to AutomationChange("daily", time = time, timeZone = zone),
        "每周" to AutomationChange("weekly", time = time, timeZone = zone, weekdays = days),
        "每小时" to AutomationChange("every", everySeconds = 3_600),
        "每 6 小时" to AutomationChange("every", everySeconds = 21_600),
        "每 12 小时" to AutomationChange("every", everySeconds = 43_200),
        "每 24 小时" to AutomationChange("every", everySeconds = 86_400)
    )
}

private fun timeZoneChoices(task: AutomationTask): List<Pair<String, String>> {
    val system = ZoneId.systemDefault().id
    val zones = buildList {
        add(system)
        task.timeZone?.takeIf { it.isNotBlank() }?.let(::add)
        add("Asia/Shanghai")
        add("Asia/Tokyo")
        add("Asia/Singapore")
        add("Europe/London")
        add("America/New_York")
        add("UTC")
    }.distinct()
    return zones.map { zone ->
        val suffix = if (zone == system) " · 系统" else ""
        zone to "${timeZoneLabel(zone)}$suffix"
    }
}

private fun timeZoneChange(task: AutomationTask, zone: String): AutomationChange = when (task.kind) {
    "daily" -> AutomationChange("daily", time = taskTime(task), timeZone = zone)
    "weekly" -> AutomationChange(
        "weekly",
        time = taskTime(task),
        timeZone = zone,
        weekdays = task.weekdays.ifEmpty { listOf(taskWeekday(task)) }
    )
    "cron" -> AutomationChange(
        "cron",
        timeZone = zone,
        expression = task.expression ?: "0 8 * * *"
    )
    else -> AutomationChange(task.kind, timeZone = zone)
}

private fun taskTime(task: AutomationTask): String {
    task.time?.takeIf { it.isNotBlank() }?.let { return it }
    return runCatching {
        Instant.parse(task.scheduledAt)
            .atZone(ZoneId.of(task.timeZone ?: ZoneId.systemDefault().id))
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }.getOrDefault("08:00:00")
}

private fun taskWeekday(task: AutomationTask): Int = runCatching {
    Instant.parse(task.scheduledAt)
        .atZone(ZoneId.of(task.timeZone ?: ZoneId.systemDefault().id))
        .dayOfWeek
        .value
}.getOrDefault(1)

private fun timeZoneLabel(id: String): String = runCatching {
    val zone = ZoneId.of(id)
    val offset = ZonedDateTime.now(zone).offset.id.replace("Z", "+00:00")
    val city = when (id) {
        "Asia/Shanghai" -> "上海"
        "Asia/Tokyo" -> "东京"
        "Asia/Singapore" -> "新加坡"
        "Europe/London" -> "伦敦"
        "America/New_York" -> "纽约"
        "UTC" -> "UTC"
        else -> id.substringAfterLast('/').replace('_', ' ')
    }
    "$city · UTC$offset"
}.getOrDefault(id)

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