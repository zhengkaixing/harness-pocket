package dev.harness.pocket.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

data class PluginAction(val label: String, val url: String)

data class PluginCardModel(
    val title: String,
    val body: String,
    val rows: List<Pair<String, String>>,
    val actions: List<PluginAction> = emptyList()
)

fun parsePluginCard(raw: String): PluginCardModel? {
    return try {
        val obj = JSONObject(raw)
        val title = obj.optString("title").trim()
        val body = obj.optString("body").trim()
        val rows = mutableListOf<Pair<String, String>>()
        val items = obj.optJSONArray("items")
        if (items != null) {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val label = item.optString("label").trim()
                val value = item.optString("value").trim()
                if (label.isNotEmpty()) rows += label to value
            }
        }
        val actions = mutableListOf<PluginAction>()
        val actionArray = obj.optJSONArray("actions")
        if (actionArray != null) {
            for (i in 0 until actionArray.length()) {
                val action = actionArray.optJSONObject(i) ?: continue
                val label = action.optString("label").trim()
                val url = action.optString("url").trim()
                if (label.isNotEmpty() && url.isNotEmpty()) actions += PluginAction(label, url)
            }
        }
        if (title.isEmpty() && body.isEmpty() && rows.isEmpty() && actions.isEmpty()) null
        else PluginCardModel(title, body, rows, actions)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun PluginCardView(card: PluginCardModel) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (card.title.isNotBlank()) {
            Text(card.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (card.body.isNotBlank()) {
            Text(card.body, color = InkSoft, fontSize = 13.sp, lineHeight = 19.sp)
        }
        card.rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth()) {
                Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.width(96.dp))
                Spacer(Modifier.width(8.dp))
                Text(value, color = Ink, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        if (card.actions.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                card.actions.forEach { action ->
                    Surface(
                        onClick = { uriHandler.openUri(action.url) },
                        shape = RoundedCornerShape(8.dp),
                        color = Mist,
                        border = BorderStroke(1.dp, Line)
                    ) {
                        Text(
                            action.label,
                            color = Blue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
        if (card.rows.isEmpty() && card.body.isBlank() && card.actions.isEmpty()) {
            Spacer(Modifier.height(1.dp))
        }
    }
}
