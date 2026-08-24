package com.example.parser

import android.net.Uri
import android.util.Base64
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ConfigParser {

    fun parseUri(raw: String): ProxyConfig? {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
            trimmed.startsWith("wg://", ignoreCase = true) || trimmed.startsWith("wireguard://", ignoreCase = true) -> parseWireguard(trimmed)
            trimmed.startsWith("socks5://", ignoreCase = true) || trimmed.startsWith("socks://", ignoreCase = true) -> parseSocks(trimmed)
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                if (trimmed.contains("@")) parseHttpProxy(trimmed) else null
            }
            else -> null
        }
    }

    private fun parseVless(uriStr: String): ProxyConfig? {
        return try {
            val uri = Uri.parse(uriStr)
            val userInfo = uri.userInfo ?: uri.authority?.substringBefore("@") ?: ""
            val hostAndPort = (if (uri.authority?.contains("@") == true) uri.authority?.substringAfter("@") else uri.authority) ?: ""
            val host = if (hostAndPort.contains(":")) hostAndPort.substringBefore(":") else hostAndPort
            val port = if (hostAndPort.contains(":")) hostAndPort.substringAfter(":").substringBefore("?").toIntOrNull() ?: 443 else 443
            
            val remark = decodeUrl(uri.fragment ?: "VLESS-$host")
            val type = uri.getQueryParameter("type") ?: "tcp"
            val security = uri.getQueryParameter("security") ?: "none"
            val path = decodeUrl(uri.getQueryParameter("path") ?: "")
            val hostParam = decodeUrl(uri.getQueryParameter("host") ?: "")
            val sni = uri.getQueryParameter("sni") ?: hostParam
            val pbk = uri.getQueryParameter("pbk") ?: ""
            val sid = uri.getQueryParameter("sid") ?: ""
            val fp = uri.getQueryParameter("fp") ?: "chrome"

            ProxyConfig(
                name = remark.ifBlank { "VLESS Server" },
                protocol = ProxyProtocol.VLESS,
                rawUri = uriStr,
                server = host,
                port = port,
                passwordOrUuid = userInfo,
                security = security,
                network = type,
                path = path,
                host = hostParam,
                sni = sni,
                pbk = pbk,
                sid = sid,
                fp = fp
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVmess(uriStr: String): ProxyConfig? {
        return try {
            val b64 = uriStr.substring(8).trim()
            val decodedJson = decodeBase64Safe(b64)
            val json = JSONObject(decodedJson)

            val remark = json.optString("ps", "VMess Server")
            val add = json.optString("add", "")
            val port = json.optInt("port", 443)
            val id = json.optString("id", "")
            val net = json.optString("net", "tcp")
            val type = json.optString("type", "none")
            val host = json.optString("host", "")
            val path = json.optString("path", "")
            val tls = json.optString("tls", "none")
            val sni = json.optString("sni", host)

            ProxyConfig(
                name = remark,
                protocol = ProxyProtocol.VMESS,
                rawUri = uriStr,
                server = add,
                port = port,
                passwordOrUuid = id,
                security = tls,
                network = net,
                path = path,
                host = host,
                sni = sni
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTrojan(uriStr: String): ProxyConfig? {
        return try {
            val uri = Uri.parse(uriStr)
            val password = uri.userInfo ?: uri.authority?.substringBefore("@") ?: ""
            val hostAndPort = (if (uri.authority?.contains("@") == true) uri.authority?.substringAfter("@") else uri.authority) ?: ""
            val host = if (hostAndPort.contains(":")) hostAndPort.substringBefore(":") else hostAndPort
            val port = if (hostAndPort.contains(":")) hostAndPort.substringAfter(":").substringBefore("?").toIntOrNull() ?: 443 else 443

            val remark = decodeUrl(uri.fragment ?: "Trojan-$host")
            val sni = uri.getQueryParameter("sni") ?: uri.getQueryParameter("peer") ?: host
            val type = uri.getQueryParameter("type") ?: "tcp"
            val security = uri.getQueryParameter("security") ?: "tls"
            val path = decodeUrl(uri.getQueryParameter("path") ?: "")
            val hostHeader = decodeUrl(uri.getQueryParameter("host") ?: "")

            ProxyConfig(
                name = remark.ifBlank { "Trojan Server" },
                protocol = ProxyProtocol.TROJAN,
                rawUri = uriStr,
                server = host,
                port = port,
                passwordOrUuid = password,
                security = security,
                network = type,
                path = path,
                host = hostHeader,
                sni = sni
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseShadowsocks(uriStr: String): ProxyConfig? {
        return try {
            val withoutScheme = uriStr.substring(5)
            val fragment = if (withoutScheme.contains("#")) withoutScheme.substringAfter("#") else ""
            val remark = decodeUrl(fragment).ifBlank { "Shadowsocks" }
            val mainPart = withoutScheme.substringBefore("#")

            if (mainPart.contains("@")) {
                // SIP002 format: ss://BASE64(method:pass)@host:port
                val userPart = mainPart.substringBefore("@")
                val hostPort = mainPart.substringAfter("@")
                val decodedUser = decodeBase64Safe(userPart)
                val method = if (decodedUser.contains(":")) decodedUser.substringBefore(":") else "aes-256-gcm"
                val pass = if (decodedUser.contains(":")) decodedUser.substringAfter(":") else decodedUser
                val host = hostPort.substringBefore(":")
                val port = hostPort.substringAfter(":").substringBefore("/").substringBefore("?").toIntOrNull() ?: 8388

                ProxyConfig(
                    name = remark,
                    protocol = ProxyProtocol.SHADOWSOCKS,
                    rawUri = uriStr,
                    server = host,
                    port = port,
                    passwordOrUuid = pass,
                    method = method
                )
            } else {
                // Legacy Base64: ss://BASE64(method:pass@host:port)
                val decoded = decodeBase64Safe(mainPart)
                val user = decoded.substringBefore("@")
                val hostPort = decoded.substringAfter("@")
                val method = if (user.contains(":")) user.substringBefore(":") else "aes-256-gcm"
                val pass = if (user.contains(":")) user.substringAfter(":") else user
                val host = hostPort.substringBefore(":")
                val port = hostPort.substringAfter(":").toIntOrNull() ?: 8388

                ProxyConfig(
                    name = remark,
                    protocol = ProxyProtocol.SHADOWSOCKS,
                    rawUri = uriStr,
                    server = host,
                    port = port,
                    passwordOrUuid = pass,
                    method = method
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWireguard(uriStr: String): ProxyConfig? {
        return try {
            val uri = Uri.parse(uriStr)
            val host = uri.host ?: "127.0.0.1"
            val port = if (uri.port > 0) uri.port else 51820
            val remark = decodeUrl(uri.fragment ?: "WireGuard-$host")
            val pubKey = uri.getQueryParameter("publickey") ?: uri.getQueryParameter("pk") ?: ""
            val privKey = uri.userInfo ?: ""

            ProxyConfig(
                name = remark,
                protocol = ProxyProtocol.WIREGUARD,
                rawUri = uriStr,
                server = host,
                port = port,
                passwordOrUuid = privKey,
                pbk = pubKey
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSocks(uriStr: String): ProxyConfig? {
        return try {
            val uri = Uri.parse(uriStr)
            val host = uri.host ?: "127.0.0.1"
            val port = if (uri.port > 0) uri.port else 1080
            val user = uri.userInfo ?: ""
            val remark = decodeUrl(uri.fragment ?: "SOCKS5-$host")

            ProxyConfig(
                name = remark,
                protocol = ProxyProtocol.SOCKS5,
                rawUri = uriStr,
                server = host,
                port = port,
                passwordOrUuid = user
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHttpProxy(uriStr: String): ProxyConfig? {
        return try {
            val uri = Uri.parse(uriStr)
            val host = uri.host ?: "127.0.0.1"
            val port = if (uri.port > 0) uri.port else 8080
            val user = uri.userInfo ?: ""
            val remark = decodeUrl(uri.fragment ?: "HTTP-$host")

            ProxyConfig(
                name = remark,
                protocol = ProxyProtocol.HTTP,
                rawUri = uriStr,
                server = host,
                port = port,
                passwordOrUuid = user
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts all valid proxy configs from a string (single line, multiline, or mixed text).
     */
    fun extractAllConfigs(text: String, subscriptionId: Long? = null): List<ProxyConfig> {
        val result = mutableListOf<ProxyConfig>()
        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                val config = parseUri(trimmed)
                if (config != null) {
                    result.add(if (subscriptionId != null) config.copy(subscriptionId = subscriptionId) else config)
                }
            }
        }
        if (result.isEmpty()) {
            // Check if whole text is base64
            val decoded = decodeBase64Safe(text.trim())
            if (decoded.isNotBlank() && decoded != text.trim()) {
                return extractAllConfigs(decoded, subscriptionId)
            }
        }
        return result
    }

    /**
     * Serializes a ProxyConfig into standard URI string.
     */
    fun serializeToUri(config: ProxyConfig): String {
        return when (config.protocol) {
            ProxyProtocol.VLESS -> {
                val queryParams = mutableListOf<String>()
                queryParams.add("type=${config.network.ifBlank { "tcp" }}")
                queryParams.add("security=${config.security.ifBlank { "none" }}")
                if (config.path.isNotBlank()) queryParams.add("path=${encodeUrl(config.path)}")
                if (config.host.isNotBlank()) queryParams.add("host=${encodeUrl(config.host)}")
                if (config.sni.isNotBlank()) queryParams.add("sni=${encodeUrl(config.sni)}")
                if (config.pbk.isNotBlank()) queryParams.add("pbk=${config.pbk}")
                if (config.sid.isNotBlank()) queryParams.add("sid=${config.sid}")
                if (config.fp.isNotBlank()) queryParams.add("fp=${config.fp}")
                
                val query = queryParams.joinToString("&")
                "vless://${config.passwordOrUuid}@${config.server}:${config.port}?$query#${encodeUrl(config.name)}"
            }
            ProxyProtocol.VMESS -> {
                val json = JSONObject().apply {
                    put("v", "2")
                    put("ps", config.name)
                    put("add", config.server)
                    put("port", config.port)
                    put("id", config.passwordOrUuid)
                    put("aid", "0")
                    put("net", config.network.ifBlank { "tcp" })
                    put("type", "none")
                    put("host", config.host)
                    put("path", config.path)
                    put("tls", config.security.ifBlank { "none" })
                    put("sni", config.sni)
                }
                val b64 = Base64.encodeToString(json.toString().toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                "vmess://$b64"
            }
            ProxyProtocol.TROJAN -> {
                val query = "security=${config.security.ifBlank { "tls" }}&sni=${encodeUrl(config.sni.ifBlank { config.server })}" +
                        if (config.path.isNotBlank()) "&path=${encodeUrl(config.path)}" else ""
                "trojan://${config.passwordOrUuid}@${config.server}:${config.port}?$query#${encodeUrl(config.name)}"
            }
            ProxyProtocol.SHADOWSOCKS -> {
                val user = "${config.method}:${config.passwordOrUuid}"
                val b64User = Base64.encodeToString(user.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                "ss://$b64User@${config.server}:${config.port}#${encodeUrl(config.name)}"
            }
            ProxyProtocol.WIREGUARD -> {
                "wg://${config.passwordOrUuid}@${config.server}:${config.port}?publickey=${config.pbk}#${encodeUrl(config.name)}"
            }
            ProxyProtocol.SOCKS5 -> {
                "socks5://${config.passwordOrUuid}@${config.server}:${config.port}#${encodeUrl(config.name)}"
            }
            ProxyProtocol.HTTP -> {
                "http://${config.passwordOrUuid}@${config.server}:${config.port}#${encodeUrl(config.name)}"
            }
        }
    }

    private fun decodeBase64Safe(input: String): String {
        return try {
            val clean = input.replace("-", "+").replace("_", "/").trim()
            val padded = clean.padEnd(clean.length + (4 - clean.length % 4) % 4, '=')
            val bytes = Base64.decode(padded, Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            input
        }
    }

    private fun decodeUrl(value: String): String {
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            value
        }
    }

    private fun encodeUrl(value: String): String {
        return try {
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            value
        }
    }
}
