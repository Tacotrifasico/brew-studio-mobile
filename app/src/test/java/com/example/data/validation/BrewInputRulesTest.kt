package com.example.data.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test fun normalizesTechniqueTotalsFromValidatedPours() {
        val normalized = BrewInputRules.normalizeTechnique(
            name = "V60 dulce",
            coffee = 15f,
            temperature = 93,
            stepTitles = listOf("Bloom", "Primer vertido", "Vertido final"),
            stepDurations = listOf(30, 45, 55),
            stepWaters = listOf(50, 90, 100)
        )

        assertNotNull(normalized)
        assertEquals(240, normalized?.waterMl)
        assertEquals(16f, normalized?.ratio)
        assertEquals(130, normalized?.totalTimeSeconds)
        assertEquals(listOf(50, 140, 240), normalized?.accumulatedWaterMl)
    }

    @Test fun normalizationRefusesInvalidOrMismatchedStepLists() {
        assertNull(
            BrewInputRules.normalizeTechnique(
                name = "Prueba",
                coffee = 15f,
                temperature = 93,
                stepTitles = listOf("Bloom", "Vertido"),
                stepDurations = listOf(30),
                stepWaters = listOf(50, 190)
            )
        )
    }
}
