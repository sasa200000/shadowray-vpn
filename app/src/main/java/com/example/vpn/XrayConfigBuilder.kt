package com.example.vpn

import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a real Xray-core JSON configuration from a ProxyConfig.
 *
 * Inbound: TUN interface (the fd is passed to startLoop by AndroidLibXrayLite,
 * same approach as v2rayNG). Outbound: protocol-specific proxy server.
 */
object XrayConfigBuilder {

    fun build(config: ProxyConfig): String {
        val outbound = when (config.protocol) {
            ProxyProtocol.VLESS -> buildVless(config)
            ProxyProtocol.VMESS -> buildVmess(config)
            ProxyProtocol.TROJAN -> buildTrojan(config)
            ProxyProtocol.SHADOWSOCKS -> buildShadowsocks(config)
            ProxyProtocol.WIREGUARD -> null // not supported by Xray outbound
            ProxyProtocol.SOCKS5 -> buildSocks(config)
            ProxyProtocol.HTTP -> buildHttp(config)
        } ?: buildSocks(config)

        val root = JSONObject().apply {
            put("log", JSONObject().apply {
                put("loglevel", "warning")
            })
            // stats+policy: required so the app can read REAL traffic numbers from the core
            put("stats", JSONObject())
            put("policy", JSONObject().apply {
                put("levels", JSONObject().apply {
                    put("8", JSONObject().apply {
                        put("handshake", 4)
                        put("connIdle", 300)
                        put("uplinkOnly", 1)
                        put("downlinkOnly", 1)
                    })
                })
                put("system", JSONObject().apply {
                    put("statsOutboundUplink", true)
                    put("statsOutboundDownlink", true)
                })
            })
            put("inbounds", JSONArray().put(JSONObject().apply {
                put("tag", "tun")
                put("protocol", "tun")
                put("settings", JSONObject().apply {
                    put("name", "xray0")
                    put("MTU", 1500)
                    put("userLevel", 8)
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().put("http").put("tls").put("quic"))
                })
            }))
            put("outbounds", JSONArray()
                .put(outbound)
                .put(JSONObject().apply { // direct fallback
                    put("tag", "direct")
                    put("protocol", "freedom")
                })
                .put(JSONObject().apply { // block outbound
                    put("tag", "block")
                    put("protocol", "blackhole")
                })
            )
            // Route private/LAN ranges directly (never through the proxy), everything else -> proxy
            // NOTE: uses explicit CIDR ranges instead of geoip:private so the core can start
            // even if geoip.dat is missing on the device.
            put("routing", JSONObject().apply {
                put("domainStrategy", "AsIs")
                put("rules", JSONArray()
                    .put(JSONObject().apply {
                        put("type", "field")
                        put("ip", JSONArray()
                            .put("10.0.0.0/8")
                            .put("172.16.0.0/12")
                            .put("192.168.0.0/16")
                            .put("127.0.0.0/8")
                            .put("169.254.0.0/16")
                            .put("224.0.0.0/4")
                            .put("::1/128")
                            .put("fc00::/7")
                        )
                        put("outboundTag", "direct")
                    })
                )
            })
        }
        return root.toString()
    }

    private fun streamSettings(config: ProxyConfig): JSONObject? {
        val network = config.network.ifBlank { "tcp" }
        val hasTls = config.isTls || config.isReality
        if (network == "tcp" && !hasTls) return null

        val stream = JSONObject()
        stream.put("network", network)

        when {
            config.isReality -> {
                stream.put("security", "reality")
                stream.put("realitySettings", JSONObject().apply {
                    put("serverName", config.sni.ifBlank { config.server })
                    put("fingerprint", config.fp.ifBlank { "chrome" })
                    if (config.pbk.isNotBlank()) put("publicKey", config.pbk)
                    if (config.sid.isNotBlank()) put("shortId", config.sid)
                })
            }
            hasTls -> {
                stream.put("security", "tls")
                stream.put("tlsSettings", JSONObject().apply {
                    put("allowInsecure", false)
                    put("serverName", config.sni.ifBlank { config.server })
                    if (config.fp.isNotBlank()) put("fingerprint", config.fp)
                })
            }
        }

        when (network) {
            "ws" -> stream.put("wsSettings", JSONObject().apply {
                put("path", config.path.ifBlank { "/" })
                if (config.host.isNotBlank()) {
                    put("headers", JSONObject().put("Host", config.host))
                }
            })
            "grpc" -> stream.put("grpcSettings", JSONObject().apply {
                if (config.path.isNotBlank()) put("serviceName", config.path)
            })
            "h2", "http" -> stream.put("httpSettings", JSONObject().apply {
                put("path", config.path.ifBlank { "/" })
                if (config.host.isNotBlank()) {
                    put("host", JSONArray().put(config.host))
                }
            })
        }
        return stream
    }

    private fun baseOutbound(config: ProxyConfig, protocol: String): JSONObject {
        val obj = JSONObject().apply {
            put("tag", "proxy")
            put("protocol", protocol)
            put("settings", JSONObject())
        }
        streamSettings(config)?.let { obj.put("streamSettings", it) }
        return obj
    }

    private fun buildVless(config: ProxyConfig): JSONObject {
        return baseOutbound(config, "vless").apply {
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                    put("users", JSONArray().put(JSONObject().apply {
                        put("id", config.passwordOrUuid)
                        put("encryption", "none")
                        put("flow", "")
                        put("level", 0)
                    }))
                }))
            })
        }
    }

    private fun buildVmess(config: ProxyConfig): JSONObject {
        return baseOutbound(config, "vmess").apply {
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                    put("users", JSONArray().put(JSONObject().apply {
                        put("id", config.passwordOrUuid)
                        put("security", "auto")
                        put("alterId", 0)
                        put("level", 0)
                    }))
                }))
            })
        }
    }

    private fun buildTrojan(config: ProxyConfig): JSONObject {
        return baseOutbound(config, "trojan").apply {
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                    put("password", config.passwordOrUuid)
                    put("level", 0)
                }))
            })
        }
    }

    private fun buildShadowsocks(config: ProxyConfig): JSONObject {
        return baseOutbound(config, "shadowsocks").apply {
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                    put("method", config.method.ifBlank { "aes-256-gcm" })
                    put("password", config.passwordOrUuid)
                    put("level", 0)
                }))
            })
        }
    }

    private fun buildSocks(config: ProxyConfig): JSONObject {
        return baseOutbound(config, "socks").apply {
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                }))
            })
        }
    }

    private fun buildHttp(config: ProxyConfig): JSONObject {
        return baseOutbound(config, "http").apply {
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                }))
            })
        }
    }
}
