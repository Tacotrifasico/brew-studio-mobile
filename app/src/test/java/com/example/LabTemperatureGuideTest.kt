package com.example

import com.example.data.engine.LabTemperatureGuide
import com.example.ui.screens.calculateLabProfile
import org.junit.Assert.*
import org.junit.Test

class LabTemperatureGuideTest {
    @Test fun recipeMarksStayVisibleAboveLocalBoilingAndUnknownMethodsHaveNone() {
        val high = LabTemperatureGuide(90.0, 2600, true)
        assertFalse(high.hasReachableBand)
        assertEquals(198f..205f, high.recipeBand)
        assertEquals(listOf(181f, 190f, 199f), high.recipePoints)
        assertEquals("Clásico · 198–205 °F", high.recipeBandText)
        assertTrue(high.recipePointsText!!.contains("oscuro 181"))
        assertEquals(92f..96f, LabTemperatureGuide(90.0, 2600).recipeBand)
        assertEquals(listOf(83f, 88f, 93f), LabTemperatureGuide(90.0, 2600).recipePoints)
        val custom = LabTemperatureGuide(90.0, 2600, method = "Mi método")
        assertNull(custom.recipeBand); assertTrue(custom.recipePoints.isEmpty()); assertNull(custom.recipePointsText)
        assertNull(LabTemperatureGuide(85.0, 0, method = "AeroPress").recipeBand)
    }
    @Test fun lowHeatAlternativesNameTheirTechniqueInsteadOfInventingUniversalZones() {
        val dark = LabTemperatureGuide(83.0, 0, true)
        assertEquals("V60 · calor para otra técnica", dark.headline)
        assertTrue(dark.compactDetail.contains("4:6"))
        assertEquals(dark.headline, LabTemperatureGuide((181.0 - 32) / 1.8, 0, true).headline)
        assertFalse(LabTemperatureGuide((181.0 - 32) / 1.8, 0, true).recipeAdvice.first().contains("está por debajo"))
        assertTrue(dark.recipeAdvice.any { it.contains("Oscuro: ≈ 181 °F") && it.contains("medio: ≈ 190 °F") })
        assertTrue(dark.recipeAdvice.any { it.contains("media-gruesa") && it.contains("1:15") })
        assertTrue(LabTemperatureGuide(70.0, 0, true).recipeAdvice.first().contains("158 °F está por debajo"))
        assertFalse(LabTemperatureGuide(83.0, 0, true, "Mi método").recipeAdvice.any { it.contains("Kasuya") })
    }
    @Test fun lowTemperatureAdviceSaysWhatToDoRatherThanPromisingGoodCoffee() {
        val v60 = LabTemperatureGuide(70.0, 0, true)
        assertTrue(v60.warning)
        assertEquals("Calor bajo para V60", v60.headline)
        assertEquals("Sube el agua a 198–205 °F. Puede faltar extracción.", v60.compactDetail)
        assertTrue(LabTemperatureGuide(70.0, 0, true, "AeroPress").compactDetail.startsWith("Agua por debajo"))
        assertTrue(LabTemperatureGuide(93.0, 0, true).compactDetail.startsWith("Estás en"))
    }
    @Test fun adviceNeverPromisesBalanceOrUsesUniversalGrinderNumbers() {
        for (clicks in listOf(12, 18, 24, 30)) for (seconds in listOf(120, 180, 240, 300)) {
            val profile = calculateLabProfile(15f, 240, 16f, 81, clicks, "en ventana", timeSeconds = seconds, preciseTemperatureC = (177.0 - 32) / 1.8, useFahrenheit = true)
            assertTrue(profile.summary.contains("puede ralentizar"))
            assertEquals("Calor bajo para V60", profile.labels.first())
            assertFalse(profile.summary.contains("equilibrada"))
            assertFalse(profile.labels.contains("Ventana Óptima"))
        }
    }
    @Test fun documentedV60ReferenceIsIdenticalInWholeDisplayDegrees() {
        assertEquals("92–96 °C", LabTemperatureGuide(92.0, 0).rangeText)
        assertEquals("198–205 °F", LabTemperatureGuide(92.0, 0, true).rangeText)
        for (degree in 198..205) assertFalse(LabTemperatureGuide((degree - 32) / 1.8, 0, true).warning)
        assertTrue(LabTemperatureGuide((197.0 - 32) / 1.8, 0, true).warning)
        assertTrue(LabTemperatureGuide((206.0 - 32) / 1.8, 0, true).warning)
    }
    @Test fun altitudeClipsWaterBandWithoutInventingAnotherReferenceOrCappingPressure() {
        val high = LabTemperatureGuide(98.0, 2240, true)
        assertEquals("Supera el hervor local", high.headline)
        assertTrue(high.recommendedRange.endInclusive <= high.degrees(high.boilingC))
        assertEquals("198–205 °F", high.rangeText) // Original reference remains truthful.
        assertFalse(LabTemperatureGuide(90.0, 5000).hasReachableBand)
        assertEquals("Hervor aprox. · 91 °C", LabTemperatureGuide(90.0, 2600).workingRangeText)
        assertEquals("Hervor aprox. · 196 °F", LabTemperatureGuide(90.0, 2600, true).workingRangeText)
        assertEquals("Según técnica", LabTemperatureGuide(90.0, 2600, method = "Mi método").workingRangeText)
        for (method in listOf("Espresso", "Moka")) {
            val pressure = LabTemperatureGuide(94.0, 5000, method = method)
            assertFalse(pressure.openHotWater)
            assertFalse(pressure.headline.contains("hervor"))
        }
    }
    @Test fun aeropress177FIsNotMisdiagnosedAsAColdV60() {
        val aero = LabTemperatureGuide((177.0 - 32) / 1.8, 0, true, "AeroPress")
        assertFalse(aero.warning)
        assertTrue(aero.detail.contains("Oscuro: 176 °F"))
        assertTrue(aero.detail.contains("185 °F"))
        assertTrue(LabTemperatureGuide((177.0 - 32) / 1.8, 0, true, "V60").warning)
    }
    @Test fun everyMethodHasItsOwnSourceAndUnknownMethodsDoNotInheritV60() {
        for (method in listOf("V60", "Chemex", "Prensa Francesa", "AeroPress", "Espresso", "Moka", "Cold Brew")) {
            val guide = LabTemperatureGuide(92.0, 0, method = method)
            assertTrue(guide.sourceURL.startsWith("https://"))
        }
        assertEquals((200.0 - 32) / 1.8, LabTemperatureGuide(92.0, 0, method = "Chemex").lowerC, 0.001)
        val custom = LabTemperatureGuide(20.0, 0, method = "Mi método")
        assertFalse(custom.hasReference); assertTrue(custom.sourceURL.isEmpty())
        assertEquals(0, calculateLabProfile(15f, 240, 16f, 20, 24, "en ventana", method = "Mi método").aroma)
        assertNull(com.example.data.validation.BrewInputRules.experimentError("Cold Brew", 15f, 240, 20))
    }
}
