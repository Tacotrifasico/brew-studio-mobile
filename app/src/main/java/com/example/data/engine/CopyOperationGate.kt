package com.example.data.engine

class CopyOperationGate {
    private val activeKeys = mutableSetOf<String>()

    @Synchronized
    fun tryStart(action: String, shareId: String): Boolean = activeKeys.add("$action:$shareId")

    @Synchronized
    fun finish(action: String, shareId: String) {
        activeKeys.remove("$action:$shareId")
    }
}
