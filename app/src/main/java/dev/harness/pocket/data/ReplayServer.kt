package dev.harness.pocket.data

import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import kotlin.concurrent.thread

/** Tiny LAN-only server for sharing one conversation replay page. */
object ReplayServer {
    @Volatile private var socket: ServerSocket? = null
    @Volatile private var body: String = ""
    @Volatile var port: Int = 0
        private set

    @Synchronized
    fun start(html: String): String? {
        stop()
        return runCatching {
            val s = ServerSocket(8787)
            socket = s
            port = s.localPort
            body = html
            thread(isDaemon = true, name = "harness-replay-server") {
                while (!s.isClosed) {
                    val client = runCatching { s.accept() }.getOrNull() ?: break
                    thread(isDaemon = true) {
                        runCatching {
                            client.use {
                                val out = it.getOutputStream()
                                val bytes = body.toByteArray(Charsets.UTF_8)
                                val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                                out.write(header.toByteArray(Charsets.US_ASCII))
                                out.write(bytes)
                                out.flush()
                            }
                        }
                    }
                }
            }
            val ip = lanAddress() ?: "127.0.0.1"
            "http://$ip:$port/"
        }.getOrNull()
    }

    @Synchronized
    fun stop() {
        runCatching { socket?.close() }
        socket = null
        port = 0
    }

    private fun lanAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}
