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
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_in") val expiresIn: Long,
    val user: SupabaseUser
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String,
    val email: String,
    @Json(name = "created_at") val createdAt: String
)


// DATA TABLES MODELS

@JsonClass(generateAdapter = true)
data class RemoteProfile(
    val id: String,
    val email: String,
    @Json(name = "display_name") val displayName: String,
    val handle: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "avatar_color") val avatarColor: String?,
    val role: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class RemoteBean(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    val roaster: String?,
    val name: String,
    val origin: String?,
    val altitude: String?,
    val process: String?,
    @Json(name = "roast_date") val roastDate: String?,
    @Json(name = "first_use_date") val firstUseDate: String?,
    val notes: String?,
    val status: String?,
    @Json(name = "stock_grams") val stockGrams: Float?,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteRecipe(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "owner_user_id") val ownerUserId: String? = null,
    @Json(name = "owner_display_name") val ownerDisplayName: String?,
    val name: String,
    val method: String?,
    @Json(name = "bean_id") val beanId: String?,
    @Json(name = "grinder_id") val grinderId: String?,
    @Json(name = "technique_id") val techniqueId: String?,
    @Json(name = "coffee_grams") val coffeeGrams: Float?,
    @Json(name = "water_ml") val waterMl: Int?,
    val ratio: Float?,
    val temperature: Int?,
    val clicks: String?,
    val notes: String?,
    val visibility: String?,
    @Json(name = "is_shared") val isShared: Boolean?,
    @Json(name = "original_author_user_id") val originalAuthorUserId: String?,
    @Json(name = "original_author_name") val originalAuthorName: String?,
    @Json(name = "original_entity_id") val originalEntityId: String?,
    @Json(name = "imported_from_share_id") val importedFromShareId: String?,
    @Json(name = "copy_mode") val copyMode: String?,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteTechnique(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "owner_user_id") val ownerUserId: String? = null,
    @Json(name = "owner_display_name") val ownerDisplayName: String?,
    val name: String,
    val method: String?,
    @Json(name = "coffee_grams") val coffeeGrams: Float?,
    @Json(name = "water_ml") val waterMl: Int?,
    val ratio: Float?,
    val temperature: Int?,
    @Json(name = "grind_clicks") val grindClicks: String?,
    @Json(name = "grinder_id") val grinderId: String?,
    @Json(name = "bean_id") val beanId: String?,
    val notes: String?,
    val visibility: String?,
    @Json(name = "is_shared") val isShared: Boolean?,
    @Json(name = "original_author_user_id") val originalAuthorUserId: String?,
    @Json(name = "original_author_name") val originalAuthorName: String?,
    @Json(name = "original_entity_id") val originalEntityId: String?,
    @Json(name = "imported_from_share_id") val importedFromShareId: String?,
    @Json(name = "copy_mode") val copyMode: String?,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteTechniqueStep(
    val id: String? = null,
    @Json(name = "technique_id") val techniqueId: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "step_order") val stepOrder: Int,
    val title: String?,
    @Json(name = "duration_sec") val durationSec: Int?,
    @Json(name = "water_add_ml") val waterAddMl: Int?,
    @Json(name = "target_water_ml") val targetWaterMl: Int?,
    val gesture: String?,
    val intensity: String?,
    val note: String?,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteShare(
    val id: String,
    @Json(name = "entity_type") val entityType: String, // "recipe", "technique"
    @Json(name = "entity_id") val entityId: String,
    @Json(name = "from_user_id") val fromUserId: String,
    @Json(name = "from_name") val fromName: String,
    @Json(name = "from_handle") val fromHandle: String?,
    @Json(name = "target_user_id") val targetUserId: String?,
    val visibility: String,
    val name: String,
    val subtitle: String?,
    val message: String?,
    @Json(name = "payload_snapshot_json") val payloadSnapshotJson: Map<String, Any>,
    @Json(name = "original_author_user_id") val originalAuthorUserId: String?,
    @Json(name = "original_author_name") val originalAuthorName: String?,
    @Json(name = "original_entity_id") val originalEntityId: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
    // Joined aggregate fields
    @Json(name = "likes_count") val likesCount: List<CountWrapper>? = null,
    @Json(name = "saves_count") val savesCount: List<CountWrapper>? = null
)

@JsonClass(generateAdapter = true)
data class CountWrapper(
    val count: Int
)

@JsonClass(generateAdapter = true)
data class RemoteInboxItem(
    val id: String,
    @Json(name = "share_id") val shareId: String,
    @Json(name = "target_user_id") val targetUserId: String,
    @Json(name = "read_at") val readAt: String?,
    @Json(name = "created_at") val createdAt: String,
    val share: RemoteShare? = null
)

@JsonClass(generateAdapter = true)
data class RemoteActivityLog(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    val action: String,
    @Json(name = "entity_type") val entityType: String?,
    @Json(name = "entity_id") val entityId: String?,
    @Json(name = "share_id") val shareId: String?,
    val note: String?,
    @Json(name = "created_at") val createdAt: String? = null
)


// RPC HANDLERS payloads

@JsonClass(generateAdapter = true)
data class RpcShareIdPayload(
    @Json(name = "input_share_id") val inputShareId: String
)
