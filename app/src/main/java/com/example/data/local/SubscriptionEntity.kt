package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val autoUpdate: Boolean = true,
    val lastUpdated: Long = 0L,
    val totalTrafficBytes: Long = 0L,
    val usedTrafficBytes: Long = 0L,
    val expireTimeMs: Long = 0L,
    val serverCount: Int = 0
)
