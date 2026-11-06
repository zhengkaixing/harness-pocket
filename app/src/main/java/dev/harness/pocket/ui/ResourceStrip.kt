package dev.harness.pocket.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.harness.pocket.data.ResourceStatus
import java.util.Locale

@Composable
fun ResourceStrip(
    status: ResourceStatus?,
    onClick: () -> Unit
) {
    if (status == null) return
    val memory = status.memory
    val memoryPercent = memory.usedPercent
        ?: if (memory.totalBytes > 0) memory.usedBytes * 100.0 / memory.totalBytes else 0.0
    val commitPercent = memory.commitPercent

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("内存", color = Muted, fontSize = 9.5.sp)
        Spacer(Modifier.width(3.dp))
        Text(
            "${memoryPercent.formatOne()}%",
            color = InkSoft,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(4.dp))
        MiniMeter(memoryPercent / 100.0, Blue)
        if (commitPercent != null) {
            Spacer(Modifier.width(8.dp))
            Text("提交", color = Muted, fontSize = 9.5.sp)
            Spacer(Modifier.width(3.dp))
            Text(
                "${commitPercent.formatOne()}%",
                color = InkSoft,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(4.dp))
            MiniMeter(commitPercent / 100.0, Color(0xFFF0A33A))
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(1.dp).height(10.dp).background(Line))
        val disks = status.disks.sortedByDescending { it.system }.take(3)
        disks.forEach { disk ->
            Spacer(Modifier.width(8.dp))
            Text(
                "${disk.name.trimEnd(':', '\\')}: 剩 ${compactBytes(disk.freeBytes)}",
                color = Muted,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun MiniMeter(fraction: Double, color: Color) {
    val safe = fraction.coerceIn(0.0, 1.0).toFloat()
    Box(
        Modifier
            .width(34.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(Color(0xFFE9EDF5))
    ) {
        Box(
            Modifier
                .fillMaxWidth(safe)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

private fun Double.formatOne(): String =
    if (this >= 100) "100" else String.format(Locale.US, "%.1f", this)

private fun compactBytes(bytes: Long): String {
    val value = bytes.coerceAtLeast(0L).toDouble()
    return when {
        value >= 1024.0 * 1024 * 1024 * 1024 ->
            String.format(Locale.US, "%.1fT", value / (1024.0 * 1024 * 1024 * 1024))
        value >= 1024.0 * 1024 * 1024 ->
            String.format(Locale.US, "%.1fG", value / (1024.0 * 1024 * 1024))
        else -> String.format(Locale.US, "%.0fM", value / (1024.0 * 1024))
    }
}
