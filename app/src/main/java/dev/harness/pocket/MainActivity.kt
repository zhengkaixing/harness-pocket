package dev.harness.pocket

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dev.harness.pocket.data.AppState
import dev.harness.pocket.ui.ChatScreen
import dev.harness.pocket.ui.ConnectScreen
import dev.harness.pocket.ui.HarnessDrawer
import dev.harness.pocket.ui.HarnessTheme
import dev.harness.pocket.ui.HomeScreen
import dev.harness.pocket.ui.SettingsDialog
import dev.harness.pocket.ui.SplashScreen
import dev.harness.pocket.ui.WorkspaceDialog
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
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
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
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) vm.attachFile(uri)
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
            onDismissError = vm::dismissConnectError
        )

        else -> ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HarnessDrawer(
                    state = state,
                    onClose = closeDrawer,
                    onNewChat = vm::newSession,
                    onWorkspace = {
                        workspaceOpen = true
                        if (state.workspaces.isEmpty()) vm.refreshWorkspaces()
                    },
                    onOpenSession = vm::openSession,
                    onSettings = { showSettings = true },
                    onPinSession = vm::togglePin,
                    onUnpinSession = vm::togglePin,
                    onArchiveSession = vm::archiveSession
                )
            }
        ) {
            if (state.activeSession != null) {
                ChatScreen(
                    state = state,
                    onBack = vm::closeSession,
                    onMenu = openDrawer,
                    onNewChat = vm::newSession,
                    onSend = vm::send,
                    onStop = vm::stop,
                    onRename = vm::rename,
                    onRefresh = vm::refreshChat,
                    onSettings = { showSettings = true },
                    onModel = vm::selectModel,
                    onPermission = vm::selectPermission,
                    onAttach = { filePicker.launch(arrayOf("*/*")) },
                    onPickImage = { imagePicker.launch("image/*") },
                    onRemoveAttachment = vm::removeAttachment
                )
            } else {
                HomeScreen(
                    state = state,
                    onMenu = openDrawer,
                    onNewChat = vm::newSession,
                    onOpenSession = vm::openSession,
                    onRefresh = vm::refreshSessions,
                    onSettings = { showSettings = true },
                    onOpenWorkspace = {
                        workspaceOpen = true
                        if (state.workspaces.isEmpty()) vm.refreshWorkspaces()
                    },
                    onPinSession = vm::togglePin,
                    onUnpinSession = vm::togglePin,
                    onArchiveSession = vm::archiveSession,
                    onUnarchiveSession = vm::unarchiveSession
                )
            }
        }
    }

    if (showSettings && state.connection != null) {
        SettingsDialog(
            baseUrl = state.connection.baseUrl,
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
}
