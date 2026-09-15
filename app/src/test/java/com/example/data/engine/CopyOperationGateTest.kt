package com.example.data.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyOperationGateTest {
    @Test fun blocksOnlyTheSameConcurrentCopyOperation() {
        val gate = CopyOperationGate()
        assertTrue(gate.tryStart("IMPORT", "share-1"))
        assertFalse(gate.tryStart("IMPORT", "share-1"))
        assertTrue(gate.tryStart("FORK", "share-1"))
        assertTrue(gate.tryStart("IMPORT", "share-2"))

        gate.finish("IMPORT", "share-1")
        assertTrue(gate.tryStart("IMPORT", "share-1"))
    }
}
