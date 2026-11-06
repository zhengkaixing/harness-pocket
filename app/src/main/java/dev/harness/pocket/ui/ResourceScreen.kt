package dev.harness.pocket.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.AppState
import dev.harness.pocket.data.ResourceDisk
import dev.harness.pocket.data.ResourceStatus

@Composable
fun ResourceScreen(
    state: AppState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLimit: (Int) -> Unit
) {
    BackHandler(onBack = onBack)
    HarnessBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftIconButton(Icons.Rounded.ArrowBack, onBack, "返回")
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("资源与并发", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "来自 dsh-conversation-queue 插件",
                        color = Muted,
                        fontSize = 10.5.sp
                    )
                }
                if (state.resourcesLoading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = Blue,
                        strokeWidth = 2.dp
                    )
                } else {
                    SoftIconButton(Icons.Rounded.Refresh, onRefresh, "刷新资源状态")
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.resourcesError?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF1F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF6C9CE))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "资源状态读取失败",
                                color = Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(error, color = InkSoft, fontSize = 12.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "请确认当前 Harness 已安装 dsh-conversation-queue 插件。",
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                val status = state.resourceStatus
                if (status == null) {
                    if (!state.resourcesLoading && state.resourcesError == null) {
                        EmptyResourceCard(onRefresh)
                    }
                } else {
                    MemoryCard(status)
                    GateCard(status, state.resourcesBusy, onLimit)
                    if (status.disks.isEmpty()) {
                        EmptyDiskCard()
                    } else {
                        status.disks.forEach { DiskCard(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(status: ResourceStatus) {
    val memory = status.memory
    val fraction = (memory.usedPercent?.div(100.0) ?: 0.0).coerceIn(0.0, 1.0).toFloat()
    ResourceCard(
        icon = Icons.Rounded.Memory,
        title = "内存",
        subtitle = status.platform ?: "本机"
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatBytes(memory.usedBytes),
                color = Ink,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                " / ${formatBytes(memory.totalBytes)}",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                memory.usedPercent?.let { "${it.toInt()}%" } ?: "--",
                color = if (fraction > 0.85f) Red else Blue,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(7.dp))
        LinearProgressIndicator(
            progress = fraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (fraction > 0.85f) Red else Blue,
            trackColor = Color(0xFFEAF0FF)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MiniStat("可用", formatBytes(memory.availBytes))
            if (memory.commitUsedBytes != null && memory.commitLimitBytes != null) {
                MiniStat(
                    "提交",
                    "${formatBytes(memory.commitUsedBytes)} / ${formatBytes(memory.commitLimitBytes)}"
                )
            }
            MiniStat("采样", memory.sampler ?: "process")
        }
    }
}

@Composable
private fun GateCard(status: ResourceStatus, busy: Boolean, onLimit: (Int) -> Unit) {
    val gate = status.gate
    ResourceCard(
        icon = Icons.Rounded.Speed,
        title = "并发闸门",
        subtitle = if (gate.enabled) "已启用" else "未启用"
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("运行中 ${gate.running}", if (gate.running > 0) Green else Muted)
            Pill("等待 ${gate.waiting}", if (gate.waiting > 0) Amber else Muted)
        }
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("最大并发", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "限制同时运行的任务数，范围 1–8",
                    color = Muted,
                    fontSize = 10.5.sp
                )
            }
            StepButton(Icons.Rounded.Remove, enabled = !busy && gate.maxConcurrent > 1) {
                onLimit(gate.maxConcurrent - 1)
            }
            Box(
                Modifier
                    .padding(horizontal = 12.dp)
                    .width(34.dp),
                contentAlignment = Alignment.Center
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        color = Blue,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "${gate.maxConcurrent}",
                        color = Blue,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            StepButton(Icons.Rounded.Add, enabled = !busy && gate.maxConcurrent < 8) {
                onLimit(gate.maxConcurrent + 1)
            }
        }
    }
}

@Composable
private fun DiskCard(disk: ResourceDisk) {
    val fraction = (disk.usedPercent?.div(100.0) ?: 0.0).coerceIn(0.0, 1.0).toFloat()
    ResourceCard(
        icon = Icons.Rounded.Storage,
        title = disk.label?.takeIf { it.isNotBlank() } ?: disk.name,
        subtitle = listOfNotNull(disk.name, disk.fileSystem, if (disk.system) "系统盘" else null)
            .joinToString(" · ")
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatBytes(disk.usedBytes),
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                " / ${formatBytes(disk.totalBytes)}",
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 3.dp)
            )
            Spacer(Modifier.weight(1f))
            Text(
                disk.usedPercent?.let { "${it.toInt()}%" } ?: "--",
                color = if (fraction > 0.9f) Red else InkSoft,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = fraction,
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (fraction > 0.9f) Red else Green,
            trackColor = Color(0xFFEFF3F0)
        )
        Spacer(Modifier.height(7.dp))
        MiniStat("可用", formatBytes(disk.freeBytes))
    }
}

@Composable
private fun ResourceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDF3FF)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(icon, null, tint = Blue, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = Muted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EmptyResourceCard(onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暂无资源数据", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "点击下方按钮从插件读取内存、磁盘和并发状态。",
                color = Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton("读取资源状态", onRefresh)
        }
    }
}

@Composable
private fun EmptyDiskCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("磁盘", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("插件没有返回磁盘信息。", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, color = Muted, fontSize = 9.5.sp)
        Text(value, color = InkSoft, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = 0.11f)
    ) {
        Text(
            text,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) Color(0xFFF0F4FF) else Color(0xFFF6F7FA),
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(
                icon,
                null,
                tint = if (enabled) Blue else Color(0xFFC5CAD4),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) "${value.toInt()} ${units[index]}"
    else String.format(java.util.Locale.US, "%.1f %s", value, units[index])
}
