package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM pairing_config WHERE id = 1 LIMIT 1")
    fun getPairingConfig(): Flow<PairingConfig?>

    @Query("SELECT * FROM pairing_config WHERE id = 1 LIMIT 1")
    suspend fun getPairingConfigDirect(): PairingConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairingConfig(config: PairingConfig)

    @Query("DELETE FROM pairing_config")
    suspend fun clearPairingConfig()

    @Query("SELECT * FROM tracker_logs ORDER BY timestamp DESC LIMIT 100")
    fun getLogs(): Flow<List<TrackerLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TrackerLog)

    @Query("DELETE FROM tracker_logs WHERE id NOT IN (SELECT id FROM (SELECT id FROM tracker_logs ORDER BY timestamp DESC LIMIT 1000))")
    suspend fun pruneLogs()

    @Query("DELETE FROM tracker_logs")
    suspend fun clearLogs()
}
