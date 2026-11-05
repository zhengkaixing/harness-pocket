package dev.harness.pocket.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AiModel
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.AssistantMessage
import dev.harness.pocket.data.Attachment
import dev.harness.pocket.data.ChatItem
import dev.harness.pocket.data.ChatState
import dev.harness.pocket.data.ModelCatalog
import dev.harness.pocket.data.ModelChoice
import dev.harness.pocket.data.NoticeItem
import dev.harness.pocket.data.PermissionCatalog
import dev.harness.pocket.data.ToolItem
import dev.harness.pocket.data.UserMessage

@Composable
fun ChatScreen(
    state: AppState,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRename: (String) -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onModel: (ModelChoice) -> Unit,
    onPermission: (String) -> Unit,
    onAttach: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveAttachment: (String) -> Unit
) {
    val chat = state.chat
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    val lastAssistant = chat.allItems.lastOrNull() as? AssistantMessage
    val showTyping = chat.sending || (chat.running && lastAssistant?.text.isNullOrBlank())

    LaunchedEffect(chat.allItems.size, showTyping) {
        val count = chat.allItems.size + if (showTyping) 1 else 0
        if (count > 0) {
            listState.animateScrollToItem(count)
        }
    }

    fun submit() {
        val clean = draft.trim()
        if (chat.sending || chat.running) return
        if (clean.isEmpty() && chat.attachments.none { it.ready }) return
        if (chat.attachments.any { it.uploading }) return
        draft = ""
        focusManager.clearFocus()
        onSend(clean)
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
            }
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
            workspace = state.activeSession?.cwd
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
                    ChatErrorState(message = chat.error, onRetry = onRefresh)
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (chat.allItems.isEmpty()) {
                            item(key = "welcome") {
                                ChatWelcome(
                                    onSuggestion = {
                                        draft = it
                                        focusRequester.requestFocus()
                                    }
                                )
                            }
                        } else {
                            item(key = "day") { DayDivider() }
                            items(chat.allItems, key = { it.key }) { item ->
                                ChatItemRow(item)
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

        ChatComposer(
            draft = draft,
            onDraftChange = { if (it.length <= 12_000) draft = it },
            attachments = chat.attachments,
            sending = chat.sending,
            running = chat.running,
            onSend = ::submit,
            onStop = onStop,
            onAttach = onAttach,
            onPickImage = onPickImage,
            onRemoveAttachment = onRemoveAttachment,
            focusRequester = focusRequester
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
    workspace: String?
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
    onSettings: () -> Unit
) {
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
                    text = subtitle,
                    color = if (running) Color(0xFF208A52) else Muted,
                    fontSize = 9.5.sp
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
private fun ChatItemRow(item: ChatItem) {
    when (item) {
        is UserMessage -> UserBubble(item.text, item.pending, item.attachments)
        is AssistantMessage -> AssistantBubble(item.text)
        is ToolItem -> Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(30.dp))
            Box(Modifier.weight(1f)) { ToolCard(item) }
        }
        is NoticeItem -> NoticeCard(item)
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp)
    ) {
        if (text.isBlank()) {
            TypingDots()
        } else {
            AssistantRow(text)
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
private fun ChatErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("对话加载失败", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
    onRemove: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        attachments.forEach { attachment ->
            AttachmentChip(attachment = attachment, onRemove = { onRemove(attachment.id) })
        }
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
private fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<Attachment>,
    sending: Boolean,
    running: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val hasReady = attachments.any { it.ready }
    val uploadBusy = attachments.any { it.uploading }
    val canSend = !sending && (running || (!uploadBusy && (draft.isNotBlank() || hasReady)))

    Column(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFFF7F9FD), Color(0xFFF7F9FD))
                )
            )
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
    ) {
        if (attachments.isNotEmpty()) {
            AttachmentStrip(
                attachments = attachments,
                onRemove = onRemoveAttachment
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 9.dp,
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                Modifier.padding(start = 14.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !sending && !running) { onPickImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PhotoCamera,
                        contentDescription = "拍照或选择图片",
                        tint = if (sending || running) Color(0xFFC4C9D3) else InkSoft,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(2.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
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
                            .heightIn(min = 24.dp)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(color = Ink, fontSize = 15.sp, lineHeight = 22.sp),
                        cursorBrush = SolidColor(Blue),
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() })
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !sending && !running) { onAttach() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "添加文件",
                        tint = if (sending || running) Color(0xFFC4C9D3) else InkSoft,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                running -> SolidColor(Color(0xFF23262E))
                                canSend -> Brush.linearGradient(
                                    listOf(Color(0xFF6C9BFF), Color(0xFF4C7DFF))
                                )
                                else -> SolidColor(Color(0xFFECEFF5))
                            }
                        )
                        .clickable(enabled = !sending && (running || canSend)) {
                            if (running) onStop() else onSend()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        sending -> CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        running -> Icon(
                            Icons.Rounded.Stop,
                            contentDescription = "停止生成",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        else -> Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = "发送",
                            tint = if (canSend) Color.White else Color(0xFFB7BCC8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
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
