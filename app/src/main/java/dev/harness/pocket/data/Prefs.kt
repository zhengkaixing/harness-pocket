package dev.harness.pocket.data

import android.content.Context

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

    companion object {
        const val DEFAULT_BASE = "http://127.0.0.1:8080"
    }
}
