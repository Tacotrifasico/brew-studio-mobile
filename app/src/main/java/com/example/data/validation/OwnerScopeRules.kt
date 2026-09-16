package com.example.data.validation

object OwnerScopeRules {
    fun isVisible(recordOwnerId: String?, activeOwnerId: String?): Boolean =
        if (activeOwnerId == null) recordOwnerId == null
        else recordOwnerId == null || recordOwnerId == activeOwnerId

    fun canSync(recordOwnerId: String?, activeOwnerId: String): Boolean =
        recordOwnerId == null || recordOwnerId == activeOwnerId
}
