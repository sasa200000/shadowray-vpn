package com.example.model

enum class RoutingMode {
    GLOBAL,
    BYPASS_IRAN_LAN,
    CUSTOM_APP_LIST
}

enum class DnsProvider(val title: String, val primary: String, val secondary: String) {
    CLOUDFLARE("Cloudflare (1.1.1.1)", "1.1.1.1", "1.0.0.1"),
    GOOGLE("Google (8.8.8.8)", "8.8.8.8", "8.8.4.4"),
    ADGUARD("AdGuard Ad-Block (94.140.14.14)", "94.140.14.14", "94.140.15.15"),
    QUAD9("Quad9 Security (9.9.9.9)", "9.9.9.9", "149.112.112.112"),
    CUSTOM("Custom DNS", "", "")
}

enum class AppLanguage {
    PERSIAN,
    ENGLISH
}

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class AppSettings(
    val routingMode: RoutingMode = RoutingMode.GLOBAL,
    val dnsProvider: DnsProvider = DnsProvider.CLOUDFLARE,
    val customDns1: String = "1.1.1.1",
    val customDns2: String = "1.0.0.1",
    val killSwitchEnabled: Boolean = false,
    val autoConnectOnLaunch: Boolean = false,
    val bypassLan: Boolean = true,
    val language: AppLanguage = AppLanguage.PERSIAN,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val bypassedPackages: Set<String> = emptySet(),
    val muxEnabled: Boolean = true,
    val mtu: Int = 1500
)
