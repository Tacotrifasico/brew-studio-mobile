package com.example.data.repository

import java.util.UUID

data class SocialPublicationTarget(
    val entityId: String,
    val visibility: String,
    val targetUserId: String?
)

/** Impide publicar identidades ficticias o envíos directos ambiguos. */
object SocialPublicationPolicy {
    fun validate(remoteId: String?, visibility: String, targetUserId: String?): Result<SocialPublicationTarget> {
        val entityId = remoteId?.trim()?.takeIf(::isUsableUuid)
            ?: return Result.failure(IllegalStateException("La fórmula sigue segura en este dispositivo. Sincronízala con Supabase antes de compartirla."))
        val normalizedVisibility = visibility.trim().uppercase()
        if (normalizedVisibility !in setOf("PUBLIC", "DIRECT")) {
            return Result.failure(IllegalArgumentException("Selecciona Muro público o Buzón directo."))
        }
        val recipient = targetUserId?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedVisibility == "DIRECT" && !isUsableUuid(recipient)) {
            return Result.failure(IllegalArgumentException("El Buzón directo necesita un identificador válido del receptor hasta que Axcis habilite el envío por alias."))
        }
        return Result.success(
            SocialPublicationTarget(
                entityId = entityId,
                visibility = normalizedVisibility,
                targetUserId = if (normalizedVisibility == "DIRECT") recipient else null
            )
        )
    }

    private fun isUsableUuid(value: String?): Boolean = try {
        value != null && UUID.fromString(value) != UUID(0, 0)
    } catch (_: IllegalArgumentException) {
        false
    }
}
