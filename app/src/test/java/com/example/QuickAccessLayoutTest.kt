package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h700dp")
class QuickAccessLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun allSixActionLabelsFitCompactRowsOnNarrowPhone() {
        updateThemeColors(false)
        val tapped = mutableSetOf<String>()
        compose.setContent { MyApplicationTheme {
            Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceCard)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accesos rápidos", color = TextPrincipal)
                    QuickAccessAction.entries.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { action -> QuickAccessPill(action.title, Icons.Default.Add, AcentoPrincipal,
                                { tapped.add(action.id) }, Modifier.weight(1f)) }
                        }
                    }
                }
            }
        } }
        QuickAccessAction.entries.forEach { action ->
            val node = compose.onNodeWithText(action.title)
            node.assertIsDisplayed().performClick()
            assertTrue(node.fetchSemanticsNode().boundsInRoot.bottom < 400f)
        }
        compose.runOnIdle { assertEquals(6, tapped.size) }
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-quick-access-320.png")
    }
}
