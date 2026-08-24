package com.example.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.PingStatus
import com.example.model.ProxyConfig
import com.example.model.VpnStats
import com.example.parser.PingTester
import com.example.util.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class LocalVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var coreController: CoreController? = null
    private var coreEnvInitialized = false

    private var sessionStartTime = 0L
    private var totalUpBytes = 0L
    private var totalDownBytes = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                disconnect()
                return START_NOT_STICKY
            }
            ACTION_CONNECT -> {
                val configName = intent.getStringExtra(EXTRA_CONFIG_NAME) ?: "ShadowRay Proxy"
                val host = intent.getStringExtra(EXTRA_CONFIG_HOST) ?: "1.1.1.1"
                val port = intent.getIntExtra(EXTRA_CONFIG_PORT, 443)
                val dns = intent.getStringExtra(EXTRA_DNS) ?: "1.1.1.1"
                val mtu = intent.getIntExtra(EXTRA_MTU, 1500)
                val bypassPackages = intent.getStringArrayListExtra(EXTRA_BYPASS_PACKAGES) ?: arrayListOf()

                connect(configName, host, port, dns, mtu, bypassPackages)
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun initCoreEnvIfNeeded() {
        if (coreEnvInitialized) return
        try {
            Libv2ray.initCoreEnv(filesDir.absolutePath, "")
            coreEnvInitialized = true
            VpnManager.log("INFO", "XRAY", "Core env initialized. ${Libv2ray.checkVersionX()}")
        } catch (t: Throwable) {
            coreEnvInitialized = false
            VpnManager.log("ERROR", "XRAY", "initCoreEnv failed: ${t.message}")
        }
    }

    private fun connect(
        configName: String,
        host: String,
        port: Int,
        dns: String,
        mtu: Int,
        bypassPackages: List<String>
    ) {
        serviceJob?.cancel()
        sessionStartTime = System.currentTimeMillis()
        totalUpBytes = 0L
        totalDownBytes = 0L

        startForegroundNotification(configName, "Connecting tunnel...")

        serviceJob = scope.launch {
            try {
                VpnManager.log("INFO", "SERVICE", "Configuring TUN interface...")

                val activeConfig: ProxyConfig = VpnManager.activeConfig
                    ?: run {
                        VpnManager.onError("No active configuration found.")
                        disconnect()
                        return@launch
                    }

                // Build the real Xray JSON config BEFORE touching the TUN device.
                val jsonConfig = XrayConfigBuilder.build(activeConfig)

                val builder = Builder()
                    .setSession("ShadowRay: $configName")
                    .setMtu(mtu.coerceIn(1280, 1500))
                    .addAddress("26.26.26.1", 24)

                // Setup DNS
                try {
                    builder.addDnsServer(dns.ifBlank { "1.1.1.1" })
                } catch (e: Exception) {
                    builder.addDnsServer("1.1.1.1")
                }

                // Route all traffic through the tunnel
                builder.addRoute("0.0.0.0", 0)

                // CRITICAL: exclude our own app from the VPN, otherwise the Xray core's own
                // outbound connection to the proxy server loops back into the TUN device and
                // the whole phone's network stalls (v2rayNG does exactly this).
                try {
                    builder.addDisallowedApplication(packageName)
                    VpnManager.log("INFO", "ROUTING", "Self package excluded from VPN: $packageName")
                } catch (e: Exception) {
                    VpnManager.log("WARN", "ROUTING", "Could not exclude self: ${e.message}")
                }

                // Split tunneling per app (kernel-level filtering by Android)
                for (pkg in bypassPackages) {
                    try {
                        builder.addDisallowedApplication(pkg)
                        VpnManager.log("INFO", "ROUTING", "Bypassing package from VPN: $pkg")
                    } catch (_: Exception) {}
                }

                vpnInterface = builder.establish()
                if (vpnInterface == null) {
                    VpnManager.onError("Failed to establish VPN interface. Permission denied or another VPN is active.")
                    stopSelf()
                    return@launch
                }
                val tunFd = vpnInterface!!.fd
                VpnManager.log("SUCCESS", "SERVICE", "TUN established. fd=$tunFd")

                // Initialize and start the real Xray core on this TUN fd
                initCoreEnvIfNeeded()
                if (!coreEnvInitialized) {
                    VpnManager.onError("Xray engine failed to load on this device. Please reinstall the app.")
                    disconnect()
                    return@launch
                }

                coreController = Libv2ray.newCoreController(object : CoreCallbackHandler {
                    override fun startup(): Long {
                        VpnManager.log("INFO", "XRAY", "Core startup callback.")
                        return 0L
                    }
                    override fun shutdown(): Long {
                        VpnManager.log("INFO", "XRAY", "Core shutdown callback.")
                        return 0L
                    }
                    override fun onEmitStatus(id: Long, message: String?): Long {
                        message?.let { VpnManager.log("INFO", "XRAY-LOG", it.take(200)) }
                        return 0L
                    }
                })

                try {
                    coreController!!.startLoop(jsonConfig, tunFd)
                    VpnManager.log("SUCCESS", "XRAY", "Core loop started with real outbound.")
                } catch (t: Throwable) {
                    VpnManager.onError("Xray core failed to start: ${t.message}")
                    disconnect()
                    return@launch
                }

                // Determine exit display info
                val country = when {
                    host.contains(".de") || host.contains("germany") -> "Germany"
                    host.contains(".nl") || host.contains("netherlands") -> "Netherlands"
                    host.contains(".fi") || host.contains("finland") -> "Finland"
                    host.contains(".fr") || host.contains("france") -> "France"
                    host.contains(".us") || host.contains("usa") -> "United States"
                    host.contains(".uk") || host.contains("london") -> "United Kingdom"
                    host.contains(".tr") || host.contains("turkey") -> "Turkey"
                    host.contains(".sg") || host.contains("singapore") -> "Singapore"
                    else -> "Proxy Tunnel"
                }
                VpnManager.onConnected(activeConfig, host, country)

                // ---- REAL stats loop: cumulative counters from the core ----
                var lastPingCheck = System.currentTimeMillis()
                var currentPing = PingTester.testTcpPing(host, port, 3000).first
                if (currentPing <= 0) currentPing = -1L

                var upCum = 0L
                var downCum = 0L
                var lastUp = 0L
                var lastDown = 0L

                while (isActive) {
                    delay(1000)
                    val durationSec = (System.currentTimeMillis() - sessionStartTime) / 1000

                    var upDelta = 0L
                    var downDelta = 0L
                    try {
                        // format: "tag,direction,value;tag,direction,value;"
                        val statsStr = coreController?.queryAllOutboundTrafficStats() ?: ""
                        for (entry in statsStr.split(";")) {
                            if (entry.isBlank()) continue
                            val parts = entry.split(",")
                            if (parts.size >= 3) {
                                val value = parts[2].trim().toLongOrNull() ?: continue
                                val direction = parts[1].trim().lowercase()
                                if (direction.contains("up")) upCum = value
                                else if (direction.contains("down")) downCum = value
                            }
                        }
                        upDelta = (upCum - lastUp).coerceAtLeast(0)
                        downDelta = (downCum - lastDown).coerceAtLeast(0)
                        lastUp = upCum
                        lastDown = downCum
                    } catch (_: Exception) {}

                    totalUpBytes += upDelta
                    totalDownBytes += downDelta

                    // Periodic TCP ping to server as liveness indicator
                    if (System.currentTimeMillis() - lastPingCheck > 10_000) {
                        lastPingCheck = System.currentTimeMillis()
                        val (newPing, status) = PingTester.testTcpPing(host, port, 2000)
                        if (status == PingStatus.SUCCESS && newPing > 0) currentPing = newPing
                    }

                    val stats = VpnStats(
                        uploadSpeedBytesPerSec = upDelta,
                        downloadSpeedBytesPerSec = downDelta,
                        totalUploadedBytes = totalUpBytes,
                        totalDownloadedBytes = totalDownBytes,
                        connectedDurationSeconds = durationSec,
                        currentPingMs = currentPing,
                        publicIp = host,
                        countryName = country
                    )
                    VpnManager.updateStats(stats)
                    updateForegroundNotification(
                        configName,
                        "↓ ${Formatters.formatSpeed(downDelta)}  ↑ ${Formatters.formatSpeed(upDelta)} | ${Formatters.formatDuration(durationSec)}"
                    )

                    // If core died unexpectedly, surface it
                    try {
                        if (coreController?.isRunning == false && isActive) {
                            VpnManager.onError("Xray core stopped unexpectedly.")
                            break
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                VpnManager.onError("Tunnel exception: ${e.message}")
                disconnect()
            }
        }
    }

    private fun disconnect() {
        serviceJob?.cancel()
        serviceJob = null
        try {
            coreController?.stopLoop()
        } catch (_: Exception) {}
        coreController = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        VpnManager.onDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification(title: String, content: String) {
        try {
            val notification = buildNotification(title, content)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } catch (e: Exception) {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            VpnManager.log("WARN", "SERVICE", "Notification start notice: ${e.message}")
        }
    }

    private fun updateForegroundNotification(title: String, content: String) {
        try {
            val notification = buildNotification(title, content)
            val manager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun buildNotification(title: String, content: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, LocalVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("ShadowRay: $title")
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "shadowray_vpn_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_CONNECT = "com.example.action.CONNECT"
        const val ACTION_DISCONNECT = "com.example.action.DISCONNECT"

        const val EXTRA_CONFIG_NAME = "extra_config_name"
        const val EXTRA_CONFIG_HOST = "extra_config_host"
        const val EXTRA_CONFIG_PORT = "extra_config_port"
        const val EXTRA_CONFIG_PROTOCOL = "extra_config_protocol"
        const val EXTRA_ROUTING_MODE = "extra_routing_mode"
        const val EXTRA_DNS = "extra_dns"
        const val EXTRA_MTU = "extra_mtu"
        const val EXTRA_BYPASS_PACKAGES = "extra_bypass_packages"
    }
}
