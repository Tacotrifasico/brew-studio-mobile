package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.SampleCoffee
import com.example.ui.components.BaristaCalcCard
import com.example.ui.components.CalculatorBeanBack
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MainBackground
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h700dp")
class CalculatorBackLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowBackKeepsMetricsAlignedAndLongBeanNameBounded() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val bean = SampleCoffee.bean().copy(id = java.util.UUID.randomUUID().toString(),
            name = "Ronpotrero — microlote Bourbon de la finca El Potrero, Chiapas")
        runBlocking { AppDatabase.getDatabase(app).beanDao().insertBean(bean) }
        val model = BaristaCalcViewModel(app)
        compose.setContent {
            val state by model.state.collectAsState()
            MyApplicationTheme {
                Box(Modifier.fillMaxWidth().background(MainBackground).padding(16.dp)) {
                    BaristaCalcCard(state, emptyList(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                        backContent = { CalculatorBeanBack(model) })
                }
            }
        }
        compose.waitUntil(10_000) { model.state.value.beansList.any { it.id == bean.id } }
        compose.runOnIdle { model.selectCalculatorBean(bean.id) }
        compose.onNodeWithText("Ver reverso", substring = true).assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/barc-android-front-320.png")
        compose.onNodeWithContentDescription("Calculadora barista. Ver ajustes del grano").performClick()
        compose.onNodeWithText(bean.name).assertIsDisplayed()
        compose.onNodeWithText("22").assertIsDisplayed()
        compose.onNodeWithText("92 °C").assertIsDisplayed()
        val clicks = compose.onNodeWithText("22").fetchSemanticsNode().boundsInRoot
        val temperature = compose.onNodeWithText("92 °C").fetchSemanticsNode().boundsInRoot
        assertEquals(clicks.top, temperature.top, 1f)
        assertTrue(clicks.right < temperature.left)
        val minus = compose.onNodeWithContentDescription("Reducir clics de molino").fetchSemanticsNode().boundsInRoot
        val plus = compose.onNodeWithContentDescription("Aumentar clics de molino").fetchSemanticsNode().boundsInRoot
        assertTrue(minus.right <= plus.left)
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/barc-android-back-320.png")
        compose.onNodeWithContentDescription("Aumentar clics de molino").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.state.value.beansList.first { it.id == bean.id }.brewProfilesJSON.contains("23")
        }
        compose.onNodeWithText("23").assertIsDisplayed()
    }
}
