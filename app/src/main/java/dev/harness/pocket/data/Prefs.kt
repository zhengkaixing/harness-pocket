package dev.harness.pocket.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("harness_pocket", Context.MODE_PRIVATE)

    fun loadConnection(): Connection? {
        val base = sp.getString("base_url", null) ?: return null
        val token = sp.getString("token", null)?.let(TokenVault::decrypt) ?: return null
        val cookie = sp.getString("cookie", null)?.let(TokenVault::decrypt) ?: return null
        return Connection(base, token, cookie)
            .takeIf { it.token.isNotBlank() && it.cookie.isNotBlank() }
    }

    fun saveConnection(connection: Connection) {
        val hosts = loadHosts().toMutableList()
        val index = hosts.indexOfFirst { it.baseUrl == connection.baseUrl }
        val host = SavedHost(connection.baseUrl, connection.token, connection.cookie)
        if (index >= 0) hosts[index] = host else hosts.add(0, host)
        saveHosts(hosts)
        sp.edit()
            .putString("base_url", connection.baseUrl)
            .putString("token", TokenVault.encrypt(connection.token))
            .putString("cookie", TokenVault.encrypt(connection.cookie))
            .apply()
    }

    fun loadHosts(): List<SavedHost> {
        val raw = sp.getString("hosts", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val base = item.optString("baseUrl")
                    if (base.isBlank()) continue
                    add(
                        SavedHost(
                            baseUrl = base,
                            token = TokenVault.decrypt(item.optString("token")),
                            cookie = TokenVault.decrypt(item.optString("cookie")),
                            label = item.optString("label").ifBlank {
                                base.removePrefix("http://").removePrefix("https://").trimEnd('/')
                            }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveHosts(hosts: List<SavedHost>) {
        val array = JSONArray()
        hosts.forEach { host ->
            array.put(
                JSONObject()
                    .put("baseUrl", host.baseUrl)
                    .put("token", TokenVault.encrypt(host.token))
                    .put("cookie", TokenVault.encrypt(host.cookie))
                    .put("label", host.label)
            )
        }
        sp.edit().putString("hosts", array.toString()).apply()
    }

    fun removeHost(baseUrl: String) {
        saveHosts(loadHosts().filterNot { it.baseUrl == baseUrl })
    }

    fun saveCookie(cookie: String) {
        sp.edit().putString("cookie", TokenVault.encrypt(cookie)).apply()
    }

    fun lastBase(): String = sp.getString("base_url", null) ?: DEFAULT_BASE

    fun deletedSessions(): Set<String> =
        sp.getStringSet("deleted_sessions", emptySet())?.toSet().orEmpty()

    fun deleteSession(sessionId: String) {
        sp.edit().putStringSet("deleted_sessions", deletedSessions() + sessionId).apply()
    }

    fun restoreSession(sessionId: String) {
        sp.edit().putStringSet("deleted_sessions", deletedSessions() - sessionId).apply()
    }

    fun ntfyTopic(): String = sp.getString("ntfy_topic", null).orEmpty()

    fun saveNtfyTopic(topic: String) {
        sp.edit().putString("ntfy_topic", topic.trim()).apply()
    }

    fun ntfyServer(): String = sp.getString("ntfy_server", null)
        ?.takeIf { it.isNotBlank() }
        ?: "https://ntfy.sh"

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
