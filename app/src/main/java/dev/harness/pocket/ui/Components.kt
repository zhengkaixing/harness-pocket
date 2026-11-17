package dev.harness.pocket.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.harness.pocket.BuildConfig
import dev.harness.pocket.R
import dev.harness.pocket.data.AskAnswer
import dev.harness.pocket.data.AskBatch
import dev.harness.pocket.data.ChatFile
import dev.harness.pocket.data.ChatImage
import dev.harness.pocket.data.NoticeItem
import dev.harness.pocket.data.ToolItem
import dev.harness.pocket.data.TurnTime
import kotlinx.coroutines.delay
import org.json.JSONObject
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
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8F9FD), Color(0xFFF3F6FF), Color(0xFFF7F8FC))
                )
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-100).dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x22566FFF), Color.Transparent)),
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
fun SessionMoreButton(
    pinned: Boolean,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: (() -> Unit)? = null,
    tint: Color = Muted,
    size: Dp = 30.dp
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SoftIconButton(
            icon = Icons.Rounded.MoreVert,
            onClick = { open = true },
            contentDescription = "会话操作",
            tint = tint,
            background = Color.Transparent,
            size = size
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (pinned) "取消置顶" else "置顶会话", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    open = false
                    onTogglePin()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.archive_session), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    open = false
                    onArchive()
                }
            )
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_session_local), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = {
                        open = false
                        onDelete()
                    }
                )
            }
        }
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
fun ChatImageBox(
    image: ChatImage,
    load: suspend (String) -> ChatImage?,
    modifier: Modifier = Modifier,
    gallery: List<ChatImage> = listOf(image),
    galleryIndex: Int = 0
) {
    var payload by remember(image.attachmentId) { mutableStateOf(image.data) }
    var failed by remember(image.attachmentId) { mutableStateOf(false) }
    var reload by remember(image.attachmentId) { mutableStateOf(0) }
    var viewerOpen by remember(image.attachmentId) { mutableStateOf(false) }

    LaunchedEffect(image.attachmentId, reload) {
        if (payload.isNullOrBlank()) {
            failed = false
            payload = try {
                val source = image.source?.trim().orEmpty()
                val sourceIsMedia = source.startsWith("data:", true) ||
                    source.startsWith("http://", true) ||
                    source.startsWith("https://", true) ||
                    source.startsWith("/") ||
                    source.startsWith("\\") ||
                    source.matches(Regex("^[A-Za-z]:[\\\\/].*")) ||
                    source.matches(Regex("^[^:]+\\.[A-Za-z0-9]{2,5}$"))
                val hasAttachment = image.attachmentId.startsWith("sha256:", ignoreCase = true)
                val candidates = buildList {
                    // Tool results have both an attachment (stored by DSH) and a
                    // host path. Prefer the attachment because a remote App can
                    // not read the host's C:\/D:\ paths.
                    if (hasAttachment) add(image.attachmentId)
                    if (sourceIsMedia) add(source)
                    if (!hasAttachment) add(image.attachmentId)
                    if (!sourceIsMedia && source.isNotBlank()) add(source)
                }.filter { it.isNotBlank() }.distinct()
                var loaded: ChatImage? = null
                for (candidate in candidates) {
                    val candidateImage = load(candidate)
                    // A successful HTTP response is not enough: proxies can
                    // return an HTML login page, and a bad attachment payload
                    // can decode to nothing. Only accept real image bytes.
                    if (candidateImage?.data?.let { decodeImageBitmap(it) != null } == true) {
                        loaded = candidateImage
                        break
                    }
                }
                loaded?.data
            } catch (_: Exception) {
                null
            }
            failed = payload.isNullOrBlank()
        }
    }

    val bitmap = remember(payload) { payload?.let { decodeImageBitmap(it) } }
    val cannotShow = bitmap == null && (!payload.isNullOrBlank() || failed)

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = image.name ?: "对话图片",
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { viewerOpen = true },
            contentScale = ContentScale.Fit
        )
        if (viewerOpen) {
            ImageViewerDialog(
                images = gallery.ifEmpty { listOf(image) },
                initialIndex = galleryIndex.coerceIn(0, (gallery.size - 1).coerceAtLeast(0)),
                initialBitmap = bitmap,
                load = load,
                title = image.name ?: "图片",
                onDismiss = { viewerOpen = false }
            )
        }
    } else {
        Box(
            modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1F4FA))
                .clickable(enabled = cannotShow) {
                    payload = null
                    failed = false
                    reload += 1
                },
            contentAlignment = Alignment.Center
        ) {
            if (cannotShow) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.image_load_failed), color = Muted, fontSize = 11.5.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(stringResource(R.string.image_tap_retry), color = Blue, fontSize = 11.sp)
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Blue,
                    strokeWidth = 1.8.dp
                )
            }
        }
    }
}

private fun decodeImageBitmap(raw: String): android.graphics.Bitmap? {
    var text = raw.trim()
    if (text.startsWith("data:", ignoreCase = true)) {
        text = text.substringAfter(',', missingDelimiterValue = "")
    }
    text = text.filterNot { it.isWhitespace() }
    if (text.isBlank()) return null
    val flags = listOf(Base64.DEFAULT, Base64.URL_SAFE or Base64.NO_WRAP)
    for (flag in flags) {
        val bytes = runCatching { Base64.decode(text, flag) }.getOrNull() ?: continue
        val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        if (bitmap != null) return bitmap
    }
    return null
}

@Composable
private fun ImageViewerDialog(
    images: List<ChatImage>,
    initialIndex: Int,
    initialBitmap: android.graphics.Bitmap,
    load: suspend (String) -> ChatImage?,
    title: String,
    onDismiss: () -> Unit
) {
    var index by remember(images) { mutableStateOf(initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))) }
    var current by remember(images) { mutableStateOf<android.graphics.Bitmap?>(initialBitmap) }
    var loading by remember(images) { mutableStateOf(false) }
    var failed by remember(images) { mutableStateOf(false) }
    var loadedIndex by remember(images) { mutableStateOf(initialIndex) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale <= 1f) Offset.Zero else offset + panChange
    }

    LaunchedEffect(index) {
        scale = 1f
        offset = Offset.Zero
        if (index == loadedIndex) return@LaunchedEffect
        loading = true
        failed = false
        val image = images.getOrNull(index)
        val candidate = image?.let {
            val source = it.source?.trim().takeIf { value -> !value.isNullOrBlank() }
            load(source ?: it.attachmentId)
        }
        current = candidate?.data?.let { decodeImageBitmap(it) }
        loadedIndex = index
        failed = current == null
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .transformable(transformState)
        ) {
            val bitmap = current
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Fit
                )
            } else if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.image_load_failed),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .systemBarsPadding()
                    .padding(12.dp)
                    .background(Color(0x99000000), RoundedCornerShape(20.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭图片",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.common_close), color = Color.White, fontSize = 12.sp)
            }
            if (images.size > 1) {
                Text(
                    text = "${index + 1} / ${images.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .systemBarsPadding()
                        .padding(top = 18.dp)
                        .background(Color(0x99000000), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Column(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ViewerPageButton(
                        up = true,
                        enabled = index > 0,
                        onClick = { if (index > 0) index-- }
                    )
                    ViewerPageButton(
                        up = false,
                        enabled = index < images.lastIndex,
                        onClick = { if (index < images.lastIndex) index++ }
                    )
                }
            }
            Text(
                text = if (failed) "点击箭头切换图片" else "双指缩放 · 上下切换",
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
                    .padding(bottom = 22.dp)
            )
        }
    }
}

@Composable
private fun ViewerPageButton(
    up: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(42.dp)
            .background(Color(0x99000000), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (up) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = stringResource(if (up) R.string.image_prev else R.string.image_next),
            tint = if (enabled) Color.White else Color(0x55FFFFFF),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun FileAttachmentChip(
    file: ChatFile,
    onOpen: (ChatFile) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE4E8F0), RoundedCornerShape(14.dp))
            .clickable { onOpen(file) }
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF0F4FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Description,
                contentDescription = null,
                tint = Color(0xFF5B7FDB),
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.name?.substringAfterLast('/')?.substringAfterLast('\\')
                    ?.takeIf { it.isNotBlank() } ?: "附件",
                color = Ink,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    if (file.bytes > 0) append(compactFileSize(file.bytes))
                    if (file.mediaType.isNotBlank() && !file.mediaType.startsWith("application/octet")) {
                        if (isNotEmpty()) append(" · ")
                        append(file.mediaType.removePrefix("image/").uppercase())
                    }
                    if (isEmpty()) append("点击下载")
                },
                color = Muted,
                fontSize = 10.5.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Rounded.Download,
            contentDescription = "下载",
            tint = Color(0xFF4C7DFF),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun compactFileSize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) "${value.toLong()} B" else String.format("%.1f %s", value, units[index])
}

@Composable
fun UserBubble(
    text: String,
    pending: Boolean = false,
    queued: Boolean = false,
    attachments: List<String> = emptyList(),
    images: List<ChatImage> = emptyList(),
    onLoadImage: suspend (String) -> ChatImage? = { null }
) {
    val maxWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            Modifier
                .widthIn(max = maxWidth)
                .alpha(if (pending || queued) 0.72f else 1f),
            horizontalAlignment = Alignment.End
        ) {
            images.forEachIndexed { index, image ->
                if (index > 0) Spacer(Modifier.height(6.dp))
                ChatImageBox(
                    image = image,
                    load = onLoadImage,
                    gallery = images,
                    galleryIndex = index
                )
            }
            if (images.isNotEmpty() && (text.isNotBlank() || attachments.isNotEmpty())) {
                Spacer(Modifier.height(7.dp))
            }
            if (text.isNotBlank() || attachments.isNotEmpty() || pending) {
                Column(
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomEnd = 6.dp,
                                bottomStart = 20.dp
                            )
                        )
                        .background(Color(0xFFE9EEFF))
                        .border(
                            1.dp,
                            Color(0xFFDCE4FF),
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomEnd = 6.dp,
                                bottomStart = 20.dp
                            )
                        )
                        .padding(horizontal = 15.dp, vertical = 11.dp)
                ) {
                    attachments.forEach { name ->
                        Text(
                            text = "📎 $name",
                            color = InkSoft,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    if (text.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = text,
                                color = Ink,
                                fontSize = 15.5.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                    if (pending) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (queued) "排队中…" else "发送中…",
                            color = Muted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantRow(
    text: String,
    images: List<ChatImage> = emptyList(),
    onLoadImage: suspend (String) -> ChatImage? = { null },
    onOpenLink: (String) -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        SparkLogo(26.dp)
        Spacer(Modifier.width(9.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            images.forEachIndexed { index, image ->
                ChatImageBox(
                    image = image,
                    load = onLoadImage,
                    gallery = images,
                    galleryIndex = index
                )
            }
            if (text.isNotBlank()) {
                MarkdownText(
                    text,
                    onLoadImage = onLoadImage,
                    onOpenLink = onOpenLink
                )
            }
        }
    }
}

@Composable
fun TurnMetaRow(item: TurnTime) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 3.dp, bottom = 1.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Schedule,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "用时 ${formatDuration(item.durationMs)}",
            color = Muted,
            fontSize = 10.5.sp
        )
        val tokens = item.usage?.total ?: 0L
        if (tokens > 0) {
            Text(
                text = " · ${formatTokens(tokens)} tokens",
                color = Muted,
                fontSize = 10.5.sp
            )
        }
        if (item.failed) {
            Text(" · 失败", color = Red, fontSize = 10.5.sp)
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
                Text(stringResource(R.string.thinking_hint), color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MarkdownInline(
    text: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    onOpenLink: (String) -> Unit
) {
    val annotated = inlineMarkdown(text)
    ClickableText(
        text = annotated,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset)
                .firstOrNull()
                ?.item
                ?.takeIf { it.isNotBlank() }
                ?.let(onOpenLink)
        }
    )
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    onLoadImage: suspend (String) -> ChatImage? = { null },
    onOpenLink: (String) -> Unit = {}
) {
    val blocks = remember(text) { MarkdownParser.parse(text) }
    val defaultImageName = stringResource(R.string.image_default)
    val markdownImages = remember(blocks, defaultImageName) {
        blocks.filterIsInstance<MdBlock.Image>().map { block ->
            ChatImage(
                attachmentId = block.source,
                name = block.alt.ifBlank { defaultImageName }
            )
        }
    }
    SelectionContainer {
        Column(
            modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Paragraph -> MarkdownInline(
                    text = block.text,
                    color = Ink,
                    fontSize = 15.5.sp,
                    lineHeight = 25.sp,
                    onOpenLink = onOpenLink
                )

                is MdBlock.Heading -> Box(Modifier.padding(top = 2.dp)) {
                    MarkdownInline(
                        text = block.text,
                        color = Ink,
                        fontSize = when (block.level) {
                            1 -> 18.sp
                            2 -> 17.sp
                            else -> 16.sp
                        },
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        onOpenLink = onOpenLink
                    )
                }

                is MdBlock.Bullet -> Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Spacer(Modifier.width((block.depth * 14).dp))
                    Text(
                        "•",
                        color = Blue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        MarkdownInline(
                            text = block.text,
                            color = Ink,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            onOpenLink = onOpenLink
                        )
                    }
                }

                is MdBlock.Ordered -> Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Spacer(Modifier.width((block.depth * 14).dp))
                    Text(
                        "${block.number}.",
                        color = Blue,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .width(22.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.weight(1f)) {
                        MarkdownInline(
                            text = block.text,
                            color = Ink,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            onOpenLink = onOpenLink
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
                    Box(Modifier.padding(vertical = 2.dp)) {
                        MarkdownInline(
                            text = block.text,
                            color = InkSoft,
                            fontSize = 14.5.sp,
                            lineHeight = 23.sp,
                            onOpenLink = onOpenLink
                        )
                    }
                }

                is MdBlock.Image -> {
                    val image = ChatImage(
                        attachmentId = block.source,
                        name = block.alt.ifBlank { stringResource(R.string.image_default) }
                    )
                    ChatImageBox(
                        image = image,
                        load = onLoadImage,
                        gallery = markdownImages,
                        galleryIndex = markdownImages.indexOfFirst {
                            it.attachmentId == image.attachmentId
                        }.coerceAtLeast(0)
                    )
                }

                is MdBlock.Table -> MarkdownTable(block.header, block.rows)

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
}

@Composable
private fun MarkdownTable(header: List<String>, rows: List<List<String>>) {
    val cols = maxOf(header.size, rows.maxOfOrNull { it.size } ?: 0)
    if (cols == 0) return
    val weights = remember(header, rows, cols) {
        (0 until cols).map { column ->
            val longest = maxOf(
                header.getOrNull(column)?.length ?: 0,
                rows.maxOfOrNull { it.getOrNull(column)?.length ?: 0 } ?: 0
            )
            longest.coerceIn(4, 28).toFloat()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5FD))
            ) {
                for (column in 0 until cols) {
                    Box(
                        Modifier
                            .weight(weights[column])
                            .padding(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Text(
                            inlineMarkdown(header.getOrNull(column).orEmpty()),
                            color = Ink,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            rows.forEachIndexed { index, row ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 1) Color(0xFFFAFBFE) else Color.White)
                ) {
                    for (column in 0 until cols) {
                        Box(
                            Modifier
                                .weight(weights[column])
                                .padding(horizontal = 8.dp, vertical = 7.dp)
                        ) {
                            Text(
                                inlineMarkdown(row.getOrNull(column).orEmpty()),
                                color = InkSoft,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
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
                    text = stringResource(if (copied) R.string.common_copied else R.string.common_retry),
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
fun ToolCard(
    item: ToolItem,
    onLoadImage: suspend (String) -> ChatImage? = { null },
    onOpenFile: (ChatFile) -> Unit = {}
) {
    if (item.name == "plugin/card" || item.name == "plugin.card") {
        parsePluginCard(item.input)?.let { card ->
            PluginCardView(card)
            return
        }
    }
    var expanded by remember(item.key) { mutableStateOf(false) }
    val statusColor = when {
        !item.done -> Blue
        item.isError -> Red
        else -> Green
    }
    val summary = toolActivitySummary(item)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (expanded) Color.White else Color.Transparent)
            .then(
                if (expanded) {
                    Modifier.border(BorderStroke(1.dp, Line), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 2.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Terminal,
                contentDescription = null,
                tint = if (item.isError) Red else if (item.done) Muted else Blue,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = summary,
                color = if (item.isError) Red else InkSoft,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!item.done) {
                Spacer(Modifier.width(7.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Blue,
                    strokeWidth = 1.6.dp
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.tool_executing), color = Blue, fontSize = 10.sp)
            } else if (item.isError) {
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.tool_failed), color = Red, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            } else {
                Spacer(Modifier.width(7.dp))
                Box(Modifier.size(5.dp).background(statusColor, CircleShape))
            }
            Spacer(Modifier.width(3.dp))
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.tool_collapse_details else R.string.tool_expand_details),
                tint = Muted,
                modifier = Modifier.size(16.dp)
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
                Text(
                    item.name,
                    color = Muted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.images.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.images.forEachIndexed { index, image ->
                            ChatImageBox(
                                image = image,
                                load = onLoadImage,
                                gallery = item.images,
                                galleryIndex = index
                            )
                        }
                    }
                }
                item.files.forEach { file ->
                    FileAttachmentChip(file = file, onOpen = onOpenFile)
                }
                if (item.input.isNotBlank()) {
                    ToolCode(label = stringResource(R.string.tool_params), text = item.input)
                }
                ToolCode(
                    label = stringResource(if (item.isError) R.string.tool_error else R.string.tool_result),
                    text = item.result?.ifBlank { stringResource(R.string.tool_no_output) } ?: stringResource(R.string.tool_waiting),
                    isError = item.isError
                )
            }
        }
    }
}

@Composable
fun ToolActivityGroupCard(
    items: List<ToolItem>,
    onLoadImage: suspend (String) -> ChatImage? = { null },
    onOpenFile: (ChatFile) -> Unit = {}
) {
    if (items.isEmpty()) return
    var expanded by remember(items.first().key) { mutableStateOf(false) }
    val running = items.any { !it.done }
    val errors = items.any { it.isError }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (expanded) Color.White else Color(0xFFF7F8FB))
            .then(
                if (expanded) {
                    Modifier.border(BorderStroke(1.dp, Line), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (running) Icons.Rounded.Terminal else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (errors) Red else if (running) Blue else Green,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = when {
                    errors -> stringResource(R.string.tool_group_failed) + " · " + stringResource(R.string.tool_calls_count, items.size)
                    running -> stringResource(R.string.tool_group_running) + " · " + stringResource(R.string.tool_calls_count, items.size)
                    else -> stringResource(R.string.tool_group_done) + " · " + stringResource(R.string.tool_calls_count, items.size)
                },
                color = if (errors) Red else InkSoft,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (running) {
                Spacer(Modifier.width(7.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Blue,
                    strokeWidth = 1.6.dp
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.tool_executing), color = Blue, fontSize = 10.sp)
            } else if (errors) {
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.tool_group_failed), color = Red, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(3.dp))
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.tool_collapse_group else R.string.tool_expand_group),
                tint = Muted,
                modifier = Modifier.size(16.dp)
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
                Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { tool ->
                    ToolCard(tool, onLoadImage, onOpenFile)
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
            SelectionContainer {
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
        SelectionContainer {
            Text(
                text = item.text,
                color = Color(0xFF9F2D3A),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun QuestionCard(
    batch: AskBatch,
    busy: Boolean,
    onSubmit: (List<AskAnswer>) -> Unit
) {
    var selections by remember(batch.callId) { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var customs by remember(batch.callId) { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun answers(skip: Boolean): List<AskAnswer> = batch.questions.map { question ->
        if (skip) {
            AskAnswer(id = question.id)
        } else {
            AskAnswer(
                id = question.id,
                selected = selections[question.id].orEmpty().toList(),
                custom = customs[question.id]?.trim()?.takeIf { it.isNotBlank() }
            )
        }
    }

    val canSubmit = batch.questions.all { question ->
        selections[question.id].orEmpty().isNotEmpty() ||
            !customs[question.id].isNullOrBlank()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Blue.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(Blue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = batch.questions.firstOrNull()?.header ?: stringResource(R.string.question_needs_confirm),
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.question_answer_continues),
                        color = Muted,
                        fontSize = 10.5.sp
                    )
                }
            }

            batch.questions.forEachIndexed { index, question ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (batch.questions.size > 1) "${index + 1}. ${question.question}" else question.question,
                    color = Ink,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium
                )
                question.detail?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(it, color = Muted, fontSize = 11.5.sp, lineHeight = 17.sp)
                }
                question.options.forEach { option ->
                    val checked = selections[question.id].orEmpty().contains(option.label)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !busy) {
                                val current = selections[question.id].orEmpty()
                                val next = if (question.multiSelect) {
                                    if (current.contains(option.label)) current - option.label
                                    else current + option.label
                                } else {
                                    if (current.contains(option.label) && current.size == 1) emptySet()
                                    else setOf(option.label)
                                }
                                selections = selections + (question.id to next)
                            }
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = when {
                                question.multiSelect && checked -> Icons.Rounded.CheckBox
                                question.multiSelect -> Icons.Rounded.CheckBoxOutlineBlank
                                checked -> Icons.Rounded.RadioButtonChecked
                                else -> Icons.Rounded.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = if (checked) Blue else Color(0xFFBEC6D4),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(option.label, color = Ink, fontSize = 13.5.sp, lineHeight = 19.sp)
                            option.description?.let {
                                Text(it, color = Muted, fontSize = 10.5.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF6F8FD))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    if (customs[question.id].isNullOrEmpty()) {
                        Text(stringResource(R.string.question_other), color = Muted.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    BasicTextField(
                        value = customs[question.id].orEmpty(),
                        onValueChange = { value ->
                            if (value.length <= 500) customs = customs + (question.id to value)
                        },
                        enabled = !busy,
                        textStyle = TextStyle(color = Ink, fontSize = 12.5.sp, lineHeight = 18.sp),
                        cursorBrush = SolidColor(Blue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onSubmit(answers(skip = true)) },
                    enabled = !busy
                ) {
                    Text(stringResource(R.string.question_skip), color = Muted, fontSize = 12.5.sp)
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (canSubmit && !busy) Blue else Color(0xFFC9D2E3))
                        .clickable(enabled = canSubmit && !busy) {
                            onSubmit(answers(skip = false))
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(if (busy) R.string.question_submitting else R.string.question_submit),
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    baseUrl: String?,
    hosts: List<dev.harness.pocket.data.SavedHost>,
    ntfyTopic: String,
    onSaveNtfy: (String) -> Unit,
    onSwitchHost: (dev.harness.pocket.data.SavedHost) -> Unit,
    onRemoveHost: (String) -> Unit,
    onAddHost: () -> Unit,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(stringResource(R.string.settings_title), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(stringResource(R.string.settings_server), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(baseUrl ?: "-", color = Ink, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.settings_saved_hosts), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                hosts.forEach { host ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(host.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(host.baseUrl, color = Muted, fontSize = 10.5.sp, maxLines = 1)
                        }
                        if (host.baseUrl == baseUrl) {
                            Text(stringResource(R.string.settings_current), color = Blue, fontSize = 10.5.sp)
                        } else {
                            TextButton(onClick = { onSwitchHost(host) }) {
                                Text(stringResource(R.string.settings_switch), color = Blue, fontSize = 11.5.sp)
                            }
                            IconButton(onClick = { onRemoveHost(host.baseUrl) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除主机", tint = Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                TextButton(onClick = onAddHost) {
                    Text(stringResource(R.string.settings_add_host), color = Blue, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.settings_token), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.settings_saved_local), color = Ink, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.settings_ntfy), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                var topic by remember(ntfyTopic) { mutableStateOf(ntfyTopic) }
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.settings_ntfy_hint)) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    colors = harnessFieldColors()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_ntfy_help), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onSaveNtfy(topic) }) {
                        Text(stringResource(R.string.settings_save), color = Blue, fontSize = 11.5.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Line)
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_done), color = Blue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDisconnect) {
                Text(stringResource(R.string.settings_disconnect), color = Red)
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
            Text(stringResource(R.string.app_tagline), color = Muted, fontSize = 13.sp)
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

@Composable
fun toolLabel(name: String): String = when {
    name.contains("pwsh", true) || name.contains("bash", true) || name.contains("shell", true) -> stringResource(R.string.tool_verb_run_command)
    name.contains("read", true) -> stringResource(R.string.tool_verb_read)
    name.contains("write", true) || name.contains("edit", true) || name.contains("patch", true) -> stringResource(R.string.tool_verb_modify)
    name.contains("grep", true) || name.contains("glob", true) || name.contains("search", true) -> stringResource(R.string.tool_verb_search)
    name == "web_search" -> stringResource(R.string.tool_verb_web_search)
    name == "web_fetch" -> stringResource(R.string.tool_verb_fetch)
    name == "present" -> stringResource(R.string.tool_verb_present)
    name.contains("agent", true) -> stringResource(R.string.tool_verb_agent)
    name == "todo_write" -> stringResource(R.string.tool_verb_todo)
    else -> name
}

@Composable
private fun toolActivitySummary(item: ToolItem): String {
    val args = runCatching { JSONObject(item.input) }.getOrNull()
    fun arg(vararg names: String): String? {
        if (args == null) return null
        for (name in names) {
            val value = args.opt(name)
            if (value is String && value.isNotBlank()) return value
        }
        return null
    }

    val file = arg("path", "file_path", "filePath", "file")?.let(::shortFileName)
    val command = arg("description", "command")
        ?.lineSequence()
        ?.firstOrNull { it.isNotBlank() }
        ?.trim()
    val pattern = arg("pattern", "query")
    val url = arg("url")
    val verb = when {
        item.name.contains("pwsh", true) || item.name.contains("bash", true) || item.name.contains("shell", true) -> stringResource(R.string.tool_verb_run)
        item.name.contains("read", true) -> stringResource(R.string.tool_verb_read)
        item.name.contains("write", true) || item.name.contains("edit", true) || item.name.contains("patch", true) -> stringResource(R.string.tool_verb_modify)
        item.name.contains("grep", true) || item.name.contains("search", true) -> stringResource(R.string.tool_verb_search)
        item.name.contains("glob", true) -> stringResource(R.string.tool_verb_find)
        item.name == "web_fetch" -> stringResource(R.string.tool_verb_fetch)
        item.name == "web_search" -> stringResource(R.string.tool_verb_web_search)
        item.name == "present" -> stringResource(R.string.tool_verb_present)
        item.name.contains("agent", true) -> stringResource(R.string.tool_verb_agent)
        item.name == "todo_write" -> stringResource(R.string.tool_verb_todo)
        else -> toolLabel(item.name)
    }
    val detail = when {
        item.name.contains("pwsh", true) || item.name.contains("bash", true) || item.name.contains("shell", true) -> command
        item.name == "web_fetch" -> url
        item.name == "web_search" -> arg("query")
        item.name == "present" -> null
        item.name == "todo_write" -> null
        item.name.contains("agent", true) -> arg("description")
        item.name.contains("read", true) ||
            item.name.contains("write", true) ||
            item.name.contains("edit", true) ||
            item.name.contains("patch", true) -> file
        else -> pattern ?: file
    }
    val suffix = detail?.take(90)?.let { " $it" }.orEmpty()
    val running = if (item.done) "" else stringResource(R.string.tool_running_short)
    return "$running$verb$suffix".trim()
}

private fun shortFileName(raw: String): String {
    val path = raw.replace('\\', '/').trimEnd('/')
    return path.substringAfterLast('/').ifBlank { raw }
}
