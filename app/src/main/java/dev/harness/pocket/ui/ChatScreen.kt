package dev.harness.pocket.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.harness.pocket.data.AiModel
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.AskAnswer
import dev.harness.pocket.data.AssistantMessage
import dev.harness.pocket.data.Attachment
import dev.harness.pocket.data.ChatImage
import dev.harness.pocket.data.ChatItem
import dev.harness.pocket.data.ChatState
import dev.harness.pocket.data.ContextPressure
import dev.harness.pocket.data.ModelCatalog
import dev.harness.pocket.data.ModelChoice
import dev.harness.pocket.data.NoticeItem
import dev.harness.pocket.data.PermissionCatalog
import dev.harness.pocket.data.TokenUsage
import dev.harness.pocket.data.ToolItem
import dev.harness.pocket.data.TurnStart
import dev.harness.pocket.data.UserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatScreen(
    state: AppState,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRename: (String) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onModel: (ModelChoice) -> Unit,
    onPermission: (String) -> Unit,
    onAttach: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onAnswer: (String, List<AskAnswer>) -> Unit,
    onLoadImage: suspend (String) -> ChatImage?,
    onRefreshResources: () -> Unit,
    onOpenResources: () -> Unit,
    onLoadEarlier: () -> Unit
) {
    val chat = state.chat
    val draft = state.activeSession?.let { state.drafts[it.sessionId]?.text }.orEmpty()
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val latestChat = rememberUpdatedState(chat)
    val listState = rememberLazyListState()
    val nearBottom by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount == 0 ||
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 2
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = !fullscreen, onBack = onBack)

    LaunchedEffect(state.activeSession?.sessionId) {
        fullscreen = false
        onRefreshResources()
    }

    val lastAssistant = chat.allItems.lastOrNull() as? AssistantMessage
    val showTyping = chat.sending || (chat.running && lastAssistant?.text.isNullOrBlank())
    val runningSince = chat.items.filterIsInstance<TurnStart>().lastOrNull()?.startedAt
        ?: chat.lastPromptAt
    var didInitialScroll by remember(state.activeSession?.sessionId) { mutableStateOf(false) }

    LaunchedEffect(chat.loading) {
        if (chat.loading) didInitialScroll = false
    }

    LaunchedEffect(
        state.activeSession?.sessionId,
        chat.loading,
        chat.allItems.size,
        chat.questions.size,
        showTyping,
        chat.error,
        chat.hasMoreHistory
    ) {
        if (chat.loading) return@LaunchedEffect
        if (chat.allItems.isEmpty() && chat.questions.isEmpty()) return@LaunchedEffect
        val target = chat.allItems.size +
            chat.questions.size +
            (if (showTyping) 1 else 0) +
            (if (chat.error != null) 1 else 0) +
            (if (chat.hasMoreHistory) 1 else 0)
        if (!didInitialScroll) {
            // Opening a conversation should land at the newest message, without
            // animating through the whole history.
            listState.scrollToItem(target)
            didInitialScroll = true
        } else {
            if (nearBottom) {
                listState.animateScrollToItem(target)
            }
        }
    }

    fun submit(): Boolean {
        val current = latestChat.value
        val clean = draft.trim()
        if (current.sending) return false
        if (clean.isEmpty() && current.attachments.none { it.ready }) return false
        if (current.attachments.any { it.uploading }) return false
        onDraftChange("")
        focusManager.clearFocus()
        onSend(clean)
        return true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFDFDFB), Color(0xFFF8FAFF), Color(0xFFF7F9FD))
                )
            )
            .systemBarsPadding()
    ) {
        ChatTopBar(
            title = sessionTitle(chat.title ?: state.activeSession?.title, state.activeSession?.blank ?: false),
            subtitle = if (chat.running) "正在生成回复…" else "AI 生成可能有误，注意核实",
            running = chat.running,
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            onMenu = onMenu,
            onNewChat = onNewChat,
            onRefresh = onRefresh,
            onRename = {
                menuOpen = false
                renameOpen = true
            },
            onSettings = {
                menuOpen = false
                onSettings()
            },
            runningSince = runningSince,
            usage = chat.usage
        )

        ChatControls(
            model = chat.model,
            modelCatalog = state.modelCatalog,
            modelBusy = chat.modelBusy,
            onModel = onModel,
            permission = chat.permission,
            permissionCatalog = state.permissionCatalog,
            permissionBusy = chat.permissionBusy,
            onPermission = onPermission,
            workspace = state.activeSession?.cwd,
            context = chat.context
        )

        ResourceStrip(
            status = state.resourceStatus,
            onClick = onOpenResources
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                chat.loading && chat.allItems.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Blue,
                            strokeWidth = 2.2.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("正在同步对话…", color = Muted, fontSize = 12.sp)
                    }
                }

                chat.allItems.isEmpty() && chat.error != null -> {
                    ChatErrorState(
                        title = if (chat.loadError) "对话加载失败" else "发送失败",
                        message = chat.error,
                        onRetry = onRefresh
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (chat.allItems.isEmpty() && chat.questions.isEmpty()) {
                            item(key = "welcome") {
                                ChatWelcome(
                                    onSuggestion = {
                                        onDraftChange(it)
                                        focusRequester.requestFocus()
                                    }
                                )
                            }
                        } else {
                            item(key = "day") { DayDivider() }
                            if (chat.hasMoreHistory) {
                                item(key = "earlier") {
                                    EarlierHistoryButton(
                                        loading = chat.loadingEarlier,
                                        onClick = onLoadEarlier
                                    )
                                }
                            }
                            items(chat.allItems, key = { it.key }) { item ->
                                ChatItemRow(item, onLoadImage)
                            }
                            items(chat.questions, key = { "question:${it.callId}" }) { batch ->
                                QuestionCard(
                                    batch = batch,
                                    busy = chat.questionBusy,
                                    onSubmit = { answers -> onAnswer(batch.callId, answers) }
                                )
                            }
                        }
                        if (showTyping) {
                            item(key = "typing") { TypingBubble() }
                        }
                        if (chat.error != null && chat.allItems.isNotEmpty()) {
                            item(key = "inline-error") {
                                ChatInlineError(message = chat.error, onRetry = onRefresh)
                            }
                        }
                    }
                }
            }
        }

        if (chat.queued.isNotEmpty()) {
            QueueStrip(chat.queued)
        }

        ChatComposer(
            draft = draft,
            onDraftChange = { if (it.length <= 12_000) onDraftChange(it) },
            attachments = chat.attachments,
            sending = chat.sending,
            running = chat.running,
            onSend = { submit() },
            onStop = onStop,
            onAttach = onAttach,
            onOpenGallery = onOpenGallery,
            onOpenCamera = onOpenCamera,
            onRemoveAttachment = onRemoveAttachment,
            onFullscreen = { fullscreen = true },
            focusRequester = focusRequester
        )
    }

    if (fullscreen) {
        FullScreenComposer(
            draft = draft,
            onDraftChange = onDraftChange,
            attachments = chat.attachments,
            sending = chat.sending,
            running = chat.running,
            onSend = {
                if (submit()) fullscreen = false
            },
            onStop = onStop,
            onAttach = onAttach,
            onOpenGallery = onOpenGallery,
            onOpenCamera = onOpenCamera,
            onRemoveAttachment = onRemoveAttachment,
            onClose = { fullscreen = false }
        )
    }

    if (renameOpen) {
        RenameDialog(
            initial = chat.title ?: state.activeSession?.title ?: "",
            onDismiss = { renameOpen = false },
            onConfirm = {
                renameOpen = false
                onRename(it)
            }
        )
    }
}

@Composable
private fun ChatControls(
    model: ModelChoice?,
    modelCatalog: ModelCatalog?,
    modelBusy: Boolean,
    onModel: (ModelChoice) -> Unit,
    permission: String?,
    permissionCatalog: PermissionCatalog?,
    permissionBusy: Boolean,
    onPermission: (String) -> Unit,
    workspace: String?,
    context: ContextPressure?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModelPill(
            model = model,
            catalog = modelCatalog,
            busy = modelBusy,
            onSelect = onModel
        )
        PermissionPill(
            value = permission,
            catalog = permissionCatalog,
            busy = permissionBusy,
            onSelect = onPermission
        )
        context?.let { ContextPill(it) }
        if (!workspace.isNullOrBlank()) {
            val name = workspace.trimEnd('/').substringAfterLast('/').ifBlank { workspace }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color.White.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        name,
                        color = Muted,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextPill(context: ContextPressure) {
    val window = context.contextWindow ?: return
    if (window <= 0) return
    val percent = context.percent ?: return
    val used = context.usedTokens ?: return
    var open by remember { mutableStateOf(false) }
    val tint = when {
        percent >= 90 -> Color(0xFFD9534F)
        percent >= 70 -> Color(0xFFD99A2B)
        else -> Blue
    }
    Box {
        Surface(
            modifier = Modifier.clickable { open = true },
            shape = RoundedCornerShape(100.dp),
            color = Color.White.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = percent / 100f,
                    modifier = Modifier.size(13.dp),
                    color = tint,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "上下文 $percent%",
                    color = tint,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text = "上下文占用 $percent%",
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatTokens(used)} / ${formatTokens(window)} tokens",
                    color = InkSoft,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "按当前对话预估，仅作参考",
                    color = Muted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private data class ModelTarget(val provider: String, val model: AiModel)

@Composable
private fun ModelPill(
    model: ModelChoice?,
    catalog: ModelCatalog?,
    busy: Boolean,
    onSelect: (ModelChoice) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var effortTarget by remember { mutableStateOf<ModelTarget?>(null) }
    val label = if (model == null) "选择模型" else catalog?.displayName(model) ?: model.model

    Box {
        Surface(
            modifier = Modifier.clickable(enabled = !busy) { open = true },
            shape = RoundedCornerShape(100.dp),
            color = Color(0xFFF5F8FF),
            border = BorderStroke(1.dp, Color(0xFFDCE6FF))
        ) {
            Row(
                Modifier.padding(start = 9.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    label,
                    color = Color(0xFF315BAE),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
                if (busy) {
                    Spacer(Modifier.width(5.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        color = Blue,
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF6B87C4),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .heightIn(max = 430.dp)
                .widthIn(min = 270.dp, max = 340.dp)
        ) {
            if (catalog == null || catalog.groups.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("模型目录加载中…", color = Muted, fontSize = 12.sp) },
                    onClick = { open = false }
                )
            } else {
                catalog.groups.forEach { group ->
                    Text(
                        text = group.name,
                        color = Muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp)
                    )
                    group.models.forEach { option ->
                        val selected = model?.provider == group.id && model.model == option.id
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            option.name,
                                            color = Ink,
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!option.description.isNullOrBlank()) {
                                            Text(
                                                option.description,
                                                color = Muted,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
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
                            },
                            onClick = {
                                open = false
                                if (option.efforts.size > 1) {
                                    effortTarget = ModelTarget(group.id, option)
                                } else {
                                    onSelect(
                                        ModelChoice(
                                            group.id,
                                            option.id,
                                            option.efforts.firstOrNull()?.id
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
        effortTarget?.let { target ->
            val currentEffort = if (
                model?.provider == target.provider && model.model == target.model.id
            ) {
                model.effort
            } else {
                target.model.defaultEffort
            }
            AlertDialog(
                onDismissRequest = { effortTarget = null },
                title = {
                    Text(
                        target.model.name,
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column {
                        Text(
                            "推理强度",
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        target.model.efforts.forEach { effort ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSelect(
                                            ModelChoice(
                                                target.provider,
                                                target.model.id,
                                                effort.id
                                            )
                                        )
                                        effortTarget = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(effort.name, color = Ink, fontSize = 14.sp)
                                    if (!effort.description.isNullOrBlank()) {
                                        Text(
                                            effort.description,
                                            color = Muted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (effort.id == currentEffort) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Blue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { effortTarget = null }) {
                        Text("取消", color = Muted)
                    }
                },
                containerColor = Color.White,
                titleContentColor = Ink,
                textContentColor = Ink
            )
        }
    }
}

@Composable
private fun PermissionPill(
    value: String?,
    catalog: PermissionCatalog?,
    busy: Boolean,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val label = catalog?.label(value) ?: "工作区权限"
    Box {
        Surface(
            modifier = Modifier.clickable(enabled = !busy) { open = true },
            shape = RoundedCornerShape(100.dp),
            color = Color.White.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                Modifier.padding(start = 9.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    label,
                    color = InkSoft,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
                if (busy) {
                    Spacer(Modifier.width(5.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        color = Blue,
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.widthIn(min = 280.dp, max = 330.dp)
        ) {
            (catalog?.options ?: emptyList()).forEach { option ->
                val selected = option.value == value
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    catalog?.label(option.value) ?: option.name,
                                    color = Ink,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    catalog?.description(option.value) ?: "",
                                    color = Muted,
                                    fontSize = 10.sp
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
                    },
                    onClick = {
                        open = false
                        if (!selected) onSelect(option.value)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String,
    running: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onRefresh: () -> Unit,
    onRename: () -> Unit,
    onSettings: () -> Unit,
    runningSince: Long? = null,
    usage: TokenUsage? = null
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running, runningSince) {
        if (running) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
    }
    val meta = buildString {
        if (running && runningSince != null) {
            append(" · ")
            append(formatDuration(now - runningSince))
        }
        val tokens = usage?.total ?: 0L
        if (tokens > 0) {
            append(" · ")
            append(formatTokens(tokens))
            append(" tokens")
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SoftIconButton(
            icon = Icons.Rounded.Menu,
            onClick = onMenu,
            contentDescription = "打开菜单",
            background = Color.Transparent,
            size = 38.dp
        )
        SoftIconButton(
            icon = Icons.Rounded.Edit,
            onClick = onNewChat,
            contentDescription = "新建对话",
            background = Color.Transparent,
            size = 36.dp
        )
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    Box(Modifier.size(5.dp).background(Green, CircleShape))
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    text = subtitle + meta,
                    color = if (running) Color(0xFF208A52) else Muted,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        SoftIconButton(
            icon = Icons.Rounded.Refresh,
            onClick = onRefresh,
            contentDescription = "刷新对话",
            background = Color.Transparent,
            size = 34.dp
        )
        Box {
            SoftIconButton(
                icon = Icons.Rounded.MoreHoriz,
                onClick = { onMenuOpenChange(true) },
                contentDescription = "更多",
                background = Color.Transparent,
                size = 36.dp
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuOpenChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("重命名会话", color = Ink, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Edit, null, tint = InkSoft, modifier = Modifier.size(17.dp))
                    },
                    onClick = onRename
                )
                DropdownMenuItem(
                    text = { Text("连接设置", color = Ink, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Settings, null, tint = InkSoft, modifier = Modifier.size(17.dp))
                    },
                    onClick = onSettings
                )
            }
        }
    }
}

@Composable
private fun ChatItemRow(
    item: ChatItem,
    onLoadImage: suspend (String) -> ChatImage?
) {
    when (item) {
        is UserMessage -> UserBubble(
            text = item.text,
            pending = item.pending,
            queued = item.queued,
            attachments = item.attachments,
            images = item.images,
            onLoadImage = onLoadImage
        )
        is AssistantMessage -> AssistantBubble(item.text, item.images, onLoadImage)
        is ToolItem -> Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(30.dp))
            Box(Modifier.weight(1f)) { ToolCard(item, onLoadImage) }
        }
        is NoticeItem -> NoticeCard(item)
        is dev.harness.pocket.data.TurnStart -> Unit
        is dev.harness.pocket.data.TurnTime -> TurnMetaRow(item)
    }
}

@Composable
private fun AssistantBubble(
    text: String,
    images: List<ChatImage> = emptyList(),
    onLoadImage: suspend (String) -> ChatImage? = { null }
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp)
    ) {
        if (text.isNotBlank() || images.isNotEmpty()) {
            AssistantRow(text, images, onLoadImage)
        }
    }
}

@Composable
private fun TypingBubble() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        TypingRow()
    }
}

@Composable
private fun DayDivider() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            "今天",
            color = Color(0xFFA5ABB8),
            fontSize = 10.sp,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(100.dp))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ChatWelcome(onSuggestion: (String) -> Unit) {
    val suggestions = listOf(
        "帮我看看这个项目" to "先浏览一下当前目录，告诉我这个项目的结构和入口。",
        "实现一个小功能" to "我想给当前项目加一个小功能，先帮我梳理应该改哪些文件。",
        "排查一个报错" to "我遇到了一个报错，帮我一起定位原因并给出修复方案。"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 42.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SparkLogo(46.dp, withShadow = true)
        Spacer(Modifier.height(16.dp))
        Text(
            "开始和 Harness 一起构建",
            color = Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "描述你的想法，或者从下面选一个开始。",
            color = Muted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(24.dp))
        suggestions.forEach { (title, prompt) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onSuggestion(prompt) },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(Color(0xFFF0F4FF), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Blue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        title,
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "填入",
                        color = Muted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatErrorState(title: String, message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            "重试",
            color = Blue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable { onRetry() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ChatInlineError(message: String, onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color(0xFFFFF1F2), RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            color = Color(0xFF9F2D3A),
            fontSize = 10.5.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "重试",
            color = Color(0xFF9F2D3A),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onRetry() }
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun AttachmentStrip(
    attachments: List<Attachment>,
    onRemove: (String) -> Unit,
    large: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            if (attachment.isImage && attachment.bytes != null) {
                AttachmentThumb(
                    attachment = attachment,
                    onRemove = { onRemove(attachment.id) },
                    large = large
                )
            } else {
                AttachmentChip(attachment = attachment, onRemove = { onRemove(attachment.id) })
            }
        }
    }
}

@Composable
private fun AttachmentThumb(
    attachment: Attachment,
    onRemove: () -> Unit,
    large: Boolean
) {
    val size = if (large) 82.dp else 60.dp
    val bitmap by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        attachment.id
    ) {
        value = withContext(Dispatchers.IO) {
            decodeImageThumb(attachment.bytes, if (large) 480 else 240)
        }
    }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8EBF0))
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = attachment.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (attachment.uploading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(18.dp),
                color = Blue,
                strokeWidth = 2.dp
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xAA1F232B))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "移除图片",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        if (attachment.error != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xCC9F2D3A))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("上传失败", color = Color.White, fontSize = 8.sp)
            }
        }
    }
}

private fun decodeImageThumb(bytes: ByteArray?, target: Int): android.graphics.Bitmap? {
    if (bytes == null) return null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > target || bounds.outHeight / sample > target) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun AttachmentChip(attachment: Attachment, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (attachment.error != null) Color(0xFFF1B7BE) else Line)
    ) {
        Row(
            Modifier.padding(start = 9.dp, end = 5.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📎", fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                attachment.name,
                color = if (attachment.error != null) Color(0xFF9F2D3A) else InkSoft,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            if (attachment.uploading) {
                Spacer(Modifier.width(6.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    color = Blue,
                    strokeWidth = 1.4.dp
                )
            }
            if (attachment.error != null) {
                Spacer(Modifier.width(5.dp))
                Text("失败", color = Color(0xFF9F2D3A), fontSize = 9.sp)
            }
            Spacer(Modifier.width(3.dp))
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "移除附件",
                    tint = Muted,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun EarlierHistoryButton(loading: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !loading) { onClick() }
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                color = Muted,
                strokeWidth = 1.5.dp
            )
            Spacer(Modifier.width(7.dp))
            Text("正在加载更早消息…", color = Muted, fontSize = 11.sp)
        } else {
            Text("↑ 加载更早消息", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QueueStrip(queued: List<UserMessage>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFF7E8),
        border = BorderStroke(1.dp, Color(0xFFF2DFC0))
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "待发送任务 ${queued.size} 条",
                    color = Color(0xFF8A5A12),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text("发送完成后自动清空", color = Color(0xFFB08A50), fontSize = 9.sp)
            }
            queued.take(3).forEachIndexed { index, item ->
                val imageCount = item.images.size
                val fileCount = item.attachments.size
                val meta = listOfNotNull(
                    if (imageCount > 0) "图片 $imageCount 张" else null,
                    if (fileCount > 0) item.attachments.joinToString("、") else null
                ).joinToString(" · ")
                Row(
                    Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "${index + 1}.",
                        color = Color(0xFFB08A50),
                        fontSize = 10.sp,
                        modifier = Modifier.width(16.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.text.trim().ifBlank {
                                if (meta.isNotBlank()) "附件任务" else "任务"
                            },
                            color = Color(0xFF6F4A14),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (meta.isNotBlank()) {
                            Text(
                                meta,
                                color = Color(0xFF9A6A22),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (queued.size > 3) {
                Text(
                    "还有 ${queued.size - 3} 条任务…",
                    color = Color(0xFF9A6A22),
                    fontSize = 9.5.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<Attachment>,
    sending: Boolean,
    running: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onFullscreen: () -> Unit,
    focusRequester: FocusRequester
) {
    val hasReady = attachments.any { it.ready }
    val uploadBusy = attachments.any { it.uploading }
    val hasContent = !uploadBusy && (draft.isNotBlank() || hasReady)
    val showStop = running && !sending && !hasContent
    val canSend = !sending && hasContent
    var panelOpen by remember { mutableStateOf(false) }
    val showFullscreen = draft.length > 80 || draft.count { it == '\n' } >= 3
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFFF7F9FD), Color(0xFFF7F9FD))
                )
            )
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    ) {
        if (attachments.isNotEmpty()) {
            AttachmentStrip(attachments = attachments, onRemove = onRemoveAttachment)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    Modifier.padding(start = 15.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        if (draft.isEmpty()) {
                            Text(
                                "给 Harness 发消息…",
                                color = Color(0xFFA6ABB8),
                                fontSize = 14.5.sp
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 24.dp, max = 108.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { if (it.isFocused) panelOpen = false },
                            textStyle = TextStyle(color = Ink, fontSize = 15.sp, lineHeight = 22.sp),
                            cursorBrush = SolidColor(Blue),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSend() })
                        )
                    }
                    if (showFullscreen) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable {
                                    panelOpen = false
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                    onFullscreen()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Fullscreen,
                                contentDescription = "全屏编辑",
                                tint = InkSoft,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    showStop -> SolidColor(Color(0xFF23262E))
                                    canSend -> Brush.linearGradient(
                                        listOf(Color(0xFF6C9BFF), Color(0xFF4C7DFF))
                                    )
                                    else -> SolidColor(Color(0xFFECEFF5))
                                }
                            )
                            .clickable(enabled = !sending && (showStop || canSend)) {
                                if (showStop) onStop() else onSend()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            sending -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Blue,
                                strokeWidth = 2.dp
                            )
                            showStop -> Icon(
                                Icons.Rounded.Stop,
                                contentDescription = "停止生成",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            else -> Icon(
                                Icons.Rounded.ArrowUpward,
                                contentDescription = "发送",
                                tint = if (canSend) Color.White else Color(0xFFB7BCC8),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(7.dp))
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(enabled = !sending) {
                        panelOpen = !panelOpen
                        if (panelOpen) {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }
                    },
                shape = CircleShape,
                color = if (panelOpen) Blue else Color.White,
                border = BorderStroke(1.dp, if (panelOpen) Blue else Line),
                shadowElevation = if (panelOpen) 0.dp else 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (panelOpen) "关闭面板" else "更多功能",
                        tint = if (panelOpen) Color.White else Ink,
                        modifier = Modifier
                            .size(22.dp)
                            .rotate(if (panelOpen) 45f else 0f)
                    )
                }
            }
        }
        if (panelOpen) {
            Spacer(Modifier.height(8.dp))
            PlusPanel(
                onGallery = {
                    panelOpen = false
                    onOpenGallery()
                },
                onCamera = {
                    panelOpen = false
                    onOpenCamera()
                },
                onFile = {
                    panelOpen = false
                    onAttach()
                }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Harness 可能会犯错，请检查重要信息。",
            color = Color(0xFFB0B5C1),
            fontSize = 9.5.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun PlusPanel(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onFile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF1F4F9)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelAction(
                label = "相册",
                icon = Icons.Rounded.PhotoLibrary,
                tint = Blue,
                modifier = Modifier.weight(1f),
                onClick = onGallery
            )
            PanelAction(
                label = "拍摄",
                icon = Icons.Rounded.PhotoCamera,
                tint = Color(0xFF4CAF7D),
                modifier = Modifier.weight(1f),
                onClick = onCamera
            )
            PanelAction(
                label = "文件",
                icon = Icons.Rounded.InsertDriveFile,
                tint = Color(0xFF7B72E9),
                modifier = Modifier.weight(1f),
                onClick = onFile
            )
        }
    }
}

@Composable
private fun PanelAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(82.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, color = Ink, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FullScreenComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<Attachment>,
    sending: Boolean,
    running: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onClose: () -> Unit
) {
    val inputFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val hasReady = attachments.any { it.ready }
    val uploadBusy = attachments.any { it.uploading }
    val hasContent = !uploadBusy && (draft.isNotBlank() || hasReady)
    val showStop = running && !sending && !hasContent
    val canSend = !sending && hasContent

    LaunchedEffect(Unit) {
        inputFocus.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F9FD)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "关闭全屏编辑",
                            tint = Ink,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        "编辑消息",
                        modifier = Modifier.weight(1f),
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.width(42.dp))
                }
                if (attachments.isNotEmpty()) {
                    AttachmentStrip(
                        attachments = attachments,
                        onRemove = onRemoveAttachment,
                        large = true
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Box(Modifier.fillMaxSize().padding(16.dp)) {
                        if (draft.isEmpty()) {
                            Text(
                                "输入消息…",
                                color = Color(0xFFA6ABB8),
                                fontSize = 16.sp
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(inputFocus),
                            textStyle = TextStyle(color = Ink, fontSize = 16.sp, lineHeight = 25.sp),
                            cursorBrush = SolidColor(Blue),
                            maxLines = Int.MAX_VALUE
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FullscreenTool(
                        icon = Icons.Rounded.PhotoLibrary,
                        label = "相册",
                        onClick = onOpenGallery
                    )
                    FullscreenTool(
                        icon = Icons.Rounded.PhotoCamera,
                        label = "拍摄",
                        onClick = onOpenCamera
                    )
                    FullscreenTool(
                        icon = Icons.Rounded.InsertDriveFile,
                        label = "文件",
                        onClick = onAttach
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .clickable(enabled = !sending && (showStop || canSend)) {
                                if (showStop) onStop() else onSend()
                            },
                        shape = RoundedCornerShape(21.dp),
                        color = when {
                            showStop -> Color(0xFF23262E)
                            canSend -> Blue
                            else -> Color(0xFFE7EBF2)
                        }
                    ) {
                        Box(
                            Modifier.padding(horizontal = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                sending -> CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                showStop -> Icon(
                                    Icons.Rounded.Stop,
                                    contentDescription = "停止生成",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                else -> Text(
                                    "发送",
                                    color = if (canSend) Color.White else Color(0xFFAAB0BC),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullscreenTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = InkSoft, modifier = Modifier.size(21.dp))
        Text(label, color = InkSoft, fontSize = 10.sp)
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("重命名会话", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 80) value = it },
                singleLine = true,
                placeholder = { Text("输入一个标题", color = Muted, fontSize = 13.sp) },
                colors = harnessFieldColors(),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank()
            ) {
                Text("保存", color = if (value.isNotBlank()) Blue else Muted, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Muted)
            }
        }
    )
}