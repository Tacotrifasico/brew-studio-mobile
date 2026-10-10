package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.database.Cup
import com.example.ui.screens.CupItemCard
import com.example.ui.screens.CupDetailSheet
import com.github.takahirom.roborazzi.captureRoboImage
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SavedCupTemperatureTest {
    @get:Rule val compose = createComposeRule()
    @Test fun savedCupOpensReadableHistoricalDetails() {
        val open = mutableStateOf(false)
        val cup = Cup(beanNameSnapshot = "Ronpotrero", techniqueNameSnapshot = "Inmersión dulce", methodNameSnapshot = "Prensa francesa", executedTemperatureC = 93, comment = "Cacao y caramelo")
        compose.setContent { MyApplicationTheme {
            if (open.value) CupDetailSheet(cup, true, { open.value = false }, {})
            else CupItemCard(cup, true, onOpen = { open.value = true }) {}
        } }
        compose.onNodeWithText("Ronpotrero").performClick()
        compose.onNodeWithText("Preparación ejecutada").assertIsDisplayed()
        compose.onNodeWithText("°F", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Prensa francesa").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-cup-details.png")
        compose.onNodeWithText("Cacao y caramelo").performScrollTo().assertIsDisplayed()
    }
    @Test fun savedCupFollowsUnitWithoutMutatingSnapshot() {
        val fahrenheit = mutableStateOf(true)
        val cup = Cup(beanNameSnapshot = "Ronpotrero", executedTemperatureC = 93)
        compose.setContent { MyApplicationTheme { CupItemCard(cup, fahrenheit.value) {} } }
        compose.onNodeWithText("°F", substring = true).assertIsDisplayed()
        compose.onNodeWithText("°C", substring = true).assertDoesNotExist()
        compose.runOnIdle { fahrenheit.value = false }
        compose.onNodeWithText("Temperatura: 93 °C").assertIsDisplayed()
        compose.onNodeWithText("°F", substring = true).assertDoesNotExist()
        assertEquals(93, cup.executedTemperatureC)
    }
}
