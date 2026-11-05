package dev.harness.pocket.ui

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
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.SessionSummary
import androidx.compose.material3.MaterialTheme

private val DrawerBg = Color(0xFFFDFDFB)
private val DrawerDivider = Color(0x0F171A21)

@Composable
fun HarnessDrawer(
    state: AppState,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onHome: () -> Unit,
    onWorkspace: () -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
    onSettings: () -> Unit
) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val active = state.activeSession
    val pinned = active ?: state.sessions.firstOrNull { it.running }
    val recents = state.sessions
        .filter { it.sessionId != pinned?.sessionId }
        .filter { query.isBlank() || sessionTitle(it.title, it.blank).contains(query, ignoreCase = true) }
        .take(9)

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
            DrawerAction(Icons.Rounded.Folder, "工作区") {
                onClose()
                onWorkspace()
            }
            DrawerAction(Icons.Rounded.Home, "会话主页") {
                onClose()
                onHome()
            }
            DrawerAction(Icons.Rounded.Settings, "连接设置") {
                onClose()
                onSettings()
            }

            Divider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = DrawerDivider
            )

            if (pinned != null) {
                SectionLabel("置顶")
                DrawerSessionRow(
                    session = pinned,
                    selected = pinned.sessionId == active?.sessionId,
                    onClick = {
                        onClose()
                        onOpenSession(pinned)
                    }
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("最近", modifier = Modifier.padding(0.dp))
                Spacer(Modifier.weight(1f))
                if (state.sessionsLoading) {
                    Text("同步中", color = Muted, fontSize = 10.sp)
                }
            }

            if (recents.isEmpty()) {
                Text(
                    if (query.isBlank()) "还没有会话" else "没有匹配的会话",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                recents.forEach { session ->
                    DrawerSessionRow(
                        session = session,
                        selected = session.sessionId == active?.sessionId,
                        onClick = {
                            onClose()
                            onOpenSession(session)
                        }
                    )
                }
            }

            if (state.sessions.size > recents.size + if (pinned != null) 1 else 0) {
                Text(
                    "查看全部 ${state.sessions.size} 个会话",
                    color = Blue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClose()
                            onHome()
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
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
                        if (state.connection != null) "已连接 · ${state.sessions.size} 个会话" else "未连接",
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
private fun DrawerSessionRow(
    session: SessionSummary,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFF0F4FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        if (session.running) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(Green, CircleShape)
            )
        }
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
