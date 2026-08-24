package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM proxy_configs ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllConfigs(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM proxy_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: Long): ConfigEntity?

    @Query("SELECT * FROM proxy_configs WHERE subscriptionId = :subscriptionId")
    suspend fun getConfigsBySubscription(subscriptionId: Long): List<ConfigEntity>

    @Query("SELECT COUNT(*) FROM proxy_configs")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<ConfigEntity>): List<Long>

    @Update
    suspend fun updateConfig(config: ConfigEntity)

    @Query("UPDATE proxy_configs SET lastPingMs = :pingMs, pingStatus = :status WHERE id = :id")
    suspend fun updatePing(id: Long, pingMs: Long, status: String)

    @Query("UPDATE proxy_configs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM proxy_configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)

    @Query("DELETE FROM proxy_configs WHERE subscriptionId = :subscriptionId")
    suspend fun deleteConfigsBySubscription(subscriptionId: Long)

    @Query("DELETE FROM proxy_configs")
    suspend fun deleteAllConfigs()
}
