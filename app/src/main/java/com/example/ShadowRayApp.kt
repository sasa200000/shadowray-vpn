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
