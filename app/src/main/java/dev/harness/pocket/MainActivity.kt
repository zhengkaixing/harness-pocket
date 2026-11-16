package dev.harness.pocket

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
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
                    onRefresh = vm::refreshHome
                )
            }
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
                    onRefresh = vm::refreshHome
                )
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
