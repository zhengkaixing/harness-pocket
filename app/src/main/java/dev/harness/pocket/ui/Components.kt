package dev.harness.pocket.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.NoticeItem
import dev.harness.pocket.data.ToolItem
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HarnessBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WarmWhite, Color(0xFFF6F8FF))))
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-100).dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x335B8CFF), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-110).dp, y = 90.dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x22FFB98A), Color.Transparent)),
                    CircleShape
                )
        )
        content()
    }
}

@Composable
fun SparkLogo(size: Dp = 36.dp, withShadow: Boolean = false) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        Modifier
            .size(size)
            .shadow(
                elevation = if (withShadow) 18.dp else 0.dp,
                shape = shape,
                ambientColor = Blue.copy(alpha = 0.22f),
                spotColor = Blue.copy(alpha = 0.38f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF7EA8FF), Color(0xFF5551F5)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        ambientColor = Blue.copy(alpha = 0.24f),
                        spotColor = Blue.copy(alpha = 0.36f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(Color(0xFF6C9BFF), Color(0xFF4C7DFF)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFDEE3ED), Color(0xFFDEE3ED)))
                }
            )
            .clickable(enabled = enabled && !loading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SoftIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String? = null,
    tint: Color = InkSoft,
    background: Color = Color.White,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun StatusPill(text: String, onClick: () -> Unit, connected: Boolean = true) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = Color.White.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(if (connected) Green else Amber, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                color = InkSoft,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TypingDots(color: Color = Muted) {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            val wave = (sin((phase * 2f * PI + index * 1.15f).toDouble()) + 1.0) / 2.0
            Box(
                Modifier
                    .size(5.dp)
                    .background(color.copy(alpha = (0.25f + 0.75f * wave).toFloat()), CircleShape)
            )
        }
    }
}

@Composable
fun UserBubble(
    text: String,
    pending: Boolean = false,
    attachments: List<String> = emptyList()
) {
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            Modifier
                .widthIn(max = maxWidth)
                .alpha(if (pending) 0.72f else 1f)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = 6.dp,
                        bottomStart = 20.dp
                    )
                )
                .background(
                    Brush.linearGradient(listOf(Color(0xFF6C9BFF), Color(0xFF4C7DFF)))
                )
                .padding(horizontal = 15.dp, vertical = 11.dp)
        ) {
            attachments.forEach { name ->
                Text(
                    text = "📎 $name",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    lineHeight = 24.sp
                )
            }
            if (pending) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "发送中…",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun AssistantRow(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        SparkLogo(26.dp)
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            MarkdownText(text)
        }
    }
}

@Composable
fun TypingRow() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SparkLogo(26.dp)
        Spacer(Modifier.width(9.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDots()
                Spacer(Modifier.width(9.dp))
                Text("Harness 正在思考", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { MarkdownParser.parse(text) }
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> Text(
                    text = inlineMarkdown(block.text),
                    color = Ink,
                    fontSize = 15.5.sp,
                    lineHeight = 25.sp
                )

                is MdBlock.Heading -> Text(
                    text = inlineMarkdown(block.text),
                    color = Ink,
                    fontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 17.sp
                        else -> 16.sp
                    },
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                is MdBlock.Bullet -> Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "•",
                        color = Blue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        Text(
                            inlineMarkdown(block.text),
                            color = Ink,
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )
                    }
                }

                is MdBlock.Quote -> Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Blue.copy(alpha = 0.35f))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        inlineMarkdown(block.text),
                        color = InkSoft,
                        fontSize = 14.5.sp,
                        lineHeight = 23.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                is MdBlock.Code -> CodeBlock(block.code, block.language)
                MdBlock.Divider -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line)
                )
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, language: String?) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CodeBg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF20242F))
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language?.lowercase() ?: "code",
                color = Color(0xFF9AA6BC),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    tint = if (copied) Green else Color(0xFF9AA6BC),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (copied) "已复制" else "复制",
                    color = if (copied) Green else Color(0xFF9AA6BC),
                    fontSize = 10.sp
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                color = Color(0xFFE7EBF3),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun ToolCard(item: ToolItem) {
    var expanded by remember(item.key) { mutableStateOf(item.isError) }
    LaunchedEffect(item.isError) {
        if (item.isError) expanded = true
    }

    val status = when {
        !item.done -> "执行中"
        item.isError -> "失败"
        else -> "完成"
    }
    val statusColor = when {
        !item.done -> Blue
        item.isError -> Red
        else -> Green
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F4FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Terminal,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = toolLabel(item.name),
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.name,
                        color = Muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!item.done) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        color = Blue,
                        strokeWidth = 1.8.dp
                    )
                    Spacer(Modifier.width(7.dp))
                } else {
                    Box(Modifier.size(6.dp).background(statusColor, CircleShape))
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = status,
                    color = statusColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (expanded) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line)
                )
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.input.isNotBlank()) {
                        ToolCode(label = "参数", text = item.input)
                    }
                    ToolCode(
                        label = if (item.isError) "错误" else "结果",
                        text = item.result?.ifBlank { "没有输出" } ?: "等待结果…",
                        isError = item.isError
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolCode(label: String, text: String, isError: Boolean = false) {
    Column {
        Text(
            text = label,
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isError) Color(0xFFFFF5F6) else Color(0xFFF6F7FA))
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                text = text.take(12000),
                color = if (isError) Color(0xFF9F2D3A) else Color(0xFF394154),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun NoticeCard(item: NoticeItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF3F4))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = Red,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.text,
            color = Color(0xFF9F2D3A),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun SettingsDialog(
    baseUrl: String?,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("连接设置", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("服务器", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(baseUrl ?: "-", color = Ink, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Text("访问令牌", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("已保存 · 仅本机可见", color = Ink, fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line)
                )
                Spacer(Modifier.height(12.dp))
                Text("Harness Pocket 0.1.0", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = Blue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDisconnect) {
                Text("断开连接", color = Red)
            }
        }
    )
}

@Composable
fun SplashScreen() {
    HarnessBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SparkLogo(78.dp, withShadow = true)
            Spacer(Modifier.height(20.dp))
            Text("Harness", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("口袋里的 coding agent", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(30.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Blue,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun harnessFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = Blue,
    focusedBorderColor = Blue,
    unfocusedBorderColor = Line,
    focusedLabelColor = Blue,
    unfocusedLabelColor = Muted,
    focusedLeadingIconColor = Blue,
    unfocusedLeadingIconColor = Muted,
    focusedTrailingIconColor = Muted,
    unfocusedTrailingIconColor = Muted
)

fun toolLabel(name: String): String = when {
    name.contains("pwsh", true) || name.contains("bash", true) || name.contains("shell", true) -> "运行命令"
    name.contains("read", true) -> "读取文件"
    name.contains("write", true) || name.contains("edit", true) || name.contains("patch", true) -> "修改文件"
    name.contains("grep", true) || name.contains("glob", true) || name.contains("search", true) -> "搜索代码"
    name == "web_search" -> "联网搜索"
    name == "web_fetch" -> "读取网页"
    name == "present" -> "生成文件"
    name.contains("agent", true) -> "调用子代理"
    name == "todo_write" -> "更新计划"
    else -> name
}
