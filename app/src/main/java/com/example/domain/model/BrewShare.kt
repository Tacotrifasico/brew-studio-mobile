package com.example.domain.model

data class BrewShare(
    val id: String = java.util.UUID.randomUUID().toString(),
    val entityType: ShareEntityType,
    val entityId: String,
    val name: String,
    val subtitle: String? = null,
    val metadata: List<String> = emptyList(),

    val fromUserId: String,
    val fromDisplayName: String,
    val fromHandle: String? = null,

    val targetUserId: String? = null,
    val visibility: ShareVisibility = ShareVisibility.PUBLIC,

    val message: String? = null,
    val createdAt: Long = System.currentTimeMillis(),

    val likes: Set<String> = emptySet(),
    val saves: Set<String> = emptySet(),

    val attribution: Attribution,
    val payload: SharedPayload? = null
)
