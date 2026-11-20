package com.example.data

import kotlinx.coroutines.flow.Flow

class TrackerRepository(private val dao: TrackerDao) {

    val pairingConfig: Flow<PairingConfig?> = dao.getPairingConfig()

    val trackerLogs: Flow<List<TrackerLog>> = dao.getLogs()

    val serverTargets: Flow<List<ServerTarget>> = dao.getServerTargets()

    suspend fun getPairingConfigDirect(): PairingConfig? {
        return dao.getPairingConfigDirect()
    }

    suspend fun getServerTargetsDirect(): List<ServerTarget> {
        return dao.getServerTargetsDirect()
    }

    suspend fun upsertServerTarget(
        id: Long = 0,
        name: String,
        host: String,
        port: Int,
        websocketUrl: String = "",
        clientId: String = "",
        secretToken: String = ""
    ): ServerTarget {
        val cleanHost = host.trim()
        val cleanWs = websocketUrl.trim()
        val existing = if (id > 0) {
            dao.getServerTargetsDirect().find { it.id == id }
        } else {
            dao.findServerTargetByEndpoint(cleanHost, port, cleanWs)
        }

        val resolvedName = name.trim().ifEmpty {
            existing?.name?.takeIf { it.isNotBlank() } ?: cleanHost.ifEmpty { "Server" }
        }
        val resolvedClientId = clientId.ifEmpty { existing?.clientId ?: "" }
        val resolvedToken = secretToken.ifEmpty { existing?.secretToken ?: "" }

        val targetToSave = ServerTarget(
            id = existing?.id ?: id,
            name = resolvedName,
            serverHost = cleanHost,
            serverPort = port,
            websocketUrl = cleanWs,
            clientId = resolvedClientId,
            secretToken = resolvedToken,
            lastUsed = System.currentTimeMillis()
        )
        val newId = dao.insertServerTarget(targetToSave)
        return if (targetToSave.id == 0L) targetToSave.copy(id = newId) else targetToSave
    }

    suspend fun deleteServerTarget(id: Long) {
        dao.deleteServerTarget(id)
    }

    suspend fun savePairing(
        host: String,
        port: Int,
        clientId: String,
        token: String,
        isPaired: Boolean,
        websocketUrl: String = "",
        locationIntervalSec: Int? = null,
        stationaryIntervalSec: Int? = null
    ) {
        val existing = dao.getPairingConfigDirect()
        dao.insertPairingConfig(
            PairingConfig(
                serverHost = host,
                serverPort = port,
                clientId = clientId,
                secretToken = token,
                isPaired = isPaired,
                websocketUrl = websocketUrl,
                locationIntervalSec = locationIntervalSec ?: existing?.locationIntervalSec ?: 10,
                stationaryIntervalSec = stationaryIntervalSec ?: existing?.stationaryIntervalSec ?: 300
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

