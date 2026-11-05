package dev.harness.pocket.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.Prefs

@Composable
fun ConnectScreen(
    state: AppState,
    onConnect: (String, String) -> Unit,
    onDismissError: () -> Unit
) {
    var server by rememberSaveable {
        mutableStateOf(state.savedBase.ifBlank { Prefs.DEFAULT_BASE })
    }
    var token by rememberSaveable { mutableStateOf("") }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    var helpExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        focusManager.clearFocus()
        onConnect(server, token)
    }

    HarnessBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(44.dp))
            SparkLogo(64.dp, withShadow = true)
            Spacer(Modifier.height(18.dp))
            Text(
                "Harness",
                color = Ink,
                fontSize = 33.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "把 coding agent 装进口袋",
                color = Muted,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(34.dp))
            Text(
                "连接你的工作台",
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "粘贴电脑上 dsh web 的访问链接，手机就能远程对话。",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Line),
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("服务器地址") },
                        placeholder = { Text("http://127.0.0.1:8080") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Dns, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = harnessFieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        enabled = !state.connecting
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("访问令牌 / 完整链接") },
                        placeholder = { Text("粘贴带 ?token= 的链接") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Key, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    imageVector = if (tokenVisible) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (tokenVisible) "隐藏令牌" else "显示令牌"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = harnessFieldColors(),
                        visualTransformation = if (tokenVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        enabled = !state.connecting
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "令牌只保存在本机，不会上传到第三方",
                            color = Muted,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (state.connecting) "正在连接…" else "连接",
                onClick = { submit() },
                loading = state.connecting,
                enabled = !state.connecting
            )

            if (!state.connectError.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3F4), RoundedCornerShape(14.dp))
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
                        text = state.connectError,
                        color = Color(0xFF9F2D3A),
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "关闭",
                        color = Color(0xFF9F2D3A),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onDismissError() }
                            .padding(start = 8.dp, top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Line)
            ) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { helpExpanded = !helpExpanded }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = Amber,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "怎么获取访问链接？",
                            color = Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = if (helpExpanded) {
                                Icons.Rounded.KeyboardArrowUp
                            } else {
                                Icons.Rounded.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = Muted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (helpExpanded) {
                        Column(
                            Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            HelpStep("1", "在电脑上启动 dsh web，或保持远程服务运行。")
                            HelpStep("2", "复制终端里带 ?token= 的完整地址；每次重启令牌都会变。")
                            HelpStep("3", "粘贴到上方。App 会用令牌换取一个只存于本机的登录 Cookie。")
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(
                "开源 · Kotlin + Jetpack Compose",
                color = Muted.copy(alpha = 0.75f),
                fontSize = 10.5.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HelpStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(18.dp)
                .background(Color(0xFFEDF2FF), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = Blue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = InkSoft,
            fontSize = 11.5.sp,
            lineHeight = 18.sp
        )
    }
}
