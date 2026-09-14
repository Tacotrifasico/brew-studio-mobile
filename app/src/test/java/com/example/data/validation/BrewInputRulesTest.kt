package com.example.data.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrewInputRulesTest {
    @Test fun validTechniqueRequiresConsistentSafeValues() {
        assertNull(BrewInputRules.techniqueError("V60 dulce", 15f, 93, listOf("Bloom", "Vertido"), listOf(30, 90), listOf(50, 190)))
    }

    @Test fun rejectsWaterOutsideLimitAndIncompleteSteps() {
        assertEquals(
            "Todos los pasos necesitan un título.",
            BrewInputRules.techniqueError("Prueba", 15f, 93, listOf("Bloom", ""), listOf(30, 90), listOf(50, 190))
        )
        assertEquals(
            "La suma de los vertidos debe estar entre 10 y 2000 ml.",
            BrewInputRules.techniqueError("Prueba", 15f, 93, listOf("Único"), listOf(30), listOf(2500))
        )
    }
}
