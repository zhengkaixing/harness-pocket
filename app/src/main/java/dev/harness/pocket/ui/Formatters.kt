package dev.harness.pocket.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.harness.pocket.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun relativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val millis = if (timestamp < 10_000_000_000L) timestamp * 1000L else timestamp
    val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> stringResource(R.string.relative_now)
        diff < 3_600_000L -> stringResource(R.string.relative_minutes, (diff / 60_000L).toInt())
        diff < 86_400_000L -> stringResource(R.string.relative_hours, (diff / 3_600_000L).toInt())
        diff < 7 * 86_400_000L -> stringResource(R.string.relative_days, (diff / 86_400_000L).toInt())
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(millis))
    }
}

fun cwdLabel(cwd: String?): String? {
    val value = cwd?.trim()?.trimEnd('/', '\\') ?: return null
    if (value.isBlank()) return null
    return value.substringAfterLast('/').substringAfterLast('\\').takeIf { it.isNotBlank() }
}

@Composable
fun formatDuration(millis: Long): String {
    val seconds = (millis.coerceAtLeast(0L) / 1000L)
    return when {
        seconds < 60 -> stringResource(R.string.duration_seconds, seconds.toInt())
        seconds < 3600 -> {
            val m = seconds / 60
            val s = seconds % 60
            if (s == 0L) stringResource(R.string.duration_minutes, m.toInt())
            else stringResource(R.string.duration_min_sec, m.toInt(), s.toInt())
        }
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            if (m == 0L) stringResource(R.string.duration_hours, h.toInt())
            else stringResource(R.string.duration_hour_min, h.toInt(), m.toInt())
        }
    }
}

fun formatTokens(count: Long): String = when {
    count < 1_000 -> count.toString()
    count < 1_000_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    else -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
}

@Composable
fun sessionTitle(title: String?, blank: Boolean): String = when {
    !title.isNullOrBlank() -> title
    blank -> stringResource(R.string.session_new)
    else -> stringResource(R.string.session_untitled)
}
