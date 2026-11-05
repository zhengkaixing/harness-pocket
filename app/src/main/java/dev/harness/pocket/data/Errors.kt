package dev.harness.pocket.data

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

fun friendlyError(error: Throwable): String = when (error) {
    is AuthException -> error.message ?: "登录已过期，请重新连接"
    is ApiException -> error.message ?: "请求失败"
    is UnknownHostException -> "找不到服务器，请检查地址或网络"
    is ConnectException -> "无法连接服务器，确认电脑上的 dsh web 正在运行"
    is SocketTimeoutException -> "连接超时，请稍后重试"
    is SSLException -> "HTTPS 握手失败，请检查证书或改用 http"
    is IOException -> error.message ?: "网络异常"
    else -> error.message ?: "发生未知错误"
}
