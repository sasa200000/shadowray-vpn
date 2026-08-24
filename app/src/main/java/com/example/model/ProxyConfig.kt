package com.example.model

enum class ProxyProtocol {
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS,
    WIREGUARD,
    SOCKS5,
    HTTP;

    companion object {
        fun fromString(value: String): ProxyProtocol {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                VLESS
            }
        }
    }
}

enum class PingStatus {
    IDLE,
    TESTING,
    SUCCESS,
    TIMEOUT,
    ERROR
}

data class ProxyConfig(
    val id: Long = 0,
    val name: String,
    val protocol: ProxyProtocol,
    val rawUri: String,
    val server: String,
    val port: Int,
    val passwordOrUuid: String = "",
    val security: String = "none", // tls, reality, none
    val network: String = "tcp", // tcp, ws, grpc, h2, http
    val path: String = "",
    val host: String = "",
    val sni: String = "",
    val pbk: String = "", // Reality public key
    val sid: String = "", // Reality short id
    val fp: String = "chrome", // Fingerprint
    val method: String = "aes-256-gcm", // For Shadowsocks
    val isFavorite: Boolean = false,
    val lastPingMs: Long = -1,
    val pingStatus: PingStatus = PingStatus.IDLE,
    val subscriptionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayAddress: String
        get() = "$server:$port"

    val displayProtocolBadge: String
        get() = protocol.name

    val isReality: Boolean
        get() = security.equals("reality", ignoreCase = true) || pbk.isNotBlank()

    val isTls: Boolean
        get() = security.equals("tls", ignoreCase = true) || isReality
}
