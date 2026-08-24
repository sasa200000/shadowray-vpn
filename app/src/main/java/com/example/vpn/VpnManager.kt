package com.example.vpn

import android.content.Context
import android.content.Intent
import com.example.ShadowRayApp
import com.example.data.local.AppLogEntity
import com.example.model.AppSettings
import com.example.model.ProxyConfig
import com.example.model.VpnState
import com.example.model.VpnStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object VpnManager {

    private val _vpnState = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _vpnStats = MutableStateFlow(VpnStats())
    val vpnStats: StateFlow<VpnStats> = _vpnStats.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<Pair<Long, Long>>>(emptyList())
    val speedHistory: StateFlow<List<Pair<Long, Long>>> = _speedHistory.asStateFlow()

    var activeConfig: ProxyConfig? = null
        private set

    var activeSettings: AppSettings = AppSettings()
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startVpn(context: Context, config: ProxyConfig, settings: AppSettings) {
        activeConfig = config
        activeSettings = settings
        _vpnState.value = VpnState.Connecting
        log("INFO", "CORE", "Initiating tunnel connection to ${config.server}:${config.port} (${config.protocol})")

        val intent = Intent(context, LocalVpnService::class.java).apply {
            action = LocalVpnService.ACTION_CONNECT
            putExtra(LocalVpnService.EXTRA_CONFIG_NAME, config.name)
            putExtra(LocalVpnService.EXTRA_CONFIG_HOST, config.server)
            putExtra(LocalVpnService.EXTRA_CONFIG_PORT, config.port)
            putExtra(LocalVpnService.EXTRA_CONFIG_PROTOCOL, config.protocol.name)
            putExtra(LocalVpnService.EXTRA_ROUTING_MODE, settings.routingMode.name)
            putExtra(LocalVpnService.EXTRA_DNS, settings.dnsProvider.primary)
            putExtra(LocalVpnService.EXTRA_MTU, settings.mtu)
            putStringArrayListExtra(LocalVpnService.EXTRA_BYPASS_PACKAGES, ArrayList(settings.bypassedPackages))
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            _vpnState.value = VpnState.Error("Failed to start VPN service: ${e.message}")
            log("ERROR", "CORE", "Failed to start VPN service: ${e.localizedMessage}")
        }
    }

    fun stopVpn(context: Context) {
        _vpnState.value = VpnState.Disconnecting
        log("INFO", "CORE", "Disconnecting active VPN tunnel...")
        val intent = Intent(context, LocalVpnService::class.java).apply {
            action = LocalVpnService.ACTION_DISCONNECT
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            _vpnState.value = VpnState.Disconnected
        }
    }

    fun onConnected(config: ProxyConfig, ip: String, country: String) {
        _vpnState.value = VpnState.Connected(config)
        _vpnStats.value = _vpnStats.value.copy(
            publicIp = ip,
            countryName = country
        )
        log("SUCCESS", "CORE", "VPN Connected successfully. Tunnel interface UP. IP: $ip ($country)")
    }

    fun onDisconnected() {
        _vpnState.value = VpnState.Disconnected
        _vpnStats.value = VpnStats()
        _speedHistory.value = emptyList()
        log("INFO", "CORE", "VPN Tunnel closed. Interface DOWN.")
    }

    fun onError(message: String) {
        _vpnState.value = VpnState.Error(message)
        log("ERROR", "CORE", "VPN Error: $message")
    }

    fun updateStats(stats: VpnStats) {
        _vpnStats.value = stats

        // Keep 20 recent speed points for the live graph
        val currentList = _speedHistory.value.toMutableList()
        currentList.add(Pair(stats.downloadSpeedBytesPerSec, stats.uploadSpeedBytesPerSec))
        if (currentList.size > 24) {
            currentList.removeAt(0)
        }
        _speedHistory.value = currentList
    }

    fun log(level: String, tag: String, message: String) {
        scope.launch {
            try {
                ShadowRayApp.instance.database.appLogDao().insertLog(
                    AppLogEntity(
                        level = level,
                        tag = tag,
                        message = message
                    )
                )
            } catch (_: Exception) {}
        }
    }
}
