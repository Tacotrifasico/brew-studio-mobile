package com.example.data.repository

/** Capacidades reales del sincronizador Android mientras se completa Supabase. */
object AndroidSyncPolicy {
    fun canRetryTechnique(syncStatus: String, remoteId: String?): Boolean = when {
        syncStatus == "SYNCED" -> false
        remoteId == null -> syncStatus == "PENDING_CREATE" || syncStatus == "ERROR"
        else -> syncStatus == "ERROR" // Reintento de pasos tras un alta parcial.
    }

    fun techniqueAwaitsBackend(syncStatus: String, remoteId: String?): Boolean =
        syncStatus != "SYNCED" && !canRetryTechnique(syncStatus, remoteId)
}
