package com.example.ui.screens

/**
 * Mantiene honesta la interfaz mientras Axcis completa el backend.
 * Hoy SyncRepository sólo puede representar técnicas sin inventar datos.
 * Las recetas actuales incluyen ingredientes y pasos que el contrato remoto
 * heredado todavía no admite.
 */
data class StoragePendingBreakdown(
    val retryableNow: Int,
    val awaitingBackend: Int
)

fun storagePendingBreakdown(
    beans: Int,
    recipes: Int,
    retryableTechniques: Int,
    backendTechniques: Int,
    grinders: Int,
    equipment: Int,
    cups: Int,
    tastings: Int,
    experiments: Int
): StoragePendingBreakdown = StoragePendingBreakdown(
    retryableNow = retryableTechniques,
    awaitingBackend = beans + recipes + backendTechniques + grinders + equipment + cups + tastings + experiments
)
