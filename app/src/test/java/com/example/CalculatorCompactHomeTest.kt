package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h700dp")
class CalculatorCompactHomeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun calculatorActionsFitFirstViewportAboveBottomNavigation() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { MyApplicationTheme { HomeScreen(model, {}) } }
        compose.onNodeWithText("Calculadora barista").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-compact-home-320.png")
        listOf("Preparar", "Laboratorio", "Guardar proporción en Calculadora").forEach {
            val node = compose.onNodeWithContentDescription(it)
            node.assertIsDisplayed()
            val bottom = node.fetchSemanticsNode().boundsInRoot.bottom
            assertTrue("$it bottom=$bottom must fit above navigation", bottom < 620f)
        }
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-compact-home-320.png")
    }
}
