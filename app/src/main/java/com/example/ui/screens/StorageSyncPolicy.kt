package com.example.ui.screens

/**
 * Mantiene honesta la interfaz mientras Axcis completa el backend.
 * Hoy SyncRepository sólo implementa subida y descarga de recetas y técnicas.
 */
data class StoragePendingBreakdown(
    val retryableNow: Int,
    val awaitingBackend: Int
)

fun storagePendingBreakdown(
    beans: Int,
    recipes: Int,
    techniques: Int,
    grinders: Int,
    equipment: Int,
    cups: Int,
    tastings: Int,
    experiments: Int
): StoragePendingBreakdown = StoragePendingBreakdown(
    retryableNow = recipes + techniques,
    awaitingBackend = beans + grinders + equipment + cups + tastings + experiments
)
