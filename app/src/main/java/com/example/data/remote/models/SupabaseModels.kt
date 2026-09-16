package com.example.data.remote.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// AUTHENTICATION REQUEST / RESPONSES

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SignInRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "refresh_token") val refreshToken: String,
    @param:Json(name = "expires_in") val expiresIn: Long,
    val user: SupabaseUser
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String,
    val email: String,
    @param:Json(name = "created_at") val createdAt: String
)


// DATA TABLES MODELS

@JsonClass(generateAdapter = true)
data class RemoteProfile(
    val id: String,
    val email: String,
    @param:Json(name = "display_name") val displayName: String,
    val handle: String?,
    @param:Json(name = "avatar_url") val avatarUrl: String?,
    @param:Json(name = "avatar_color") val avatarColor: String?,
    val role: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class RemoteBean(
    val id: String? = null,
    @param:Json(name = "user_id") val userId: String? = null,
    val roaster: String?,
    val name: String,
    val origin: String?,
    val altitude: String?,
    val process: String?,
    @param:Json(name = "roast_date") val roastDate: String?,
    @param:Json(name = "first_use_date") val firstUseDate: String?,
    val notes: String?,
    val status: String?,
    @param:Json(name = "stock_grams") val stockGrams: Float?,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteRecipe(
    val id: String? = null,
    @param:Json(name = "user_id") val userId: String? = null,
    @param:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @param:Json(name = "owner_display_name") val ownerDisplayName: String?,
    val name: String,
    val method: String?,
    @param:Json(name = "bean_id") val beanId: String?,
    @param:Json(name = "grinder_id") val grinderId: String?,
    @param:Json(name = "technique_id") val techniqueId: String?,
    @param:Json(name = "coffee_grams") val coffeeGrams: Float?,
    @param:Json(name = "water_ml") val waterMl: Int?,
    val ratio: Float?,
    val temperature: Int?,
    val clicks: String?,
    val notes: String?,
    val visibility: String?,
    @param:Json(name = "is_shared") val isShared: Boolean?,
    @param:Json(name = "original_author_user_id") val originalAuthorUserId: String?,
    @param:Json(name = "original_author_name") val originalAuthorName: String?,
    @param:Json(name = "original_entity_id") val originalEntityId: String?,
    @param:Json(name = "imported_from_share_id") val importedFromShareId: String?,
    @param:Json(name = "copy_mode") val copyMode: String?,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteTechnique(
    val id: String? = null,
    @param:Json(name = "user_id") val userId: String? = null,
    @param:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @param:Json(name = "owner_display_name") val ownerDisplayName: String?,
    val name: String,
    val method: String?,
    @param:Json(name = "coffee_grams") val coffeeGrams: Float?,
    @param:Json(name = "water_ml") val waterMl: Int?,
    val ratio: Float?,
    val temperature: Int?,
    @param:Json(name = "grind_clicks") val grindClicks: String?,
    @param:Json(name = "grinder_id") val grinderId: String?,
    @param:Json(name = "bean_id") val beanId: String?,
    val notes: String?,
    val visibility: String?,
    @param:Json(name = "is_shared") val isShared: Boolean?,
    @param:Json(name = "original_author_user_id") val originalAuthorUserId: String?,
    @param:Json(name = "original_author_name") val originalAuthorName: String?,
    @param:Json(name = "original_entity_id") val originalEntityId: String?,
    @param:Json(name = "imported_from_share_id") val importedFromShareId: String?,
    @param:Json(name = "copy_mode") val copyMode: String?,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteTechniqueStep(
    val id: String? = null,
    @param:Json(name = "technique_id") val techniqueId: String,
    @param:Json(name = "user_id") val userId: String? = null,
    @param:Json(name = "step_order") val stepOrder: Int,
    val title: String?,
    @param:Json(name = "duration_sec") val durationSec: Int?,
    @param:Json(name = "water_add_ml") val waterAddMl: Int?,
    @param:Json(name = "target_water_ml") val targetWaterMl: Int?,
    val gesture: String?,
    val intensity: String?,
    val note: String?,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteShare(
    val id: String,
    @param:Json(name = "entity_type") val entityType: String, // "recipe", "technique"
    @param:Json(name = "entity_id") val entityId: String,
    @param:Json(name = "from_user_id") val fromUserId: String,
    @param:Json(name = "from_name") val fromName: String,
    @param:Json(name = "from_handle") val fromHandle: String?,
    @param:Json(name = "target_user_id") val targetUserId: String?,
    val visibility: String,
    val name: String,
    val subtitle: String?,
    val message: String?,
    @param:Json(name = "payload_snapshot_json") val payloadSnapshotJson: Map<String, Any>,
    @param:Json(name = "original_author_user_id") val originalAuthorUserId: String?,
    @param:Json(name = "original_author_name") val originalAuthorName: String?,
    @param:Json(name = "original_entity_id") val originalEntityId: String?,
    @param:Json(name = "created_at") val createdAt: String,
    @param:Json(name = "updated_at") val updatedAt: String,
    // Joined aggregate fields
    @param:Json(name = "likes_count") val likesCount: List<CountWrapper>? = null,
    @param:Json(name = "saves_count") val savesCount: List<CountWrapper>? = null
)

@JsonClass(generateAdapter = true)
data class CountWrapper(
    val count: Int
)

@JsonClass(generateAdapter = true)
data class RemoteInboxItem(
    val id: String,
    @param:Json(name = "share_id") val shareId: String,
    @param:Json(name = "target_user_id") val targetUserId: String,
    @param:Json(name = "read_at") val readAt: String?,
    @param:Json(name = "created_at") val createdAt: String,
    val share: RemoteShare? = null
)

@JsonClass(generateAdapter = true)
data class RemoteActivityLog(
    val id: String? = null,
    @param:Json(name = "user_id") val userId: String? = null,
    val action: String,
    @param:Json(name = "entity_type") val entityType: String?,
    @param:Json(name = "entity_id") val entityId: String?,
    @param:Json(name = "share_id") val shareId: String?,
    val note: String?,
    @param:Json(name = "created_at") val createdAt: String? = null
)


// RPC HANDLERS payloads

@JsonClass(generateAdapter = true)
data class RpcShareIdPayload(
    @param:Json(name = "input_share_id") val inputShareId: String
)
