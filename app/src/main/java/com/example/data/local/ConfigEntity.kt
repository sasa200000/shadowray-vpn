package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.PingStatus
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol

@Entity(tableName = "proxy_configs")
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val protocol: String,
    val rawUri: String,
    val server: String,
    val port: Int,
    val passwordOrUuid: String = "",
    val security: String = "none",
    val network: String = "tcp",
    val path: String = "",
    val host: String = "",
    val sni: String = "",
    val pbk: String = "",
    val sid: String = "",
    val fp: String = "chrome",
    val method: String = "aes-256-gcm",
    val isFavorite: Boolean = false,
    val lastPingMs: Long = -1,
    val pingStatus: String = "IDLE",
    val subscriptionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toProxyConfig(): ProxyConfig {
        return ProxyConfig(
            id = id,
            name = name,
            protocol = ProxyProtocol.fromString(protocol),
            rawUri = rawUri,
            server = server,
            port = port,
            passwordOrUuid = passwordOrUuid,
            security = security,
            network = network,
            path = path,
            host = host,
            sni = sni,
            pbk = pbk,
            sid = sid,
            fp = fp,
            method = method,
            isFavorite = isFavorite,
            lastPingMs = lastPingMs,
            pingStatus = try { PingStatus.valueOf(pingStatus) } catch (e: Exception) { PingStatus.IDLE },
            subscriptionId = subscriptionId,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromProxyConfig(config: ProxyConfig): ConfigEntity {
            return ConfigEntity(
                id = config.id,
                name = config.name,
                protocol = config.protocol.name,
                rawUri = config.rawUri,
                server = config.server,
                port = config.port,
                passwordOrUuid = config.passwordOrUuid,
                security = config.security,
                network = config.network,
                path = config.path,
                host = config.host,
                sni = config.sni,
                pbk = config.pbk,
                sid = config.sid,
                fp = config.fp,
                method = config.method,
                isFavorite = config.isFavorite,
                lastPingMs = config.lastPingMs,
                pingStatus = config.pingStatus.name,
                subscriptionId = config.subscriptionId,
                createdAt = config.createdAt
            )
        }
    }
}
