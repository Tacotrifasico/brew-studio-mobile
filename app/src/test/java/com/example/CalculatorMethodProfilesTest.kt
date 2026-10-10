package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.*
import com.example.ui.components.BaristaCalcCard
import com.example.ui.components.CalculatorBeanBack
import com.example.ui.components.BeanSettingsControls
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BaristaCalcViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp")
class CalculatorMethodProfilesTest {
    @get:Rule val compose = createComposeRule()
    @Test fun customMethodStartsEmptyAndImportedTechniqueSuppliesItsSteps() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { MyApplicationTheme { androidx.compose.material3.Text("Prueba") } }
        val name = "Método improvisado ${java.util.UUID.randomUUID()}"
        compose.runOnIdle { model.addCustomMethod(name) }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.allBrewMethods.any { it.nameKey == name } }
        compose.runOnIdle { model.onMethodSelected(name); model.onActionPrepare() }
        compose.runOnIdle {
            val method = model.state.value.allBrewMethods.first { it.nameKey == name }
            assertTrue(model.state.value.techniquesList.none { it.methodId == method.id })
            assertTrue(model.state.value.activePrepSteps.isEmpty())
            model.startTimer(); assertFalse(model.state.value.timerRunning)
        }
        val technique = Technique(name = "Mi secuencia", methodId = "", doseG = 15f, waterMl = 240)
        val step = TechniqueStep(techniqueId = technique.id, stepNumber = 1, title = "Inmersión", durationSeconds = 180, waterAddedMl = 240, waterAccumulatedMl = 240, gesture = "WAIT")
        val draft = com.example.data.engine.TechniqueFiles.decode(com.example.data.engine.TechniqueFiles.encode(technique, name, listOf(step)))
        var imported = false
        compose.runOnIdle { model.importTechniqueFile(draft) { imported = it } }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); imported && model.state.value.techniquesList.any { it.name == "Mi secuencia" } }
        val saved = model.state.value.techniquesList.first { it.name == "Mi secuencia" }
        compose.runOnIdle { model.loadPrepTechnique(saved.id) }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.activePrepTechniqueId == saved.id }
        compose.runOnIdle { assertEquals("WAIT", model.state.value.activePrepSteps.single().gesture) }
    }
    @Test fun fahrenheitTemperatureButtonsDoNotSkip195() {
        var profile by mutableStateOf(BeanBrewProfile("V60", 22, 91))
        compose.setContent {
            MyApplicationTheme { BeanSettingsControls(profile, true, false, onClicks = {},
                onTemperature = { profile = profile.copy(temperatureC = it) }) }
        }
        compose.onNodeWithText("196 °F").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reducir temperatura").performClick()
        compose.onNodeWithText("195 °F").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reducir temperatura").performClick()
        compose.onNodeWithText("194 °F").assertIsDisplayed()
        compose.onNodeWithContentDescription("Aumentar temperatura").performClick()
        compose.onNodeWithText("195 °F").assertIsDisplayed()
    }
    @Test fun calculatorPreviewAndBackRecallAndSaveOnlyTheSelectedBeanMethod() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val bean = SampleCoffee.bean().copy(id = java.util.UUID.randomUUID().toString(), name = "Perfil por método", brewProfilesJSON = "{}")
        runBlocking { AppDatabase.getDatabase(app).beanDao().insertBean(bean) }
        val model = BaristaCalcViewModel(app)
        compose.setContent {
            val state by model.state.collectAsState()
            MyApplicationTheme { BaristaCalcCard(state, emptyList(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                backContent = { CalculatorBeanBack(model) }) }
        }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.beansList.any { it.id == bean.id } }
        compose.runOnIdle { model.selectCalculatorBean(bean.id); model.onMethodSelected("V60") }
        compose.onNodeWithText("22 clics · 92 °C", substring = true).assertIsDisplayed()
        compose.runOnIdle { model.onMethodSelected("AeroPress") }
        compose.onNodeWithText("18 clics · 88 °C", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Calculadora barista. Ver ajustes del grano").performClick()
        compose.onNodeWithText("18").assertIsDisplayed()
        compose.onNodeWithText("88 °C").assertIsDisplayed()
        compose.onNodeWithText("Guardar favorito").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            BeanBrewProfiles.read(model.state.value.beansList.first { it.id == bean.id }.brewProfilesJSON, "AeroPress") != null
        }
        compose.onNodeWithContentDescription("Aumentar clics de molino").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            BeanBrewProfiles.read(model.state.value.beansList.first { it.id == bean.id }.brewProfilesJSON, "AeroPress")?.clicks == 19
        }
        compose.runOnIdle { model.onMethodSelected("Chemex") }
        compose.onNodeWithText("26").assertIsDisplayed()
        compose.runOnIdle { model.onMethodSelected("AeroPress") }
        compose.onNodeWithText("19").assertIsDisplayed()
        compose.onNodeWithText("88 °C").assertIsDisplayed()
        compose.runOnIdle {
            val json = model.state.value.beansList.first { it.id == bean.id }.brewProfilesJSON
            assertNull(BeanBrewProfiles.read(json, "V60")); assertNull(BeanBrewProfiles.read(json, "Chemex"))
        }
    }
}
