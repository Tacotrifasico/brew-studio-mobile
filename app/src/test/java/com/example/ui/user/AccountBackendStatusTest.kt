package com.example.ui.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountBackendStatusTest {
    @Test
    fun `unconfigured build never claims remote synchronization or active rls`() {
        val status = accountBackendStatus(backendConfigured = false, loggedIn = true)

        assertEquals("Modo local · Supabase no configurado", status.account)
        assertEquals("Guardados en este dispositivo", status.data)
        assertTrue(status.security.contains("pendiente", ignoreCase = true))
    }

    @Test
    fun `configured signed in build remains honest about partial android sync`() {
        val status = accountBackendStatus(backendConfigured = true, loggedIn = true)

        assertEquals("Sesión de Supabase iniciada", status.account)
        assertEquals("Sincronización Android parcial", status.data)
        assertTrue(status.security.contains("A/B"))
    }
}
