package dev.harness.pocket

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Bitmap
import android.net.Uri
import android.text.TextUtils
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import dev.harness.pocket.ui.WarmWhite
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.ReplayServer
import dev.harness.pocket.data.AssistantMessage
import dev.harness.pocket.data.ChatFile
import dev.harness.pocket.ui.AlbumPicker
import dev.harness.pocket.ui.AutomationScreen
import dev.harness.pocket.ui.ChatScreen
import dev.harness.pocket.ui.ConnectScreen
import dev.harness.pocket.ui.HarnessDrawer
import dev.harness.pocket.ui.HarnessTheme
import dev.harness.pocket.ui.HomeScreen
import dev.harness.pocket.ui.ResourceScreen
import dev.harness.pocket.ui.SettingsDialog
import dev.harness.pocket.ui.SplashScreen
import dev.harness.pocket.ui.WorkspaceDialog
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val pendingSessionId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingSessionId.value = intent?.getStringExtra(TaskMonitorService.EXTRA_SESSION_ID)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        setContent {
            HarnessTheme {
                val state by vm.state.collectAsState()
                AppRoot(
                    state = state,
                    vm = vm,
                    pendingSessionId = pendingSessionId.value,
                    onPendingConsumed = { pendingSessionId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSessionId.value = intent.getStringExtra(TaskMonitorService.EXTRA_SESSION_ID)
    }
}

@Composable
private fun AppRoot(
    state: AppState,
    vm: MainViewModel,
    pendingSessionId: String?,
    onPendingConsumed: () -> Unit
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var workspaceOpen by rememberSaveable { mutableStateOf(false) }
    var galleryOpen by rememberSaveable { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        cameraUri = null
        if (success && uri != null) vm.attachFile(uri)
    }
    val openCamera: () -> Unit = {
        try {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File.createTempFile("camera_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "无法打开相机：${e.message ?: "没有可用的相机应用"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val shareConversation: () -> Unit = {
        val chat = state.chat
        val title = state.activeSession?.title?.takeIf { it.isNotBlank() } ?: "Harness 对话"
        val text = buildString {
            appendLine("# $title")
            appendLine()
            chat.items.forEach { item ->
                when (item) {
                    is dev.harness.pocket.data.UserMessage -> {
                        appendLine("## 你")
                        appendLine(item.text)
                        appendLine()
                    }
                    is dev.harness.pocket.data.AssistantMessage -> {
                        if (item.text.isNotBlank()) {
                            appendLine("## 助手")
                            appendLine(item.text)
                            appendLine()
                        }
                    }
                    else -> Unit
                }
            }
        }.take(90_000)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "导出对话")) }
    }

    val shareReplyCard: () -> Unit = {
        val reply = state.chat.items
            .filterIsInstance<AssistantMessage>()
            .lastOrNull()?.text?.trim()
        if (reply.isNullOrBlank()) {
            Toast.makeText(context, "没有可分享的回复", Toast.LENGTH_SHORT).show()
        } else {
            val title = state.activeSession?.title?.takeIf { it.isNotBlank() } ?: "Harness 对话"
            val cardTitle = title.take(30)
            val width = 1080
            val pad = 64
            val contentWidth = width - pad * 2
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(32, 37, 52)
                textSize = 34f
                typeface = Typeface.DEFAULT
            }
            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(83, 109, 254)
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(32, 37, 52)
                textSize = 44f
                typeface = Typeface.DEFAULT_BOLD
            }
            val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(139, 146, 165)
                textSize = 24f
            }
            val lines = ArrayList<String>()
            reply.lines().forEach { paragraph ->
                if (paragraph.isBlank()) {
                    lines.add("")
                } else {
                    var rest = paragraph.trim()
                    while (rest.isNotEmpty()) {
                        val count = bodyPaint.breakText(rest, true, contentWidth.toFloat(), null).coerceAtLeast(1)
                        lines.add(rest.take(count))
                        rest = rest.drop(count)
                    }
                }
            }
            val visible = lines.take(62)
            val height = pad + 56 + 18 + 58 + 26 + visible.size * 50 + 58
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawText("HARNESS POCKET", pad.toFloat(), (pad + 28).toFloat(), headPaint)
            canvas.drawText(cardTitle, pad.toFloat(), (pad + 56 + 18 + 42).toFloat(), titlePaint)
            var y = pad + 56 + 18 + 58 + 26 + 30
            visible.forEach { line ->
                if (line.isNotEmpty()) canvas.drawText(line, pad.toFloat(), y.toFloat(), bodyPaint)
                y += 50
            }
            canvas.drawText("来自 Harness Pocket", pad.toFloat(), (height - pad + 4).toFloat(), footPaint)
            val card = File(context.cacheDir, "attachments").apply { mkdirs() }
                .resolve("reply-card.png")
            FileOutputStream(card).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                card
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, "分享回复卡片")) }
        }
    }

    val shareReplay: () -> Unit = {
        val chat = state.chat
        val title = state.activeSession?.title?.takeIf { it.isNotBlank() } ?: "Harness 对话"
        val body = buildString {
            append("<!doctype html><html lang=\"zh\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
            append("<title>").append(TextUtils.htmlEncode(title)).append("</title><style>")
            append("body{margin:0;background:#f7f8fc;color:#202534;font:15px/1.7 -apple-system,BlinkMacSystemFont,Segoe UI,sans-serif}")
            append(".wrap{max-width:760px;margin:0 auto;padding:32px 18px 64px}h1{font-size:22px;margin:0 0 24px}")
            append(".m{margin:0 0 18px;padding:14px 16px;border-radius:14px;white-space:pre-wrap;word-break:break-word}")
            append(".u{background:#e9eeff;border:1px solid #dce4ff}.a{background:#fff;border:1px solid #e7e9f2}")
            append(".t{font-size:12px;color:#8b92a5;margin:0 0 4px}.tool{font-size:12px;color:#5b6274;background:#fff;border:1px dashed #e7e9f2;border-radius:10px;padding:8px 10px;margin:6px 0}")
            append("</style></head><body><div class=\"wrap\"><h1>").append(TextUtils.htmlEncode(title)).append("</h1>")
            chat.items.forEach { item ->
                when (item) {
                    is dev.harness.pocket.data.UserMessage -> append("<div class=\"m u\"><div class=\"t\">你</div>").append(TextUtils.htmlEncode(item.text)).append("</div>")
                    is dev.harness.pocket.data.AssistantMessage -> if (item.text.isNotBlank()) append("<div class=\"m a\"><div class=\"t\">助手</div>").append(TextUtils.htmlEncode(item.text)).append("</div>")
                    is dev.harness.pocket.data.ToolItem -> append("<div class=\"tool\">").append(TextUtils.htmlEncode(item.name)).append(" · ").append(if (item.isError) "失败" else if (item.done) "完成" else "执行中").append("</div>")
                    is dev.harness.pocket.data.NoticeItem -> append("<div class=\"m a\"><div class=\"t\">提示</div>").append(TextUtils.htmlEncode(item.text)).append("</div>")
                    else -> Unit
                }
            }
            append("</div></body></html>")
        }
        val file = File(context.cacheDir, "attachments").apply { mkdirs() }.resolve("replay.html")
        file.writeText(body, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val url = ReplayServer.start(body)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            if (url != null) putExtra(Intent.EXTRA_TEXT, "对话回放（同一局域网可打开）：$url")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享回放网页")) }
        if (url != null) Toast.makeText(context, "局域网回放：$url", Toast.LENGTH_LONG).show()
    }

    val openLink: (String) -> Unit = { raw ->
        val link = raw.trim()
        if (link.isNotBlank()) {
            val normalized = when {
                link.startsWith("www.", ignoreCase = true) -> "https://$link"
                else -> link
            }
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "无法打开链接：${e.message ?: "没有可用的应用"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val openFile: (ChatFile) -> Unit = { file ->
        scope.launch {
            val saved = vm.downloadFile(file)
            if (saved == null) {
                Toast.makeText(context, "附件下载失败，请重试", Toast.LENGTH_LONG).show()
            } else {
                val mime = when {
                    saved.name.endsWith(".apk", ignoreCase = true) ->
                        "application/vnd.android.package-archive"
                    file.mediaType.isNotBlank() && !file.mediaType.startsWith("application/octet") ->
                        file.mediaType
                    else -> "application/octet-stream"
                }
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        saved
                    )
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, mime)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(intent, "打开附件"))
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "已保存到 ${saved.absolutePath}，但没有可打开的应用",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.attachFile(uri)
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { vm.attachFile(it) }
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(state.connection) {
        if (state.connection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) vm.refreshWorkspaces()
    }

    LaunchedEffect(pendingSessionId, state.sessions) {
        val id = pendingSessionId ?: return@LaunchedEffect
        val session = state.sessions.firstOrNull { it.sessionId == id }
        if (session != null) {
            vm.openSession(session)
            onPendingConsumed()
        }
    }

    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() }; Unit }
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() }; Unit }

    when {
        state.restoring -> SplashScreen()

        state.connection == null -> ConnectScreen(
            state = state,
            onConnect = vm::connect,
            onSwitchHost = vm::switchHost,
            onDismissError = vm::dismissConnectError
        )

        else -> ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HarnessDrawer(
                    state = state,
                    onClose = closeDrawer,
                    onNewChat = vm::newSession,
                    onNewChatInWorkspace = vm::newSessionInWorkspace,
                    onWorkspace = {
                        workspaceOpen = true
                        if (state.workspaces.isEmpty()) vm.refreshWorkspaces()
                    },
                    onMoveWorkspace = vm::moveWorkspace,
                    onOpenSession = vm::openSession,
                    onSettings = { showSettings = true },
                    onResources = vm::openResources,
                    onAutomations = vm::openAutomations,
                    onPinSession = vm::togglePin,
                    onUnpinSession = vm::togglePin,
                    onArchiveSession = vm::archiveSession,
                    onUnarchiveSession = vm::unarchiveSession,
                    onRefresh = vm::refreshHome,
                    onSearch = vm::searchSessions
                )
            }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(WarmWhite),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .widthIn(max = if (state.activeSession != null) 900.dp else 680.dp)
                        .fillMaxWidth()
                ) {
            if (state.automationsVisible) {
                AutomationScreen(
                    state = state,
                    onBack = vm::closeAutomations,
                    onRefresh = vm::refreshAutomations,
                    onOpenSession = vm::openSession,
                    onDelete = vm::deleteAutomation,
                    onUpdate = vm::updateAutomation
                )
            } else if (state.resourcesVisible) {
                ResourceScreen(
                    state = state,
                    onBack = vm::closeResources,
                    onRefresh = vm::refreshResources,
                    onLimit = vm::setResourceLimit
                )
            } else if (state.activeSession != null) {
                ChatScreen(
                    state = state,
                    onBack = vm::closeSession,
                    onMenu = openDrawer,
                    onNewChat = vm::newSession,
                    onDraftChange = vm::setDraft,
                    onSend = vm::send,
                    onSteer = { vm.send(it, "steer") },
                    onStop = vm::stop,
                    onRename = vm::rename,
                    onRefresh = vm::refreshChat,
                    onSettings = { showSettings = true },
                    onModel = vm::selectModel,
                    onPermission = vm::selectPermission,
                    onToggleComputerUse = vm::toggleComputerUse,
                    approvals = state.approvals,
                    approvalBusyId = state.approvalBusyId,
                    approvalError = state.approvalError,
                    onAnswerApproval = vm::answerApproval,
                    onAttach = { filePicker.launch(arrayOf("*/*")) },
                    onOpenGallery = { galleryOpen = true },
                    onOpenCamera = openCamera,
                    onRemoveAttachment = vm::removeAttachment,
                    onAnswer = vm::answerQuestions,
                    onLoadImage = vm::loadImage,
                    onOpenFile = openFile,
                    onOpenLink = openLink,
                    onExport = shareConversation,
                    onShareCard = shareReplyCard,
                    onShareReplay = shareReplay,
                    onRefreshResources = vm::refreshResources,
                    onOpenResources = vm::openResources,
                    onLoadEarlier = vm::loadEarlier
                )
            } else {
                HomeScreen(
                    state = state,
                    onNewChat = vm::newSession,
                    onNewChatInWorkspace = vm::newSessionInWorkspace,
                    onOpenSession = vm::openSession,
                    onWorkspace = {
                        workspaceOpen = true
                        if (state.workspaces.isEmpty()) vm.refreshWorkspaces()
                    },
                    onMoveWorkspace = vm::moveWorkspace,
                    onSettings = { showSettings = true },
                    onResources = vm::openResources,
                    onAutomations = vm::openAutomations,
                    onPinSession = vm::togglePin,
                    onUnpinSession = vm::togglePin,
                    onArchiveSession = vm::archiveSession,
                    onUnarchiveSession = vm::unarchiveSession,
                    onRefresh = vm::refreshHome,
                    onSearch = vm::searchSessions
                )
            }
                }
            }
        }
    }

    if (showSettings && state.connection != null) {
        SettingsDialog(
            baseUrl = state.connection.baseUrl,
            hosts = state.savedHosts,
            ntfyTopic = state.ntfyTopic,
            onSaveNtfy = vm::saveNtfyTopic,
            onSwitchHost = {
                showSettings = false
                vm.switchHost(it)
            },
            onRemoveHost = vm::removeHost,
            onAddHost = {
                showSettings = false
                vm.disconnect()
            },
            onDismiss = { showSettings = false },
            onDisconnect = {
                showSettings = false
                vm.disconnect()
            }
        )
    }

    if (workspaceOpen) {
        WorkspaceDialog(
            state = state,
            onDismiss = { workspaceOpen = false },
            onSelect = {
                vm.selectWorkspace(it)
                workspaceOpen = false
            },
            onRefresh = vm::refreshWorkspaces,
            onCreate = vm::createWorkspace
        )
    }

    if (galleryOpen) {
        AlbumPicker(
            onDismiss = { galleryOpen = false },
            onPick = { uris ->
                galleryOpen = false
                uris.forEach { vm.attachFile(it) }
            },
            onSystemPicker = {
                galleryOpen = false
                imagePicker.launch("image/*")
            }
        )
    }
}
