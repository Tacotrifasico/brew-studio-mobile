package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.calculateLabProfile
import com.example.ui.viewmodel.BaristaCalcViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PreparationSettingsTest {
    @Test fun switchingMethodsKeepsGrinderReferenceAndUsesHoursForColdBrew() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        model.updateLabVariables(clicks = 27, temperature = 92, estTimeSeconds = 180)
        model.selectMethodForLab(null, "Cold Brew")
        assertEquals(20, model.state.value.labTemp)
        assertEquals(43200, model.state.value.labEstTimeSeconds)
        assertEquals(27, model.state.value.labClicks)
        model.selectMethodForLab(null, "Espresso")
        assertEquals(92, model.state.value.labTemp)
        assertEquals(25, model.state.value.labEstTimeSeconds)
        assertEquals(27, model.state.value.labClicks)
    }
    @Test fun heatMovesFlavorUntilAltitudeBoilingLimit() {
        val cool = calculateLabProfile(15f, 240, 16f, 80, 24, "en ventana", 0)
        val hot = calculateLabProfile(15f, 240, 16f, 98, 24, "en ventana", 0)
        assertNotEquals(cool.aroma, hot.aroma)
        assertNotEquals(cool.acidity, hot.acidity)
        assertNotEquals(cool.sweetness, hot.sweetness)
        assertNotEquals(cool.bitterness, hot.bitterness)
        val capped = calculateLabProfile(15f, 240, 16f, 94, 24, "en ventana", 2240)
        val above = calculateLabProfile(15f, 240, 16f, 98, 24, "en ventana", 2240)
        assertEquals(capped.extractionIndex, above.extractionIndex)
        assertEquals(capped.aroma, above.aroma)
    }
    @Test fun settingsPersistAndSurviveLabResetWithoutConvertingActualTemperature() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val model = BaristaCalcViewModel(app)
        val temperature = model.state.value.labTemp
        model.setPreparationSettings(2240, true)
        model.resetLabVariables()
        assertEquals(2240, model.state.value.labAltitudeMeters)
        assertTrue(model.state.value.useFahrenheit)
        assertEquals(temperature, model.state.value.labTemp)
        val restored = BaristaCalcViewModel(app)
        assertEquals(2240, restored.state.value.labAltitudeMeters)
        assertTrue(restored.state.value.useFahrenheit)
        val sea = calculateLabProfile(15f, 240, 16f, 98, 24, "en ventana", 0)
        val high = calculateLabProfile(15f, 240, 16f, 98, 24, "en ventana", 2240)
        assertNotEquals(sea.extractionIndex, high.extractionIndex)
        assertTrue(high.summary.contains("hierve"))
        model.setPreparationSettings(6000, false)
        assertEquals(5000, model.state.value.labAltitudeMeters)
    }
}
