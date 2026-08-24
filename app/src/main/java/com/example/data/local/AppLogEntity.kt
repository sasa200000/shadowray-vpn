package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_logs")
data class AppLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO", // INFO, WARN, ERROR, SUCCESS
    val tag: String = "CORE",
    val message: String
)
