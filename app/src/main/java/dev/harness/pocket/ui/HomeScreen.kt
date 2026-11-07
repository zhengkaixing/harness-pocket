package dev.harness.pocket.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.SessionSummary
import dev.harness.pocket.data.WorkspaceInfo
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: AppState,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onPinSession: (SessionSummary) -> Unit,
    onUnpinSession: (SessionSummary) -> Unit,
    onArchiveSession: (SessionSummary) -> Unit,
    onUnarchiveSession: (SessionSummary) -> Unit
) {
    var archivedOpen by remember { mutableStateOf(false) }
    val archivedIds = state.archivedSessionIds.toSet()
    val visible = state.sessions.filterNot { it.sessionId in archivedIds }
    val pinnedIds = state.pinnedSessionIds.toSet()
    val pinned = state.pinnedSessionIds.mapNotNull { id -> visible.firstOrNull { it.sessionId == id } }
    val others = visible.filterNot { it.sessionId in pinnedIds }
    val ordered = pinned + others
    val archived = state.sessions.filter { it.sessionId in archivedIds }

    HarnessBackground {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                HomeTopBar(onMenu = onMenu, onSettings = onSettings)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Hero() }
                    item {
                        WorkspaceBar(
                            selected = state.selectedWorkspace,
                            loading = state.workspacesLoading,
                            onClick = onOpenWorkspace
                        )
                    }
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "最近会话",
                                color = Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(7.dp))
                            if (visible.isNotEmpty()) {
                                Text(
                                    "${visible.size}",
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clickable(enabled = !state.sessionsLoading) { onRefresh() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.sessionsLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Blue,
                                        strokeWidth = 1.8.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        contentDescription = "刷新会话",
                                        tint = Muted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (state.sessionsError != null && state.sessions.isNotEmpty()) {
                        item {
                            InlineError(text = state.sessionsError, onRetry = onRefresh)
                        }
                    }

                    when {
                        state.sessionsLoading && visible.isEmpty() -> {
                            items(4) { SkeletonSessionCard() }
                        }

                        visible.isEmpty() && state.sessionsError != null -> {
                            item {
                                EmptySessions(
                                    title = "同步失败",
                                    subtitle = state.sessionsError,
                                    isError = true,
                                    onRetry = onRefresh
                                )
                            }
                        }

                        visible.isEmpty() -> {
                            item { EmptySessions() }
                        }

                        else -> {
                            items(ordered, key = { it.sessionId }) { session ->
                                val isPinned = session.sessionId in pinnedIds
                                SessionCard(
                                    session = session,
                                    pinned = isPinned,
                                    onClick = { onOpenSession(session) },
                                    onTogglePin = {
                                        if (isPinned) onUnpinSession(session)
                                        else onPinSession(session)
                                    },
                                    onArchive = { onArchiveSession(session) }
                                )
                            }
                        }
                    }

                    if (archived.isNotEmpty()) {
                        item {
                            ArchivedBar(
                                count = archived.size,
                                onClick = { archivedOpen = true }
                            )
                        }
                    }
                }
            }

            HomeComposer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                creating = state.creating,
                onClick = onNewChat
            )
        }
    }

    if (archivedOpen) {
        ArchivedSessionsDialog(
            sessions = archived,
            onDismiss = { archivedOpen = false },
            onRestore = { session ->
                onUnarchiveSession(session)
            }
        )
    }
}

@Composable
private fun WorkspaceBar(
    selected: WorkspaceInfo?,
    loading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color(0xFFF3F6FC), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("工作区", color = Muted, fontSize = 9.5.sp)
                Spacer(Modifier.height(1.dp))
                Text(
                    selected?.path?.takeIf { it.isNotBlank() } ?: "默认工作区（由 Harness 自动选择）",
                    color = Ink,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    color = Blue,
                    strokeWidth = 1.6.dp
                )
            } else {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "选择工作区",
                    tint = Muted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun WorkspaceDialog(
    state: AppState,
    onDismiss: () -> Unit,
    onSelect: (WorkspaceInfo?) -> Unit,
    onRefresh: () -> Unit,
    onCreate: (String) -> Unit
) {
    var path by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("选择工作区", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(Modifier.heightIn(max = 430.dp)) {
                Text(
                    "新建会话会在所选目录里运行。不指定时，由 Harness 使用默认工作区。",
                    color = Muted,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("已有工作区", color = InkSoft, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(26.dp)
                            .clickable(enabled = !state.workspacesLoading) { onRefresh() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "刷新工作区",
                            tint = Muted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                LazyColumn(Modifier.heightIn(max = 210.dp)) {
                    item(key = "default-workspace") {
                        WorkspaceOption(
                            title = "默认工作区",
                            subtitle = "不指定目录，由 Harness 决定",
                            selected = state.selectedWorkspace == null,
                            onClick = { onSelect(null) }
                        )
                    }
                    items(state.workspaces, key = { it.workspaceId }) { workspace ->
                        WorkspaceOption(
                            title = workspace.title.ifBlank {
                                workspace.path.trimEnd('/').substringAfterLast('/')
                            },
                            subtitle = workspace.path,
                            selected = state.selectedWorkspace?.workspaceId == workspace.workspaceId,
                            onClick = { onSelect(workspace) }
                        )
                    }
                }
                if (state.workspacesLoading) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Blue,
                            strokeWidth = 1.7.dp
                        )
                    }
                }
                if (!state.workspacesError.isNullOrBlank()) {
                    Text(
                        state.workspacesError,
                        color = Color(0xFF9F2D3A),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("添加目录", color = InkSoft, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    singleLine = true,
                    placeholder = { Text("/path/to/workspace", color = Muted, fontSize = 12.sp) },
                    colors = harnessFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            onCreate(path.trim())
                            path = ""
                        },
                        enabled = path.isNotBlank() && !state.workspacesLoading
                    ) {
                        Text("添加工作区", color = if (path.isNotBlank()) Blue else Muted)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = Blue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun WorkspaceOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) Color(0xFFF0F5FF) else Color.Transparent
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Ink,
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = Muted,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(onMenu: () -> Unit, onSettings: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        SoftIconButton(
            icon = Icons.Rounded.Menu,
            onClick = onMenu,
            contentDescription = "打开菜单",
            background = Color.Transparent,
            size = 38.dp,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Harness",
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(Green, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(
                    "已连接 · 口袋里的 coding agent",
                    color = Muted,
                    fontSize = 9.5.sp
                )
            }
        }
        SoftIconButton(
            icon = Icons.Rounded.Settings,
            onClick = onSettings,
            contentDescription = "连接设置",
            tint = InkSoft,
            background = Color.White.copy(alpha = 0.7f),
            size = 38.dp,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun Hero() {
    Column(Modifier.padding(top = 26.dp, bottom = 6.dp)) {
        Text(
            text = buildAnnotatedString {
                append("嗨，今天想")
                pushStyle(SpanStyle(color = Blue))
                append("造点什么")
                pop()
                append("？")
            },
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 39.sp,
            letterSpacing = (-0.5).sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "连接你的 Harness，让想法直接跑起来。",
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SessionCard(
    session: SessionSummary,
    pinned: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0x0F171A21))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Color(0xFFF0F4FF), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sessionTitle(session.title, session.blank),
                            color = Ink,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (pinned) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Rounded.PushPin,
                                contentDescription = "已置顶",
                                tint = Blue,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (session.running) {
                            Spacer(Modifier.width(7.dp))
                            RunningBadge(session.lastPromptAt)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = relativeTime(session.updatedAt),
                            color = Muted,
                            fontSize = 11.sp
                        )
                        val folder = cwdLabel(session.cwd)
                        if (folder != null) {
                            Text(" · ", color = Muted, fontSize = 11.sp)
                            Text(
                                text = folder,
                                color = Muted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val tokens = session.usage?.total ?: 0L
                        if (tokens > 0) {
                            Text(" · ", color = Muted, fontSize = 11.sp)
                            Text(
                                text = "${formatTokens(tokens)} tokens",
                                color = Muted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            SessionMoreButton(
                pinned = pinned,
                onTogglePin = onTogglePin,
                onArchive = onArchive,
                tint = Muted,
                size = 30.dp
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC1C7D3),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun RunningBadge(since: Long?) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(since, since != null) {
        if (since != null && since > 0) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
    }
    val suffix = if (since != null && since > 0) {
        " " + formatDuration(now - since)
    } else {
        ""
    }
    Row(
        Modifier
            .background(Color(0xFFEAF8F0), RoundedCornerShape(100.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).background(Green, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(
            "运行中$suffix",
            color = Color(0xFF208A52),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ArchivedBar(count: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Archive,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("已归档", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("$count 个会话", color = Muted, fontSize = 10.sp)
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC1C7D3),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun ArchivedSessionsDialog(
    sessions: List<SessionSummary>,
    onDismiss: () -> Unit,
    onRestore: (SessionSummary) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("已归档会话", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            if (sessions.isEmpty()) {
                Text("暂无已归档会话", color = Muted, fontSize = 12.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 390.dp)) {
                    items(sessions, key = { it.sessionId }) { session ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sessionTitle(session.title, session.blank),
                                    color = Ink,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    relativeTime(session.updatedAt),
                                    color = Muted,
                                    fontSize = 9.5.sp
                                )
                            }
                            TextButton(onClick = { onRestore(session) }) {
                                Text("恢复", color = Blue, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Blue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun SkeletonSessionCard() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(Color(0xFFEDF1F8), RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
                    .background(Color(0xFFEDF1F8), RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(9.dp)
                    .background(Color(0xFFF2F4F9), RoundedCornerShape(5.dp))
            )
        }
    }
}

@Composable
private fun EmptySessions(
    title: String = "还没有会话",
    subtitle: String? = "点下面的输入框，开始第一次对话。",
    isError: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isError) Color(0xFFFFF6F7) else Color.White.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, if (isError) Color(0x22E35D6A) else Line)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 30.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(
                        if (isError) Color(0xFFFFE9EB) else Color(0xFFF0F4FF),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = if (isError) Red else Blue,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "重试",
                    color = Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRetry() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun InlineError(text: String, onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3F4), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = Red,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            color = Color(0xFF9F2D3A),
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "重试",
            color = Color(0xFF9F2D3A),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onRetry() }
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun HomeComposer(modifier: Modifier, creating: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFFF7F9FF), Color(0xFFF7F9FF))
                )
            )
            .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable(enabled = !creating) { onClick() },
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(Color(0xFFF2F5FB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = InkSoft,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "给 Harness 发消息…",
                    color = Muted,
                    fontSize = 14.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(42.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF6C9BFF), Color(0xFF4C7DFF))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (creating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = "开始新对话",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

