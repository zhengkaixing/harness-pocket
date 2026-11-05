package dev.harness.pocket.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun relativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val millis = if (timestamp < 10_000_000_000L) timestamp * 1000L else timestamp
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L} 小时前"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L} 天前"
        else -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(millis))
    }
}

fun cwdLabel(cwd: String?): String? {
    val value = cwd?.trim()?.trimEnd('/', '\\') ?: return null
    if (value.isBlank()) return null
    return value.substringAfterLast('/').substringAfterLast('\\').takeIf { it.isNotBlank() }
}

fun sessionTitle(title: String?, blank: Boolean): String = when {
    !title.isNullOrBlank() -> title
    blank -> "新对话"
    else -> "未命名会话"
}
