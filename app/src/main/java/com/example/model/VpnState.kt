package com.example.model

sealed class VpnState {
    object Disconnected : VpnState()
    object Connecting : VpnState()
    data class Connected(val config: ProxyConfig) : VpnState()
    object Disconnecting : VpnState()
    data class Error(val message: String) : VpnState()

    val isConnected: Boolean
        get() = this is Connected

    val isConnecting: Boolean
        get() = this is Connecting
}
