package com.example.domain.model

enum class AttributionMode {
    IMPORT,
    FORK
}

data class Attribution(
    val required: Boolean = true,
    val mode: AttributionMode? = null,
    val originalAuthorUserId: String,
    val originalAuthorName: String,
    val originalEntityId: String,
    val importedFromShareId: String? = null
)

enum class SocialCopyMode {
    IMPORT,
    FORK
}

data class SocialSource(
    val shareId: String,
    val copiedAt: Long = System.currentTimeMillis(),
    val copyMode: SocialCopyMode,
    val fromUserId: String,
    val fromDisplayName: String
)
