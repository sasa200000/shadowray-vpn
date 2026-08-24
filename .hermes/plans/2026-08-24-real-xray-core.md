# Real VPN Core Integration (libv2ray/Xray) — Implementation Plan

> **For Hermes:** Execute task-by-task. Build verification happens via GitHub Actions (no local Android SDK).

**Goal:** Replace the simulated traffic loop in `LocalVpnService` with the real Xray core (`libv2ray.aar` from 2dust/AndroidLibXrayLite) so configs (VLESS/VMess/Trojan/SS) actually tunnel traffic.

**Architecture:** TUN device (fd from VpnService.Builder) → Xray core with a `socks`-inbound-on-fd / tun2socks bridge. We use the proven v2rayNG pattern: Xray consumes the TUN fd directly via `startLoop(config, fd)` using its built-in TUN inbound support, plus per-app split tunneling through Android's `addDisallowedApplication` (kernel-level, works without core changes).

**Tech Stack:** Kotlin + Jetpack Compose (existing), `io.github.2dust:libv2ray` AAR v26.8.20 (56MB, contains all ABIs), Room DB (existing).

---

## Current state

- `LocalVpnService.kt` establishes TUN, then runs a **fake traffic generator** (`Random.nextLong(...)`) — no packets are ever processed.
- `VpnManager.startVpn()` passes host/port/uuid to the service but there is no core.
- `ProxyConfig` model already has all fields needed to build an Xray JSON config (protocol, uuid, sni, pbk, sid, fp, network, path, host).
- CI builds via GitHub Actions; APK artifacts upload automatically.

## Approach

1. Download `libv2ray.aar` at **CI build time** (NOT committed to git — it's 56MB and GitHub blocks files >100MB anyway; 56MB is fine for git LFS-less repos but bloats clones).
2. Add a pure-Kotlin `XrayConfigBuilder` that converts `ProxyConfig` → Xray JSON (inbounds: tun on fd; outbounds: protocol-specific).
3. Rewrite `LocalVpnService.connect()` to:
   - establish TUN → get fd
   - `Libv2ray.initCoreEnv(...)` once
   - `CoreController(handler).startLoop(json, fd)`
   - poll `queryAllOutboundTrafficStats()` every second for REAL stats
   - `stopLoop()` on disconnect
4. Keep `addDisallowedApplication` for split tunneling (Android-side filtering).
5. Real ping via core `measureOutboundDelay`.

---

### Task 1: Add libv2ray dependency (downloaded in CI)

**Files:**
- Modify: `.github/workflows/build-apk.yml`
- Create: `app/libs/.gitkeep`

**Step 1:** Update workflow — add download step before build:

```yaml
      - name: Download libv2ray core
        run: |
          mkdir -p app/libs
          curl -sL -o app/libs/libv2ray.aar \
            "https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.8.20/libv2ray.aar"
          ls -la app/libs/
```

**Step 2:** In `app/build.gradle.kts`, add to dependencies block:

```kotlin
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```

Note: AGP 9 may need `implementation(files("libs/libv2ray.aar"))` if fileTree is deprecated — try files() first.

**Verify:** Push → Actions run passes "Download libv2ray core" step.

**Commit:** `feat: add real Xray core (libv2ray) dependency downloaded in CI`

---

### Task 2: XrayConfigBuilder — ProxyConfig → Xray JSON

**Files:**
- Create: `app/src/main/java/com/example/vpn/XrayConfigBuilder.kt`

Full file:

```kotlin
package com.example.vpn

import com.example.model.ProxyConfig
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds Xray-core JSON configuration from a ProxyConfig.
 * Inbound: socks5 on localhost:port used by tun2socks layer inside core (tun fd passed separately).
 */
object XrayConfigBuilder {

    fun build(config: ProxyConfig, socksPort: Int = 10808): String {
        val outbound = when (config.protocol) {
            com.example.model.ProxyProtocol.VLESS -> buildVless(config)
            com.example.model.ProxyProtocol.VMESS -> buildVmess(config)
            com.example.model.ProxyProtocol.TROJAN -> buildTrojan(config)
            com.example.model.ProxyProtocol.SHADOWSOCKS -> buildShadowsocks(config)
            else -> buildFallbackSocks(config)
        }

        val root = JSONObject().apply {
            put("log", JSONObject().apply {
                put("loglevel", "warning")
            })
            put("inbounds", JSONArray().put(JSONObject().apply {
                put("tag", "socks")
                put("protocol", "socks")
                put("listen", "127.0.0.1")
                put("port", socksPort)
                put("settings", JSONObject().apply {
                    put("udp", true)
                    put("auth", "noauth")
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().put("http").put("tls"))
                })
            }))
            put("outbounds", JSONArray()
                .put(outbound)
                .put(JSONObject().apply {  // direct fallback
                    put("tag", "direct")
                    put("protocol", "freedom")
                })
            )
        }
        return root.toString()
    }

    private fun streamSettings(config: ProxyConfig): JSONObject {
        val stream = JSONObject()
        val network = config.network.ifBlank { "tcp" }
        stream.put("network", network)

        // TLS / Reality security
        when {
            config.isReality -> {
                stream.put("security", "reality")
                stream.put("realitySettings", JSONObject().apply {
                    put("serverName", config.sni.ifBlank { config.server })
                    put("fingerprint", config.fp.ifBlank { "chrome" })
                    put("publicKey", config.pbk)
                    put("shortId", config.sid)
                })
            }
            config.isTls -> {
                stream.put("security", "tls")
                stream.put("tlsSettings", JSONObject().apply {
                    put("allowInsecure", false)
                    put("serverName", config.sni.ifBlank { config.server })
                    put("fingerprint", config.fp.ifBlank { "chrome" })
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
                put("serviceName", config.path)
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
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", protocol)
            put("settings", JSONObject())
            put("streamSettings", streamSettings(config))
        }
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
                }))
            })
        }
    }

    private fun buildFallbackSocks(config: ProxyConfig): JSONObject {
        return baseOutbound(config, "socks").apply {
            put("settings", JSONObject().apply {
                put("servers", JSONArray().put(JSONObject().apply {
                    put("address", config.server)
                    put("port", config.port)
                }))
            })
        }
    }
}
```

**Verify:** unit test or compile check in CI. Quick sanity test possible locally with `python3 -c` equivalent JSON checks — but Kotlin compile in CI is the gate.

**Commit:** `feat: add XrayConfigBuilder (ProxyConfig -> Xray JSON)`

---

### Task 3: Wire core into LocalVpnService (replace fake stats)

**Files:**
- Modify: `app/src/main/java/com/example/vpn/LocalVpnService.kt` (major rewrite of connect())

Key replacement logic inside `serviceJob = scope.launch { ... }` AFTER `vpnInterface = builder.establish()` succeeds:

```kotlin
val tunFd = vpnInterface!!.fd  // ParcelFileDescriptor fd
VpnManager.log("INFO", "XRAY", "TUN fd=$tunFd, starting Xray core...")

// Init core env once (filesDir as asset placeholder)
try {
    libv2ray.Libv2ray.initCoreEnv(filesDir.absolutePath, "")
} catch (_: Exception) {}

// Build config JSON
val jsonConfig = XrayConfigBuilder.build(activeConfig!!)

coreController = libv2ray.Libv2ray.newCoreController(object : libv2ray.CoreCallbackHandler {
    override fun startup(): Long { VpnManager.log("INFO","XRAY","core startup"); return 0L }
    override fun shutdown(): Long { VpnManager.log("INFO","XRAY","core shutdown"); return 0L }
    override fun onEmitStatus(p0: Long, p1: String?): Long {
        p1?.let { VpnManager.log("INFO","XRAY-LOG", it.take(200)) }
        return 0L
    }
})

coreController!!.startLoop(jsonConfig, tunFd)
VpnManager.onConnected(activeConfig!!, host, country)

// REAL stats polling loop
while (isActive) {
    delay(1000)
    val durationSec = (System.currentTimeMillis() - sessionStartTime) / 1000
    var up = 0L; var down = 0L
    try {
        // format: "tag,direction,value;tag,direction,value;"
        val statsStr = coreController!!.queryAllOutboundTrafficStats()
        for (entry in statsStr.split(";")) {
            if (entry.isBlank()) continue
            val parts = entry.split(",")
            if (parts.size >= 3) {
                val value = parts[2].toLongOrNull() ?: continue
                // direction uplink/downlink cumulative counters - diff them
                if (parts[1].contains("up")) upCum = value else downCum = value
            }
        }
        up = (upCum - lastUp).coerceAtLeast(0); down = (downCum - lastDown).coerceAtLeast(0)
        lastUp = upCum; lastDown = downCum
    } catch (_: Exception) {}
    totalUpBytes += up; totalDownBytes += down

    VpnManager.updateStats(VpnStats(
        uploadSpeedBytesPerSec = up,
        downloadSpeedBytesPerSec = down,
        totalUploadedBytes = totalUpBytes,
        totalDownloadedBytes = totalDownBytes,
        connectedDurationSeconds = durationSec,
        currentPingMs = currentPing,
        publicIp = host,
        countryName = country
    ))
}
```

Also:
- Add field: `private var coreController: libv2ray.CoreController? = null`
- In `disconnect()`: `try { coreController?.stopLoop() } catch(_){}; coreController = null` BEFORE closing vpnInterface.
- Remove all `Random.nextLong` fake stats code.

**IMPORTANT — routing caveat:** The current code routes ALL traffic into TUN (`addRoute("0.0.0.0/0")`) but the core only listens on a SOCKS port — nothing bridges TUN→SOCKS unless we use core's TUN inbound. Two options:

- **Option A (chosen):** Use Xray's built-in tun inbound instead of socks inbound. Change `XrayConfigBuilder` inbound to:
```json
{"tag":"tun-in","protocol":"tun",
 "settings":{"name":"sbtun","mtu":1500},
 "streamSettings":{}}
```
and pass fd via startLoop's second arg — AndroidLibXrayLite's StartLoop(configContent, tunFd) wires the fd automatically (this is exactly what v2rayNG does).

So final inbound becomes protocol "tun" with NO listen/port. Keep sniffing.

**Commit:** `feat: wire real Xray core into LocalVpnService with live stats`

---

### Task 4: Real ping via core

**Files:**
- Modify: `app/src/main/java/com/example/parser/PingTester.kt`

Add method using core measure (works even before connecting):

```kotlin
suspend fun testRealPing(configJson: String): Long {
    return withContext(Dispatchers.IO) {
        try {
            libv2ray.Libv2ray.measureOutboundDelay(
                configJson,
                "https://www.google.com/generate_204"
            )
        } catch (e: Exception) {
            -1L
        }
    }
}
```

Callers (ConfigsScreen ping-all flow) can switch to this later — keep TCP ping as fast pre-filter.

**Commit:** `feat: add real outbound delay measurement via Xray core`

---

### Task 5: CI build + release v1.2

**Files:**
- Modify: `.github/workflows/build-apk.yml` (already done in Task 1)

**Steps:**
1. Push all commits → wait for Actions run (monitor with background loop like previous releases).
2. On success, download artifact, create release `v1.2` with APK.
3. Send user direct download link.

**Risks / gotchas:**
- ⚠️ **APK size jumps ~25MB → ~80MB** (all ABIs in libv2ray.aar). Acceptable trade-off for correctness. Later optimization: abiFilters arm64-v8a only (~30MB total) — ask user first since it drops old 32-bit phones.
- ⚠️ AGP 9.1.1 is bleeding-edge; if `implementation(files(...))` fails to package native .so from AAR, fallback: unzip AAR in CI, copy `jni/*` into `app/src/main/jniLibs/`.
- ⚠️ `startLoop` throws if config invalid — wrap and surface error to UI via `VpnManager.onError(e.message)`.
- ⚠️ First connection needs VPN permission re-grant after app update (normal Android behavior).

**Verification checklist (user-facing):**
1. Install v1.2 APK over existing install.
2. Add a working VLESS/VMess/Trojan config.
3. Connect → notification shows REAL speed numbers (not smooth random waves).
4. Open browser → google.com loads THROUGH proxy.
5. Check app IP display matches server exit country.
