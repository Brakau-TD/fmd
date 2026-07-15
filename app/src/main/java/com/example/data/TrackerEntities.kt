package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pairing_config")
data class PairingConfig(
    @PrimaryKey val id: Int = 1,
    val serverHost: String = "",
    val serverPort: Int = 8080,
    val clientId: String = "",
    val secretToken: String = "",
    val isPaired: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val websocketUrl: String = "",
    val locationIntervalSec: Int = 10,
    val stationaryIntervalSec: Int = 300
)

@Entity(tableName = "tracker_logs")
data class TrackerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "CONNECTION", "LOCATION", "COMMAND", "SENSOR"
    val message: String,
    val status: String = "INFO" // "INFO", "SUCCESS", "WARNING", "ERROR"
)
