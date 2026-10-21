package com.example.data.remote

/** A previously authenticated account can use local tools offline; no guest/demo. */
object AccountAccess {
    fun canEnter(hasStoredSession: Boolean, userId: String?, email: String?): Boolean =
        hasStoredSession && !email.isNullOrBlank() && userId?.let {
            runCatching { java.util.UUID.fromString(it) }.isSuccess
        } == true
}
