package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.BrewOrganicBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrincipal
import com.example.ui.theme.updateThemeColors
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
@Config(sdk = [36], qualifiers = "w360dp-h780dp")
class OrganicBackgroundTest {
    @get:Rule val compose = createComposeRule()
    @Test fun organicLightAndDarkCanvasesDoNotInterceptButtons() {
        updateThemeColors(false)
        var clicked = false
        compose.setContent {
            MyApplicationTheme {
                Box(Modifier.fillMaxSize()) {
                    BrewOrganicBackground()
                    Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                        Text("Brew Studio", color = TextPrincipal)
                        Button(onClick = { clicked = true }) { Text("Guardar") }
                    }
                }
            }
        }
        compose.onNodeWithText("Guardar").performClick()
        compose.runOnIdle { assertTrue(clicked) }
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-organic-light.png")
        compose.runOnIdle { updateThemeColors(true) }
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-organic-dark.png")
        compose.runOnIdle { updateThemeColors(false) }
    }
}
