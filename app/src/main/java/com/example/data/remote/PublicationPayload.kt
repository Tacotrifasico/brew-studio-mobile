package com.example.data.remote

import com.example.data.remote.models.RemoteShare

/** Read models contain server-owned counters; never serialize them back into a write. */
internal fun publicationPayload(s: RemoteShare): Map<String, Any> = linkedMapOf<String, Any>(
    "id" to s.id, "entity_type" to s.entityType, "entity_id" to s.entityId,
    "from_user_id" to s.fromUserId, "from_name" to s.fromName, "from_handle" to s.fromHandle.orEmpty(),
    "visibility" to s.visibility.lowercase(), "name" to s.name, "subtitle" to s.subtitle.orEmpty(),
    "message" to s.message.orEmpty(), "payload_snapshot_json" to s.payloadSnapshotJson
).apply {
    s.targetUserId?.let { put("target_user_id", it) }
    s.sourceShareId?.let { put("source_share_id", it) }
    s.originalAuthorUserId?.let { put("original_author_user_id", it) }
    s.originalAuthorName?.let { put("original_author_name", it) }
    s.originalEntityId?.let { put("original_entity_id", it) }
}
