package com.example.data.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalFirstSessionTest {
    @Test fun tokensNeverFallBackToPlaintextPreferences() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val session = SessionManager(context)
        session.clearSession()
        session.saveSession("access-private", "11111111-1111-4111-8111-111111111111", "test@example.invalid", "Ana", "ana", null, "refresh-private", 123456)
        val values = context.getSharedPreferences("brew_studio_prefs", Context.MODE_PRIVATE).all.values.joinToString()
        assertFalse(values.contains("access-private")); assertFalse(values.contains("refresh-private"))
        assertEquals("access-private",session.getAccessToken()); assertEquals("refresh-private",session.getRefreshToken())
        assertEquals(123456L,session.getExpiresAt())
        session.clearSession(); assertNull(session.getAccessToken()); assertNull(session.getRefreshToken())
    }
}
