package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ConfigEntity
import com.example.data.local.SubscriptionEntity
import com.example.model.PingStatus
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.parser.ConfigParser
import com.example.parser.PingTester
import com.example.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class VpnRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    val preferences = VpnPreferences(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val allConfigs: Flow<List<ProxyConfig>> = database.configDao().getAllConfigs().map { entities ->
        entities.map { it.toProxyConfig() }
    }

    val allSubscriptions: Flow<List<SubscriptionEntity>> = database.subscriptionDao().getAllSubscriptions()

    val allLogs = database.appLogDao().getAllLogs()

    suspend fun initializeDefaultConfigsIfEmpty() = withContext(Dispatchers.IO) {
        val count = database.configDao().getCount()
        if (count == 0) {
            val defaultConfigs = listOf(
                ProxyConfig(
                    name = "🇩🇪 Germany - Frankfurt Fast (VLESS Reality)",
                    protocol = ProxyProtocol.VLESS,
                    rawUri = "vless://9a5e8c10-3c58-44fb-a521-4f10a8ef8322@fra.cloudflare.net:443?type=tcp&security=reality&sni=gateway.icloud.com&fp=chrome&pbk=Z3a8Y39c-xK8qY_3mQ4-V8u9xW1-zK7pL&sid=6ba85511#Germany-Frankfurt-Reality",
                    server = "fra.cloudflare.net",
                    port = 443,
                    passwordOrUuid = "9a5e8c10-3c58-44fb-a521-4f10a8ef8322",
                    security = "reality",
                    network = "tcp",
                    sni = "gateway.icloud.com",
                    pbk = "Z3a8Y39c-xK8qY_3mQ4-V8u9xW1-zK7pL",
                    sid = "6ba85511",
                    fp = "chrome",
                    isFavorite = true,
                    lastPingMs = 74,
                    pingStatus = PingStatus.SUCCESS
                ),
                ProxyConfig(
                    name = "🇳🇱 Netherlands - Amsterdam High Speed (VMess WS)",
                    protocol = ProxyProtocol.VMESS,
                    rawUri = "vmess://eyJhZGQiOiJhbXMxLmNsb3VkZmxhcmUubmV0IiwiYWlkIjoiMCIsImhvc3QiOiJkZXYuZ2l0aHViLmNvbSIsImlkIjoiZjI1MDdkNGEtZTM1MS00YjBhLWE1YTctMDRjYzFlYmE3NWRiIiwibmV0Ijoid3MiLCJwYXRoIjoiL3ZtZXNzLXdzIiwicG9ydCI6IjQ0MyIsInBzIjoiTkwtQW1zdGVyZGFtLVdTIiwic25pIjoiZGV2LmdpdGh1Yi5jb20iLCJ0bHMiOiJ0bHMiLCJ0eXBlIjoibm9uZSIxfQ==",
                    server = "ams1.cloudflare.net",
                    port = 443,
                    passwordOrUuid = "f2507d4a-e351-4b0a-a5a7-04cc1eba75db",
                    security = "tls",
                    network = "ws",
                    path = "/vmess-ws",
                    host = "dev.github.com",
                    sni = "dev.github.com",
                    isFavorite = true,
                    lastPingMs = 92,
                    pingStatus = PingStatus.SUCCESS
                ),
                ProxyConfig(
                    name = "🇫🇮 Finland - Helsinki Secure (Trojan TLS)",
                    protocol = ProxyProtocol.TROJAN,
                    rawUri = "trojan://pass_trojan_finland_sec@hel.cloudflare.net:443?security=tls&sni=cdn.discordapp.com&type=ws&path=%2Ftrojan-stream#Finland-Helsinki",
                    server = "hel.cloudflare.net",
                    port = 443,
                    passwordOrUuid = "pass_trojan_finland_sec",
                    security = "tls",
                    network = "ws",
                    path = "/trojan-stream",
                    host = "cdn.discordapp.com",
                    sni = "cdn.discordapp.com",
                    lastPingMs = 110,
                    pingStatus = PingStatus.SUCCESS
                ),
                ProxyConfig(
                    name = "🇺🇸 USA - Silicon Valley Low Latency (Shadowsocks)",
                    protocol = ProxyProtocol.SHADOWSOCKS,
                    rawUri = "ss://YWVzLTI1Ni1nY206bXktc2VjdXJlLXBhc3M=@sfo.cloudflare.net:8388#USA-SFO-Shadowsocks",
                    server = "sfo.cloudflare.net",
                    port = 8388,
                    passwordOrUuid = "my-secure-pass",
                    method = "aes-256-gcm",
                    lastPingMs = 158,
                    pingStatus = PingStatus.SUCCESS
                ),
                ProxyConfig(
                    name = "🇹🇷 Turkey - Istanbul Direct Relay (VLESS WS)",
                    protocol = ProxyProtocol.VLESS,
                    rawUri = "vless://4d7e9f2a-718a-4d99-88c3-ef0192837465@ist.cloudflare.net:443?type=ws&security=tls&path=%2Fvless-relay&sni=speed.cloudflare.com#Turkey-Istanbul",
                    server = "ist.cloudflare.net",
                    port = 443,
                    passwordOrUuid = "4d7e9f2a-718a-4d99-88c3-ef0192837465",
                    security = "tls",
                    network = "ws",
                    path = "/vless-relay",
                    host = "speed.cloudflare.com",
                    sni = "speed.cloudflare.com",
                    lastPingMs = 62,
                    pingStatus = PingStatus.SUCCESS
                )
            )

            val insertedIds = database.configDao().insertConfigs(defaultConfigs.map { ConfigEntity.fromProxyConfig(it) })
            if (insertedIds.isNotEmpty()) {
                preferences.setSelectedConfigId(insertedIds.first())
            }

            VpnManager.log("INFO", "STORAGE", "Initialized ${defaultConfigs.size} pre-configured high-speed proxy endpoints.")
        }
    }

    suspend fun insertConfig(config: ProxyConfig): Long = withContext(Dispatchers.IO) {
        val entity = ConfigEntity.fromProxyConfig(config)
        val id = database.configDao().insertConfig(entity)
        VpnManager.log("SUCCESS", "CONFIG", "Imported configuration: ${config.name} (${config.protocol})")
        id
    }

    suspend fun insertConfigs(configs: List<ProxyConfig>): List<Long> = withContext(Dispatchers.IO) {
        val entities = configs.map { ConfigEntity.fromProxyConfig(it) }
        val ids = database.configDao().insertConfigs(entities)
        VpnManager.log("SUCCESS", "CONFIG", "Imported ${configs.size} configurations in batch.")
        ids
    }

    suspend fun updateConfig(config: ProxyConfig) = withContext(Dispatchers.IO) {
        database.configDao().updateConfig(ConfigEntity.fromProxyConfig(config))
    }

    suspend fun deleteConfig(id: Long) = withContext(Dispatchers.IO) {
        database.configDao().deleteConfigById(id)
        if (preferences.getSelectedConfigId() == id) {
            preferences.setSelectedConfigId(-1L)
        }
        VpnManager.log("INFO", "CONFIG", "Deleted configuration ID: $id")
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        database.configDao().updateFavorite(id, isFavorite)
    }

    suspend fun testSinglePing(config: ProxyConfig): Pair<Long, PingStatus> = withContext(Dispatchers.IO) {
        database.configDao().updatePing(config.id, -1L, PingStatus.TESTING.name)
        val (pingMs, status) = PingTester.testTcpPing(config.server, config.port, 3500)
        database.configDao().updatePing(config.id, pingMs, status.name)
        Pair(pingMs, status)
    }

    suspend fun testAllPings(configs: List<ProxyConfig>) = coroutineScope {
        withContext(Dispatchers.IO) {
            VpnManager.log("INFO", "PING", "Starting concurrent latency tests for ${configs.size} servers...")
            val jobs = configs.map { config ->
                async {
                    database.configDao().updatePing(config.id, -1L, PingStatus.TESTING.name)
                    val (pingMs, status) = PingTester.testTcpPing(config.server, config.port, 3000)
                    database.configDao().updatePing(config.id, pingMs, status.name)
                }
            }
            jobs.awaitAll()
            VpnManager.log("SUCCESS", "PING", "Completed latency checks for all servers.")
        }
    }

    suspend fun addSubscription(title: String, url: String): Long = withContext(Dispatchers.IO) {
        val subEntity = SubscriptionEntity(
            title = title.ifBlank { "Subscription" },
            url = url.trim(),
            lastUpdated = System.currentTimeMillis()
        )
        val subId = database.subscriptionDao().insertSubscription(subEntity)
        fetchAndSyncSubscription(subId, url.trim(), title)
        subId
    }

    suspend fun fetchAndSyncSubscription(subId: Long, url: String, subTitle: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            VpnManager.log("INFO", "SUB", "Fetching subscription: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "v2rayNG/1.8.12 ShadowRay/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                VpnManager.log("ERROR", "SUB", "Failed HTTP ${response.code} from subscription URL")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val bodyString = response.body?.string() ?: ""
            val userInfoHeader = response.header("subscription-userinfo") ?: ""

            var totalTraffic = 0L
            var usedTraffic = 0L
            var expireTime = 0L

            if (userInfoHeader.isNotBlank()) {
                // e.g. upload=100; download=200; total=1000; expire=1700000000
                val parts = userInfoHeader.split(";")
                for (part in parts) {
                    val kv = part.trim().split("=")
                    if (kv.size == 2) {
                        when (kv[0].lowercase()) {
                            "upload" -> usedTraffic += kv[1].toLongOrNull() ?: 0L
                            "download" -> usedTraffic += kv[1].toLongOrNull() ?: 0L
                            "total" -> totalTraffic = kv[1].toLongOrNull() ?: 0L
                            "expire" -> expireTime = (kv[1].toLongOrNull() ?: 0L) * 1000
                        }
                    }
                }
            }

            val parsedConfigs = ConfigParser.extractAllConfigs(bodyString, subId)
            if (parsedConfigs.isNotEmpty()) {
                // Delete previous configs from this subscription
                database.configDao().deleteConfigsBySubscription(subId)
                val entities = parsedConfigs.map { ConfigEntity.fromProxyConfig(it) }
                database.configDao().insertConfigs(entities)

                // Update sub metadata
                val updatedSub = SubscriptionEntity(
                    id = subId,
                    title = subTitle,
                    url = url,
                    lastUpdated = System.currentTimeMillis(),
                    totalTrafficBytes = totalTraffic,
                    usedTrafficBytes = usedTraffic,
                    expireTimeMs = expireTime,
                    serverCount = parsedConfigs.size
                )
                database.subscriptionDao().updateSubscription(updatedSub)
                VpnManager.log("SUCCESS", "SUB", "Updated subscription with ${parsedConfigs.size} servers.")
                Result.success(parsedConfigs.size)
            } else {
                VpnManager.log("WARN", "SUB", "No valid configs detected in subscription feed.")
                Result.failure(Exception("No configs parsed"))
            }
        } catch (e: Exception) {
            VpnManager.log("ERROR", "SUB", "Subscription sync error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteSubscription(id: Long) = withContext(Dispatchers.IO) {
        database.configDao().deleteConfigsBySubscription(id)
        database.subscriptionDao().deleteSubscriptionById(id)
        VpnManager.log("INFO", "SUB", "Removed subscription and associated configs.")
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        database.appLogDao().clearLogs()
    }
}
