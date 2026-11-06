package dev.harness.pocket.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("harness_pocket", Context.MODE_PRIVATE)

    fun loadConnection(): Connection? {
        val base = sp.getString("base_url", null) ?: return null
        val token = sp.getString("token", null) ?: return null
        val cookie = sp.getString("cookie", null) ?: return null
        return Connection(base, token, cookie)
    }

    fun saveConnection(connection: Connection) {
        sp.edit()
            .putString("base_url", connection.baseUrl)
            .putString("token", connection.token)
            .putString("cookie", connection.cookie)
            .apply()
    }

    fun saveCookie(cookie: String) {
        sp.edit().putString("cookie", cookie).apply()
    }

    fun lastBase(): String = sp.getString("base_url", null) ?: DEFAULT_BASE

    fun clearConnection() {
        sp.edit().remove("base_url").remove("token").remove("cookie").apply()
    }

    fun loadWorkspaces(): List<WorkspaceInfo> {
        val raw = sp.getString("workspaces", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    val path = item.optString("path")
                    if (id.isBlank() || path.isBlank()) continue
                    add(
                        WorkspaceInfo(
                            workspaceId = id,
                            path = path,
                            title = item.optString("title").ifBlank { path.substringAfterLast('\\') }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveWorkspaces(workspaces: List<WorkspaceInfo>) {
        val array = JSONArray()
        workspaces.forEach { workspace ->
            array.put(
                JSONObject()
                    .put("id", workspace.workspaceId)
                    .put("path", workspace.path)
                    .put("title", workspace.title)
            )
        }
        sp.edit().putString("workspaces", array.toString()).apply()
    }

    companion object {
        const val DEFAULT_BASE = "http://8.130.48.9:18080"
    }
}
