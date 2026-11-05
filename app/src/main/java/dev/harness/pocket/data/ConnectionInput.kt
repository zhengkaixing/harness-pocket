package dev.harness.pocket.data

import android.net.Uri

object ConnectionInput {
    data class Parsed(val baseUrl: String, val token: String)

    fun parse(server: String, tokenInput: String): Parsed {
        var serverText = server.trim()
        var token = tokenInput.trim()

        for (candidate in listOf(tokenInput.trim(), server.trim())) {
            if (candidate.isEmpty() || !candidate.contains("token=")) continue
            val withScheme = if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
                candidate
            } else {
                "http://$candidate"
            }
            try {
                val uri = Uri.parse(withScheme)
                val parsedToken = uri.getQueryParameter("token")?.trim()
                if (!parsedToken.isNullOrBlank()) {
                    token = parsedToken
                    uri.authority?.let { serverText = "${uri.scheme}://$it" }
                }
            } catch (_: Exception) {
                // Ignore malformed candidates and fall through to validation below.
            }
        }

        if (serverText.isBlank()) throw ApiException("请填写服务器地址")
        if (token.isBlank()) throw ApiException("请粘贴访问令牌，或带 ?token= 的完整链接")

        var base = serverText
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "http://$base"
        }
        val uri = Uri.parse(base)
        val scheme = uri.scheme?.lowercase()
        val authority = uri.authority
        if (scheme != "http" && scheme != "https") {
            throw ApiException("服务器地址必须以 http:// 或 https:// 开头")
        }
        if (authority.isNullOrBlank()) {
            throw ApiException("服务器地址无效，请检查 IP 和端口")
        }
        return Parsed("$scheme://$authority", token)
    }
}
