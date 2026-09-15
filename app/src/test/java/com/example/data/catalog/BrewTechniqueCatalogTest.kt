package com.example.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrewTechniqueCatalogTest {
    @Test
    fun everyCalculatorMethodHasAtLeastThreeTechniques() {
        assertEquals(7, BrewTechniqueCatalog.methods.size)
        BrewTechniqueCatalog.methods.forEach { method ->
            val techniques = BrewTechniqueCatalog.techniques.filter { it.methodId == method.id }
            assertTrue("${method.nameKey} debe tener por lo menos 3 técnicas", techniques.size >= 3)
        }
    }

    @Test
    fun techniqueAndStepIdsAreUnique() {
        assertEquals(
            BrewTechniqueCatalog.techniques.size,
            BrewTechniqueCatalog.techniques.map { it.id }.distinct().size
        )
        val stepIds = BrewTechniqueCatalog.techniques.flatMap { BrewTechniqueCatalog.steps(it, 347) }.map { it.id }
        assertEquals(stepIds.size, stepIds.distinct().size)
    }

    @Test
    fun stepsAlwaysUseExactlyCalculatorWater() {
        listOf(36, 240, 347, 800).forEach { water ->
            BrewTechniqueCatalog.techniques.forEach { technique ->
                val steps = BrewTechniqueCatalog.steps(technique, water)
                assertEquals(water, steps.sumOf { it.waterAddedMl })
                assertEquals(water, steps.last().waterAccumulatedMl)
            }
        }
    }

    @Test
    fun scalingPreservesTimingAndUsesNewWaterTotal() {
        val technique = BrewTechniqueCatalog.techniques.first()
        val original = BrewTechniqueCatalog.steps(technique, 240)
        val scaled = BrewTechniqueCatalog.scaleSteps(original, 240, 375)

        assertEquals(original.map { it.durationSeconds }, scaled.map { it.durationSeconds })
        assertEquals(375, scaled.sumOf { it.waterAddedMl })
        assertEquals(375, scaled.last().waterAccumulatedMl)
    }

    @Test
    fun everyLabMethodResolvesToItsOwnStorageMethod() {
        BrewTechniqueCatalog.methods.forEach { method ->
            assertEquals(method.id, BrewTechniqueCatalog.methodId(method.nameKey))
            val template = BrewTechniqueCatalog.firstTechniqueFor(method.nameKey)
            assertEquals(method.id, template?.methodId)
        }
    }
}
