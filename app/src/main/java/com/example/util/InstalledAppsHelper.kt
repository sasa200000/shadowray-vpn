package com.example.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfoItem(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)

object InstalledAppsHelper {

    suspend fun getInstalledApps(context: Context): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val list = mutableListOf<AppInfoItem>()

        val myPackage = context.packageName

        for (info in installedApps) {
            if (info.packageName == myPackage) continue
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appName = try {
                packageManager.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                info.packageName
            }
            val icon = try {
                packageManager.getApplicationIcon(info)
            } catch (e: Exception) {
                null
            }

            list.add(
                AppInfoItem(
                    appName = appName,
                    packageName = info.packageName,
                    icon = icon,
                    isSystemApp = isSystem
                )
            )
        }

        list.sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase() }))
    }
}
