package com.example.data

import kotlinx.coroutines.flow.Flow

class TrackerRepository(private val dao: TrackerDao) {

    val pairingConfig: Flow<PairingConfig?> = dao.getPairingConfig()

    val trackerLogs: Flow<List<TrackerLog>> = dao.getLogs()

    suspend fun getPairingConfigDirect(): PairingConfig? {
        return dao.getPairingConfigDirect()
    }

    suspend fun savePairing(
        host: String,
        port: Int,
        clientId: String,
        token: String,
        isPaired: Boolean,
        websocketUrl: String = "",
        locationIntervalSec: Int = 10,
        stationaryIntervalSec: Int = 300
    ) {
        dao.insertPairingConfig(
            PairingConfig(
                serverHost = host,
                serverPort = port,
                clientId = clientId,
                secretToken = token,
                isPaired = isPaired,
                websocketUrl = websocketUrl,
                locationIntervalSec = locationIntervalSec,
                stationaryIntervalSec = stationaryIntervalSec
            )
        )
    }

    suspend fun updatePairingStatus(isPaired: Boolean) {
        val current = dao.getPairingConfigDirect() ?: PairingConfig()
        dao.insertPairingConfig(current.copy(isPaired = isPaired))
    }

    suspend fun addLog(type: String, message: String, status: String = "INFO") {
        dao.insertLog(
            TrackerLog(
                type = type,
                message = message,
                status = status
            )
        )
        try {
            dao.pruneLogs()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    suspend fun clearLogs() {
        dao.clearLogs()
    }

    suspend fun disconnect() {
        dao.clearPairingConfig()
        dao.insertPairingConfig(PairingConfig()) // Reset to default empty config
        addLog("CONNECTION", "Pairing connection disconnected and reset", "WARNING")
    }
}
