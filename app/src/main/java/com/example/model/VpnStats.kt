package com.example.model

data class VpnStats(
    val uploadSpeedBytesPerSec: Long = 0L,
    val downloadSpeedBytesPerSec: Long = 0L,
    val totalUploadedBytes: Long = 0L,
    val totalDownloadedBytes: Long = 0L,
    val connectedDurationSeconds: Long = 0L,
    val currentPingMs: Long = -1L,
    val publicIp: String = "---.---.---.---",
    val countryCode: String = "UN",
    val countryName: String = "Auto Relay"
)
