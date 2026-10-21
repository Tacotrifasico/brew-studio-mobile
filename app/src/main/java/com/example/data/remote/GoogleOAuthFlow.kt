package com.example.data.remote

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** PKCE is device-bound. Never accept access_token/refresh_token from a deep link. */
object GoogleOAuthFlow {
    fun verifier(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
        ByteArray(32).also { SecureRandom().nextBytes(it) })
    fun challenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
    fun authorizationURL(base: String, redirect: String, verifier: String): String {
        require(URI(base).scheme == "https")
        val query = linkedMapOf("provider" to "google", "redirect_to" to redirect,
            "code_challenge" to challenge(verifier), "code_challenge_method" to "s256",
            "scopes" to "openid email profile", "prompt" to "select_account")
        return base.trimEnd('/') + "/auth/v1/authorize?" + query.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
    }
    fun matches(callback: String, expected: String): Boolean = runCatching {
        val actual = URI(callback); val target = URI(expected)
        actual.scheme == target.scheme && actual.rawAuthority == target.rawAuthority &&
            actual.rawPath.trimEnd('/') == target.rawPath.trimEnd('/') && actual.rawUserInfo == null && actual.port == -1
    }.getOrDefault(false)
    fun code(callback: String, expected: String): String {
        require(matches(callback, expected)) { "El enlace de regreso no corresponde a esta app. (G02)" }
        val uri = URI(callback)
        val fields = uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.map {
            val parts = it.split('=', limit = 2)
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }
        require(fields.map { it.first }.distinct().size == fields.size) { "El enlace de regreso está duplicado. Inicia de nuevo. (G04)" }
        if (fields.any { it.first in setOf("error", "error_code") }) {
            val message = when (fields.firstOrNull { it.first == "error_code" }?.second) {
                "flow_state_expired", "flow_state_not_found", "bad_oauth_state" -> "Este intento de Google terminó. Toca Continuar con Google otra vez. (G05)"
                "provider_disabled", "oauth_provider_not_supported" -> "El acceso con Google no está disponible. (G06)"
                else -> "Google no pudo completar el acceso. Puedes intentarlo de nuevo. (G07)"
            }
            throw IllegalArgumentException(message)
        }
        require(uri.fragment.orEmpty() in setOf("", "_", "sb", "sb=")) { "El enlace de regreso no es válido. Inicia de nuevo. (G03)" }
        require(fields.none { it.first in setOf("access_token", "refresh_token") }) { "El enlace de regreso no es válido. Inicia de nuevo. (G03)" }
        return requireNotNull(fields.singleOrNull { it.first == "code" }?.second?.takeIf { it.isNotBlank() && it.length <= 4096 }) {
            "No llegó el código de acceso. Toca Continuar con Google otra vez. (G08)"
        }
    }
}
