package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppLanguage
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.DnsProvider
import com.example.model.RoutingMode

class VpnPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("shadowray_prefs", Context.MODE_PRIVATE)

    fun getSelectedConfigId(): Long {
        return prefs.getLong(KEY_SELECTED_CONFIG_ID, -1L)
    }

    fun setSelectedConfigId(id: Long) {
        prefs.edit().putLong(KEY_SELECTED_CONFIG_ID, id).apply()
    }

    fun getAppSettings(): AppSettings {
        val routingModeStr = prefs.getString(KEY_ROUTING_MODE, RoutingMode.GLOBAL.name) ?: RoutingMode.GLOBAL.name
        val dnsProviderStr = prefs.getString(KEY_DNS_PROVIDER, DnsProvider.CLOUDFLARE.name) ?: DnsProvider.CLOUDFLARE.name
        val customDns1 = prefs.getString(KEY_CUSTOM_DNS1, "1.1.1.1") ?: "1.1.1.1"
        val customDns2 = prefs.getString(KEY_CUSTOM_DNS2, "1.0.0.1") ?: "1.0.0.1"
        val killSwitch = prefs.getBoolean(KEY_KILL_SWITCH, false)
        val autoConnect = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        val bypassLan = prefs.getBoolean(KEY_BYPASS_LAN, true)
        val langStr = prefs.getString(KEY_LANGUAGE, AppLanguage.PERSIAN.name) ?: AppLanguage.PERSIAN.name
        val themeStr = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val bypassedPackages = prefs.getStringSet(KEY_BYPASSED_PACKAGES, emptySet()) ?: emptySet()
        val mux = prefs.getBoolean(KEY_MUX, true)
        val mtu = prefs.getInt(KEY_MTU, 1500)

        return AppSettings(
            routingMode = try { RoutingMode.valueOf(routingModeStr) } catch (e: Exception) { RoutingMode.GLOBAL },
            dnsProvider = try { DnsProvider.valueOf(dnsProviderStr) } catch (e: Exception) { DnsProvider.CLOUDFLARE },
            customDns1 = customDns1,
            customDns2 = customDns2,
            killSwitchEnabled = killSwitch,
            autoConnectOnLaunch = autoConnect,
            bypassLan = bypassLan,
            language = try { AppLanguage.valueOf(langStr) } catch (e: Exception) { AppLanguage.PERSIAN },
            themeMode = try { AppThemeMode.valueOf(themeStr) } catch (e: Exception) { AppThemeMode.DARK },
            bypassedPackages = bypassedPackages,
            muxEnabled = mux,
            mtu = mtu
        )
    }

    fun saveAppSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_ROUTING_MODE, settings.routingMode.name)
            .putString(KEY_DNS_PROVIDER, settings.dnsProvider.name)
            .putString(KEY_CUSTOM_DNS1, settings.customDns1)
            .putString(KEY_CUSTOM_DNS2, settings.customDns2)
            .putBoolean(KEY_KILL_SWITCH, settings.killSwitchEnabled)
            .putBoolean(KEY_AUTO_CONNECT, settings.autoConnectOnLaunch)
            .putBoolean(KEY_BYPASS_LAN, settings.bypassLan)
            .putString(KEY_LANGUAGE, settings.language.name)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putStringSet(KEY_BYPASSED_PACKAGES, settings.bypassedPackages)
            .putBoolean(KEY_MUX, settings.muxEnabled)
            .putInt(KEY_MTU, settings.mtu)
            .apply()
    }

    companion object {
        private const val KEY_SELECTED_CONFIG_ID = "selected_config_id"
        private const val KEY_ROUTING_MODE = "routing_mode"
        private const val KEY_DNS_PROVIDER = "dns_provider"
        private const val KEY_CUSTOM_DNS1 = "custom_dns_1"
        private const val KEY_CUSTOM_DNS2 = "custom_dns_2"
        private const val KEY_KILL_SWITCH = "kill_switch"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_BYPASS_LAN = "bypass_lan"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BYPASSED_PACKAGES = "bypassed_packages"
        private const val KEY_MUX = "mux_enabled"
        private const val KEY_MTU = "mtu_value"
    }
}
