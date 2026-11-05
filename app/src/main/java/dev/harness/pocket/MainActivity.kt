package dev.harness.pocket

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import dev.harness.pocket.data.AppState
import dev.harness.pocket.ui.ChatScreen
import dev.harness.pocket.ui.ConnectScreen
import dev.harness.pocket.ui.HarnessTheme
import dev.harness.pocket.ui.HomeScreen
import dev.harness.pocket.ui.SettingsDialog
import dev.harness.pocket.ui.SplashScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                AppRoot(state = state, vm = vm)
            }
        }
    }
}

@Composable
private fun AppRoot(state: AppState, vm: MainViewModel) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    when {
        state.restoring -> SplashScreen()

        state.connection == null -> ConnectScreen(
            state = state,
            onConnect = vm::connect,
            onDismissError = vm::dismissConnectError
        )

        state.activeSession != null -> ChatScreen(
            state = state,
            onBack = vm::closeSession,
            onSend = vm::send,
            onStop = vm::stop,
            onRename = vm::rename,
            onRefresh = vm::refreshChat,
            onSettings = { showSettings = true }
        )

        else -> HomeScreen(
            state = state,
            onNewChat = vm::newSession,
            onOpenSession = vm::openSession,
            onRefresh = vm::refreshSessions,
            onSettings = { showSettings = true }
        )
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
}
