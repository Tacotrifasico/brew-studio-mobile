package com.example

import com.example.data.engine.LabTemperatureGuide
import com.example.ui.screens.calculateLabProfile
import org.junit.Assert.*
import org.junit.Test

class LabTemperatureGuideTest {
    @Test fun coldWaterNeverGetsBalancedSummaryEvenIfMillingCompensatesExtractionIndex() {
        for (clicks in listOf(12, 18, 24, 30)) for (seconds in listOf(120, 180, 240, 300)) {
            val profile = calculateLabProfile(15f, 240, 16f, 81, clicks, "en ventana", timeSeconds = seconds, preciseTemperatureC = (177.0 - 32) / 1.8, useFahrenheit = true)
            assertTrue(profile.summary.contains("Riesgo de subextracción"))
            assertEquals("Agua demasiado fría", profile.labels.first())
            assertFalse(profile.labels.contains("Ventana Óptima"))
        }
    }
    @Test fun zoneHasSameWholeSelectableBoundariesInBothUnits() {
        val c = LabTemperatureGuide(92.0, 0)
        val f = LabTemperatureGuide((195.0 - 32) / 1.8, 0, true)
        assertEquals("90–96 °C", c.rangeText); assertEquals("194–205 °F", f.rangeText)
        for (degree in 194..205) assertFalse(LabTemperatureGuide((degree - 32) / 1.8, 0, true).warning)
        assertTrue(LabTemperatureGuide((193.0 - 32) / 1.8, 0, true).warning)
        assertTrue(LabTemperatureGuide((206.0 - 32) / 1.8, 0, true).warning)
    }
    @Test fun altitudeLimitsRecommendationsAndPrioritizesBoilingWarning() {
        val guide = LabTemperatureGuide(98.0, 2240, true)
        assertEquals("Supera el hervor local", guide.headline)
        assertTrue(guide.upperC <= guide.boilingC)
        assertEquals("187–198 °F", guide.rangeText)
        val high = LabTemperatureGuide(90.0, 5000)
        assertEquals("80–83 °C", high.rangeText)
        assertTrue(high.detail.contains("altura"))
    }
}
