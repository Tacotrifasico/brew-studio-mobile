package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.BaristaCalcCard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BaristaCalcState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp")
class CalculatorFlipTest {
    @get:Rule val compose = createComposeRule()
    @Test fun titleFlipsEntireCardAndReturnsWithoutChangingCalculation() {
        compose.setContent {
            MyApplicationTheme {
                BaristaCalcCard(state = BaristaCalcState(water = 240), presets = emptyList(),
                    onCoffeeChanged = {}, onRatioChanged = {}, onWaterChanged = {},
                    onMethodSelected = {}, onPresetSelected = {}, onAdjustCoffee = {}, onAdjustRatio = {},
                    onAdjustWater = {}, onResetRatio = {}, onCoffeeFocusLost = {}, onRatioFocusLost = {},
                    onWaterFocusLost = {}, onPrepare = {}, onLab = {}, onFavorite = {},
                    backContent = { Text("Grano del Almacén") })
            }
        }
        compose.onNodeWithText("Ver reverso").assertIsDisplayed()
        compose.onNodeWithContentDescription("Calculadora barista. Ver ajustes del grano").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Grano del Almacén").assertIsDisplayed()
        compose.onNodeWithText("Volver al cálculo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Calculadora barista. Volver al cálculo").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Calculadora barista. Ver ajustes del grano").assertIsDisplayed()
        compose.onNodeWithText("Grano del Almacén").assertDoesNotExist()
    }
}
