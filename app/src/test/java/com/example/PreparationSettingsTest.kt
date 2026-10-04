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
