package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialPublicationPolicyTest {
    private val entityId = "11111111-1111-4111-8111-111111111111"
    private val recipientId = "22222222-2222-4222-8222-222222222222"

    @Test
    fun `local formula without remote identity is not published`() {
        val missing = SocialPublicationPolicy.validate(null, "public", null)
        val placeholder = SocialPublicationPolicy.validate("00000000-0000-0000-0000-000000000000", "public", null)

        assertTrue(missing.isFailure)
        assertTrue(placeholder.isFailure)
        assertTrue(missing.exceptionOrNull()?.message?.contains("sigue segura") == true)
    }

    @Test
    fun `public publication discards an accidental recipient`() {
        val target = SocialPublicationPolicy.validate(entityId, "public", recipientId).getOrThrow()

        assertEquals("PUBLIC", target.visibility)
        assertEquals(entityId, target.entityId)
        assertNull(target.targetUserId)
    }

    @Test
    fun `direct publication requires a real recipient uuid`() {
        assertTrue(SocialPublicationPolicy.validate(entityId, "direct", "").isFailure)
        assertTrue(SocialPublicationPolicy.validate(entityId, "direct", "@ana").isFailure)
        assertTrue(SocialPublicationPolicy.validate(entityId, "direct", "00000000-0000-0000-0000-000000000000").isFailure)

        val target = SocialPublicationPolicy.validate(entityId, "direct", recipientId).getOrThrow()
        assertEquals("DIRECT", target.visibility)
        assertEquals(recipientId, target.targetUserId)
    }
}
