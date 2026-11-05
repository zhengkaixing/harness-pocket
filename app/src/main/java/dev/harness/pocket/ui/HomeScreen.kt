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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun HomeScreen(
    state: AppState,
    onNewChat: () -> Unit,
    onOpenSession: (SessionSummary) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    HarnessBackground {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                HomeTopBar(onSettings)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Hero() }
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
                            if (state.sessions.isNotEmpty()) {
                                Text(
                                    "${state.sessions.size}",
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
                        state.sessionsLoading && state.sessions.isEmpty() -> {
                            items(4) { SkeletonSessionCard() }
                        }

                        state.sessions.isEmpty() && state.sessionsError != null -> {
                            item {
                                EmptySessions(
                                    title = "同步失败",
                                    subtitle = state.sessionsError,
                                    isError = true,
                                    onRetry = onRefresh
                                )
                            }
                        }

                        state.sessions.isEmpty() -> {
                            item { EmptySessions() }
                        }

                        else -> {
                            items(state.sessions, key = { it.sessionId }) { session ->
                                SessionCard(session = session, onClick = { onOpenSession(session) })
                            }
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
}

@Composable
private fun HomeTopBar(onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SparkLogo(34.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Harness",
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            )
            Text(
                "口袋里的 coding agent",
                color = Muted,
                fontSize = 10.5.sp
            )
        }
        Spacer(Modifier.weight(1f))
        StatusPill("已连接", onClick = onSettings)
        Spacer(Modifier.width(8.dp))
        SoftIconButton(
            icon = Icons.Rounded.Settings,
            onClick = onSettings,
            contentDescription = "连接设置",
            tint = InkSoft,
            background = Color.White.copy(alpha = 0.8f),
            size = 36.dp
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
private fun SessionCard(session: SessionSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0x0F171A21))
    ) {
        Row(
            Modifier.padding(14.dp),
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
                    if (session.running) {
                        Spacer(Modifier.width(7.dp))
                        RunningBadge()
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
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC1C7D3),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RunningBadge() {
    Row(
        Modifier
            .background(Color(0xFFEAF8F0), RoundedCornerShape(100.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).background(Green, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text("运行中", color = Color(0xFF208A52), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
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
