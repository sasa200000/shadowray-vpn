package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ShadowRayApp
import com.example.data.local.AppLogEntity
import com.example.data.local.SubscriptionEntity
import com.example.model.AppSettings
import com.example.model.PingStatus
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.model.VpnState
import com.example.model.VpnStats
import com.example.parser.ConfigParser
import com.example.util.LocalizationHelper
import com.example.vpn.VpnManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConfigFilter {
    ALL,
    VLESS,
    VMESS,
    TROJAN,
    SHADOWSOCKS,
    FAVORITES
}

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ShadowRayApp).repository

    val vpnState: StateFlow<VpnState> = VpnManager.vpnState
    val vpnStats: StateFlow<VpnStats> = VpnManager.vpnStats
    val speedHistory: StateFlow<List<Pair<Long, Long>>> = VpnManager.speedHistory

    val allConfigs: StateFlow<List<ProxyConfig>> = repository.allConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<AppLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settings = MutableStateFlow(repository.preferences.getAppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _selectedConfigId = MutableStateFlow(repository.preferences.getSelectedConfigId())
    val selectedConfigId: StateFlow<Long> = _selectedConfigId.asStateFlow()

    val selectedConfig: StateFlow<ProxyConfig?> = combine(allConfigs, _selectedConfigId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(ConfigFilter.ALL)
    val activeFilter: StateFlow<ConfigFilter> = _activeFilter.asStateFlow()

    private val _isTestingAll = MutableStateFlow(false)
    val isTestingAll: StateFlow<Boolean> = _isTestingAll.asStateFlow()

    private val _isUpdatingSubs = MutableStateFlow(false)
    val isUpdatingSubs: StateFlow<Boolean> = _isUpdatingSubs.asStateFlow()

    val filteredConfigs: StateFlow<List<ProxyConfig>> = combine(
        allConfigs,
        _searchQuery,
        _activeFilter
    ) { list, query, filter ->
        list.filter { config ->
            val matchesQuery = query.isBlank() ||
                    config.name.contains(query, ignoreCase = true) ||
                    config.server.contains(query, ignoreCase = true) ||
                    config.protocol.name.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ConfigFilter.ALL -> true
                ConfigFilter.VLESS -> config.protocol == ProxyProtocol.VLESS
                ConfigFilter.VMESS -> config.protocol == ProxyProtocol.VMESS
                ConfigFilter.TROJAN -> config.protocol == ProxyProtocol.TROJAN
                ConfigFilter.SHADOWSOCKS -> config.protocol == ProxyProtocol.SHADOWSOCKS
                ConfigFilter.FAVORITES -> config.isFavorite
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDefaultConfigsIfEmpty()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ConfigFilter) {
        _activeFilter.value = filter
    }

    fun selectConfig(config: ProxyConfig) {
        _selectedConfigId.value = config.id
        repository.preferences.setSelectedConfigId(config.id)
    }

    fun toggleConnect(context: Context) {
        val currentState = vpnState.value
        if (currentState is VpnState.Connected || currentState is VpnState.Connecting) {
            VpnManager.stopVpn(context)
        } else {
            val config = selectedConfig.value
            if (config == null) {
                Toast.makeText(
                    context,
                    LocalizationHelper.getString("no_config_selected", settings.value.language),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            VpnManager.startVpn(context, config, settings.value)
        }
    }

    fun importConfigsFromText(text: String, context: Context): Int {
        val extracted = ConfigParser.extractAllConfigs(text)
        if (extracted.isNotEmpty()) {
            viewModelScope.launch {
                val ids = repository.insertConfigs(extracted)
                if (ids.isNotEmpty() && selectedConfig.value == null) {
                    _selectedConfigId.value = ids.first()
                    repository.preferences.setSelectedConfigId(ids.first())
                }
            }
            Toast.makeText(
                context,
                "${LocalizationHelper.getString("import_success", settings.value.language)} (${extracted.size})",
                Toast.LENGTH_SHORT
            ).show()
            return extracted.size
        } else {
            Toast.makeText(
                context,
                LocalizationHelper.getString("import_fail", settings.value.language),
                Toast.LENGTH_SHORT
            ).show()
            return 0
        }
    }

    fun addManualConfig(config: ProxyConfig, context: Context) {
        viewModelScope.launch {
            val id = repository.insertConfig(config)
            _selectedConfigId.value = id
            repository.preferences.setSelectedConfigId(id)
            Toast.makeText(
                context,
                LocalizationHelper.getString("import_success", settings.value.language),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateConfig(config: ProxyConfig) {
        viewModelScope.launch {
            repository.updateConfig(config)
        }
    }

    fun deleteConfig(id: Long) {
        viewModelScope.launch {
            repository.deleteConfig(id)
        }
    }

    fun toggleFavorite(config: ProxyConfig) {
        viewModelScope.launch {
            repository.toggleFavorite(config.id, !config.isFavorite)
        }
    }

    fun testSinglePing(config: ProxyConfig) {
        viewModelScope.launch {
            repository.testSinglePing(config)
        }
    }

    fun testAllPings() {
        if (_isTestingAll.value) return
        viewModelScope.launch {
            _isTestingAll.value = true
            try {
                repository.testAllPings(allConfigs.value)
            } finally {
                _isTestingAll.value = false
            }
        }
    }

    fun autoPickFastestServer(context: Context) {
        val validConfigs = allConfigs.value.filter { it.lastPingMs > 0 && it.pingStatus == PingStatus.SUCCESS }
        if (validConfigs.isNotEmpty()) {
            val fastest = validConfigs.minByOrNull { it.lastPingMs }
            if (fastest != null) {
                selectConfig(fastest)
                Toast.makeText(
                    context,
                    "${fastest.name} (${fastest.lastPingMs}ms)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            testAllPings()
        }
    }

    fun addSubscription(title: String, url: String, context: Context) {
        viewModelScope.launch {
            try {
                val subId = repository.addSubscription(title, url)
                if (subId > 0) {
                    Toast.makeText(
                        context,
                        LocalizationHelper.getString("sub_update_success", settings.value.language),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateSubscription(subscription: SubscriptionEntity, context: Context) {
        viewModelScope.launch {
            _isUpdatingSubs.value = true
            try {
                val result = repository.fetchAndSyncSubscription(subscription.id, subscription.url, subscription.title)
                if (result.isSuccess) {
                    Toast.makeText(
                        context,
                        "${subscription.title}: ${result.getOrNull()} servers updated",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isUpdatingSubs.value = false
            }
        }
    }

    fun updateAllSubscriptions(context: Context) {
        if (_isUpdatingSubs.value) return
        viewModelScope.launch {
            _isUpdatingSubs.value = true
            try {
                for (sub in subscriptions.value) {
                    repository.fetchAndSyncSubscription(sub.id, sub.url, sub.title)
                }
                Toast.makeText(
                    context,
                    LocalizationHelper.getString("sub_update_success", settings.value.language),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isUpdatingSubs.value = false
            }
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch {
            repository.deleteSubscription(id)
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        repository.preferences.saveAppSettings(newSettings)
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }
}
