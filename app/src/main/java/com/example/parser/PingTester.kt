package com.example.parser

import com.example.model.PingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    /**
     * REAL latency test through the proxy outbound via Xray core.
     * Returns round-trip ms or -1 on failure.
     */
    suspend fun testRealPing(config: com.example.model.ProxyConfig): Long {
        return withContext(Dispatchers.IO) {
            try {
                val json = com.example.vpn.XrayConfigBuilder.build(config)
                libv2ray.Libv2ray.measureOutboundDelay(
                    json,
                    "https://www.google.com/generate_204"
                )
            } catch (e: Exception) {
                -1L
            }
        }
    }

    /**
     * Performs a TCP handshake test to measure latency in milliseconds.
     */
    suspend fun testTcpPing(host: String, port: Int, timeoutMs: Int = 3000): Pair<Long, PingStatus> {
        return withContext(Dispatchers.IO) {
            if (host.isBlank() || port <= 0 || port > 65535) {
                return@withContext Pair(-1L, PingStatus.ERROR)
            }

            var socket: Socket? = null
            try {
                val start = System.currentTimeMillis()
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val duration = System.currentTimeMillis() - start
                Pair(duration, PingStatus.SUCCESS)
            } catch (e: java.net.SocketTimeoutException) {
                Pair(-1L, PingStatus.TIMEOUT)
            } catch (e: Exception) {
                // If direct socket failed, test fallback resolution
                Pair(-1L, PingStatus.ERROR)
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Fallback HTTP ping to test general internet routing latency.
     */
    suspend fun testHttpPing(urlStr: String = "https://www.google.com/generate_204", timeoutMs: Int = 3500): Long {
        return withContext(Dispatchers.IO) {
            try {
                val start = System.currentTimeMillis()
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.instanceFollowRedirects = false
                connection.requestMethod = "GET"
                connection.connect()
                val code = connection.responseCode
                val duration = System.currentTimeMillis() - start
                connection.disconnect()
                if (code in 200..399) duration else -1L
            } catch (e: Exception) {
                -1L
            }
        }
    }
}
