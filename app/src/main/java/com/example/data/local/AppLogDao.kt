package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLogDao {
    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT 500")
    fun getAllLogs(): Flow<List<AppLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AppLogEntity)

    @Query("DELETE FROM app_logs")
    suspend fun clearLogs()
}
