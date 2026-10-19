package com.example.data.remote

import com.example.data.remote.models.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.repository.SyncRepository
import org.junit.Assert.*
import org.junit.Test

class LocalFirstContractTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    @Test fun confirmationResponseDoesNotInventASession() {
        val response = moshi.adapter(AuthResponse::class.java).fromJson("""{"id":"id","email":"test@example.invalid","created_at":"today"}""")!!
        assertNull(response.accessToken); assertNull(response.refreshToken); assertNull(response.user)
        assertEquals("id", response.id)
    }
    @Test fun canonicalProfileDoesNotRequirePrivateEmailOrRoleColumns() {
        val profile = moshi.adapter(RemoteProfile::class.java).fromJson("""{"id":"id","display_name":"Ana","handle":"ana","avatar_url":null,"avatar_color":"#3F7A63","created_at":null,"updated_at":null,"is_private":true}""")!!
        assertNull(profile.email); assertNull(profile.role); assertTrue(profile.isPrivate)
        val body = moshi.adapter(RemoteProfile::class.java).toJson(profile)
        assertFalse(body.contains("\"email\"")); assertFalse(body.contains("\"role\""))
    }
    @Test fun publishingNeverSendsServerCountersOrPrivateInventory() {
        val share = RemoteShare("id","recipe","local-id","author","Ana","ana",null,"PUBLIC","Receta","", "",mapOf("steps" to listOf("one")),null,null,null,"", "",likeCount=42)
        val body = publicationPayload(share)
        assertEquals("public",body["visibility"]); assertEquals("local-id",body["entity_id"])
        assertFalse(body.containsKey("like_count")); assertFalse(body.containsKey("beans")); assertFalse(body.containsKey("created_at"))
    }
}
