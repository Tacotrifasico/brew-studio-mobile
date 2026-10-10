package com.example

import com.example.data.engine.LabTemperatureGuide
import com.example.ui.screens.calculateLabProfile
import org.junit.Assert.*
import org.junit.Test

class LabTemperatureGuideTest {
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
