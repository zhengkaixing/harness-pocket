package dev.harness.pocket.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.SessionSummary
import dev.harness.pocket.data.WorkspaceInfo
import androidx.compose.material3.MaterialTheme

private val DrawerBg = Color(0xFFFDFDFB)
private val DrawerDivider = Color(0x0F171A21)
private val TreeLine = Color(0x24171A21)

@Composable
fun HarnessDrawer(
    state: AppState,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onWorkspace: () -> Unit,
    onMoveWorkspace: (String, String?) -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
    onSettings: () -> Unit,
    onResources: () -> Unit,
    onPinSession: (SessionSummary) -> Unit,
    onUnpinSession: (SessionSummary) -> Unit,
    onArchiveSession: (SessionSummary) -> Unit
) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val active = state.activeSession
    val archivedIds = state.archivedSessionIds.toSet()
    val visible = state.sessions.filterNot { it.sessionId in archivedIds }
    val pinnedSessions = state.pinnedSessionIds.mapNotNull { id ->
        visible.firstOrNull { it.sessionId == id }
    }
    val pinnedIds = pinnedSessions.map { it.sessionId }.toSet()
    val treeSessions = (if (query.isBlank()) visible.filterNot { it.sessionId in pinnedIds } else visible)
        .filter { query.isBlank() || sessionTitle(it.title, it.blank).contains(query, ignoreCase = true) }
    val workspaceOrder = state.workspaces.map { it.workspaceId }
    val groups = groupSessions(
        workspaces = state.workspaces,
        sessions = treeSessions,
        allSessions = visible,
        includeEmpty = query.isBlank()
    )

    ModalDrawerSheet(
        modifier = Modifier
            .width(312.dp)
            .fillMaxHeight(),
        drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
        drawerContainerColor = DrawerBg,
        drawerContentColor = Ink,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Harness",
                    color = Ink,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.weight(1f))
                SoftIconButton(
                    icon = Icons.Rounded.Search,
                    onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    },
                    contentDescription = "搜索会话",
                    background = Color.Transparent,
                    size = 38.dp
                )
            }

            if (searchOpen) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        if (query.isEmpty()) {
                            Text("搜索会话", color = Muted, fontSize = 13.sp)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 13.sp),
                            cursorBrush = SolidColor(Blue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            DrawerAction(Icons.Rounded.Add, "新对话") {
                onClose()
                onNewChat()
            }
            DrawerAction(Icons.Rounded.Memory, "资源与并发") {
                onClose()
                onResources()
            }
            DrawerAction(Icons.Rounded.Settings, "连接设置") {
                onClose()
                onSettings()
            }

            Divider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = DrawerDivider
            )

            if (pinnedSessions.isNotEmpty()) {
                SectionLabel("置顶")
                pinnedSessions.take(4).forEach { session ->
                    DrawerSessionRow(
                        session = session,
                        selected = session.sessionId == active?.sessionId,
                        pinned = true,
                        onClick = {
                            onClose()
                            onOpenSession(session)
                        },
                        onTogglePin = { onUnpinSession(session) },
                        onArchive = { onArchiveSession(session) }
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("工作区", modifier = Modifier.padding(0.dp))
                Spacer(Modifier.weight(1f))
                if (state.sessionsLoading || state.workspacesLoading) {
                    Text("同步中", color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.width(4.dp))
                SoftIconButton(
                    icon = Icons.Rounded.Add,
                    onClick = {
                        onClose()
                        onWorkspace()
                    },
                    contentDescription = "管理工作区",
                    background = Color.Transparent,
                    size = 28.dp
                )
            }

            if (groups.isEmpty()) {
                Text(
                    if (query.isBlank()) "还没有会话" else "没有匹配的会话",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                groups.forEach { group ->
                    val key = group.workspace.workspaceId
                    val open = query.isNotBlank() || expanded[key] == true
                    val registryIndex = workspaceOrder.indexOf(key)
                    DrawerWorkspaceHeader(
                        group = group,
                        open = open,
                        movable = registryIndex >= 0,
                        canMoveUp = registryIndex > 0,
                        canMoveDown = registryIndex >= 0 && registryIndex < workspaceOrder.lastIndex,
                        onMoveUp = {
                            if (registryIndex > 0) {
                                onMoveWorkspace(key, workspaceOrder[registryIndex - 1])
                            }
                        },
                        onMoveDown = {
                            if (registryIndex >= 0 && registryIndex < workspaceOrder.lastIndex) {
                                onMoveWorkspace(key, workspaceOrder.getOrNull(registryIndex + 2))
                            }
                        },
                        onClick = { expanded[key] = !open }
                    )
                    if (open) {
                        group.sessions.forEachIndexed { index, session ->
                            DrawerSessionRow(
                                session = session,
                                selected = session.sessionId == active?.sessionId,
                                pinned = false,
                                onClick = {
                                    onClose()
                                    onOpenSession(session)
                                },
                                onTogglePin = { onPinSession(session) },
                                onArchive = { onArchiveSession(session) },
                                indent = 16.dp,
                                tree = true,
                                last = index == group.sessions.lastIndex
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Divider(color = DrawerDivider)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF4C7DFF))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "H",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Harness Pocket",
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (state.connection != null) "已连接 · ${visible.size} 个会话" else "未连接",
                        color = Muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SoftIconButton(
                    icon = Icons.Rounded.Settings,
                    onClick = {
                        onClose()
                        onSettings()
                    },
                    contentDescription = "设置",
                    background = Color.Transparent,
                    size = 34.dp
                )
            }
        }
    }
}

@Composable
private fun DrawerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier.padding(start = 20.dp, end = 20.dp)) {
    Text(
        text,
        color = Muted,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun DrawerWorkspaceHeader(
    group: WorkspaceGroup,
    open: Boolean,
    movable: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onClick: () -> Unit
) {
    var moveMenu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .background(Color(0xFFEFF4FF), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Folder,
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                group.workspace.title.ifBlank {
                    cwdLabel(group.workspace.path) ?: "工作区"
                },
                color = Ink,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (group.workspace.path.isNotBlank()) {
                Text(
                    group.workspace.path,
                    color = Muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (group.runningCount > 0) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFFE8F8EF)
            ) {
                Row(
                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .background(Green, CircleShape)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${group.runningCount} 运行中",
                        color = Green,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(
            "${group.sessions.size}",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
        if (movable) {
            Box {
                SoftIconButton(
                    icon = Icons.Rounded.DragHandle,
                    onClick = { moveMenu = true },
                    contentDescription = "调整工作区顺序",
                    background = Color.Transparent,
                    size = 26.dp
                )
                DropdownMenu(
                    expanded = moveMenu,
                    onDismissRequest = { moveMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("上移", fontSize = 13.sp) },
                        enabled = canMoveUp,
                        onClick = {
                            moveMenu = false
                            onMoveUp()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("下移", fontSize = 13.sp) },
                        enabled = canMoveDown,
                        onClick = {
                            moveMenu = false
                            onMoveDown()
                        }
                    )
                }
            }
        }
        Icon(
            if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (open) "收起工作区" else "展开工作区",
            tint = Muted,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun TreeGuide(last: Boolean) {
    Canvas(Modifier.width(13.dp).height(26.dp)) {
        val x = size.width * 0.3f
        val mid = size.height / 2f
        drawLine(
            TreeLine,
            Offset(x, 0f),
            Offset(x, if (last) mid else size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            TreeLine,
            Offset(x, mid),
            Offset(size.width, mid),
            strokeWidth = 1.dp.toPx()
        )
    }
    Spacer(Modifier.width(2.dp))
}

@Composable
private fun DrawerSessionRow(
    session: SessionSummary,
    selected: Boolean,
    pinned: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    indent: Dp = 0.dp,
    tree: Boolean = false,
    last: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp + indent, end = 10.dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFF0F4FF) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tree) {
                TreeGuide(last = last)
            }
            SessionDot(session.sessionId)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sessionTitle(session.title, session.blank),
                    color = if (selected) Color(0xFF315BAE) else Ink,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(relativeTime(session.updatedAt), color = Muted, fontSize = 9.5.sp)
                    val folder = cwdLabel(session.cwd)
                    if (folder != null) {
                        Text(" · $folder", color = Muted, fontSize = 9.5.sp, maxLines = 1)
                    }
                }
            }
        }
        if (pinned) {
            Icon(
                Icons.Rounded.PushPin,
                contentDescription = null,
                tint = Blue,
                modifier = Modifier.size(12.dp)
            )
        }
        if (session.running) {
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier
                    .size(6.dp)
                    .background(Green, CircleShape)
            )
        }
        SessionMoreButton(
            pinned = pinned,
            onTogglePin = onTogglePin,
            onArchive = onArchive,
            tint = Muted,
            size = 27.dp
        )
    }
}

@Composable
private fun SessionDot(seed: String) {
    val colors = listOf(
        Color(0xFFEAF1FF) to Color(0xFF4C7DFF),
        Color(0xFFEAF8F0) to Color(0xFF208A52),
        Color(0xFFFFF4E5) to Color(0xFFC98117),
        Color(0xFFF3EEFF) to Color(0xFF7B61FF)
    )
    val pair = colors[(seed.hashCode() and Int.MAX_VALUE) % colors.size]
    Box(
        Modifier
            .size(27.dp)
            .background(pair.first, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.ChatBubbleOutline,
            contentDescription = null,
            tint = pair.second,
            modifier = Modifier.size(14.dp)
        )
    }
}

private data class WorkspaceGroup(
    val workspace: WorkspaceInfo,
    val sessions: List<SessionSummary>,
    val runningCount: Int = 0
)

private fun groupSessions(
    workspaces: List<WorkspaceInfo>,
    sessions: List<SessionSummary>,
    allSessions: List<SessionSummary>,
    includeEmpty: Boolean
): List<WorkspaceGroup> {
    return workspaces.mapNotNull { workspace ->
        val ids = workspace.sessionIds.toSet()
        val items = if (ids.isEmpty()) {
            sessions.filter { cwdKey(it.cwd) == cwdKey(workspace.path) }
        } else {
            sessions.filter { it.sessionId in ids }
        }
        if (items.isEmpty() && !includeEmpty) return@mapNotNull null
        buildGroup(workspace, items, allSessions)
    }
}

private fun buildGroup(
    workspace: WorkspaceInfo,
    sessions: List<SessionSummary>,
    allSessions: List<SessionSummary>
): WorkspaceGroup {
    val sorted = sessions.sortedByDescending { it.updatedAt }
    val ids = workspace.sessionIds.toSet()
    val running = if (ids.isEmpty()) {
        allSessions.count { it.running && cwdKey(it.cwd) == cwdKey(workspace.path) }
    } else {
        allSessions.count { it.running && it.sessionId in ids }
    }
    return WorkspaceGroup(workspace, sorted, running)
}

private fun cwdKey(path: String?): String =
    path?.trim()?.trimEnd('\\', '/')?.lowercase().orEmpty()


