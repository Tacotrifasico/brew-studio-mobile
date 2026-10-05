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
