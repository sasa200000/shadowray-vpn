package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.VpnRepository
import com.example.data.local.AppDatabase
import com.example.vpn.LocalVpnService

class ShadowRayApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: VpnRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        repository = VpnRepository(this, database)

        createNotificationChannels()
        copyGeoAssetsIfNeeded()
    }

    /**
     * Copies geoip.dat / geosite.dat from APK assets to filesDir so the Xray core
     * can find them (asset path is set via xray.location.asset = filesDir).
     * Skips copy if the file already exists with a plausible size.
     */
    private fun copyGeoAssetsIfNeeded() {
        for (name in listOf("geoip.dat", "geosite.dat")) {
            try {
                val out = java.io.File(filesDir, name)
                if (out.exists() && out.length() > 1_000_000) continue
                assets.open(name).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) {
                // asset missing — routing rules with geoip:/geosite: will fail at start,
                // but the tunnel itself can still work without them.
            }
        }
    }

    fun onCoreLibraryLoaded() {
        coreAvailable = true
    }

    var coreAvailable: Boolean = false
        private set

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LocalVpnService.NOTIFICATION_CHANNEL_ID,
                "ShadowRay VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing VPN connection status and data traffic"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: ShadowRayApp
            private set
    }
}
