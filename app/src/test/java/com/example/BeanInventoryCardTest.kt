package com.example

import androidx.compose.foundation.layout.*
import android.app.Application
import android.os.Looper
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.database.SampleCoffee
import com.example.data.database.AppDatabase
import com.example.ui.screens.BeanItemCard
import com.example.ui.screens.BeanDetailSheet
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w320dp-h700dp")
class BeanInventoryCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactCardKeepsPreparationAndAllActionsVisible() {
        updateThemeColors(false)
        val bean = SampleCoffee.bean().copy(id = "00000000-0000-0000-0000-000000000003", name = "Sierra Azul")
        var detail = 0; var edit = 0; var lab = 0; var brew = 0; var delete = 0
        compose.setContent {
            MyApplicationTheme {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BeanItemCard(bean, { detail++ }, { brew++ }, { lab++ }, { edit++ }, { delete++ })
                    BeanItemCard(SampleCoffee.bean(), {}, {}, {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("Sierra Azul").assertIsDisplayed()
        compose.onAllNodesWithText("1700 m")[0].assertIsDisplayed()
        compose.onNodeWithText(bean.roaster).assertDoesNotExist()
        compose.onNodeWithText(bean.notes).assertDoesNotExist()
        compose.onAllNodesWithText("Cómo lo preparo")[0].performClick()
        compose.onAllNodesWithContentDescription("Editar")[0].performClick()
        compose.onAllNodesWithContentDescription("Usar en Laboratorio")[0].performClick()
        compose.onAllNodesWithText("Preparar")[0].performClick()
        compose.onNodeWithContentDescription("Borrar").performClick()
        compose.runOnIdle { assertEquals(listOf(1, 1, 1, 1, 1), listOf(detail, edit, lab, brew, delete)) }
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/coffee-inventory-compact-320.png")
    }

    @Test fun colorsAreStableAndSupportBothThemes() {
        val id = SampleCoffee.ID
        updateThemeColors(false)
        val light = coffeeAccent(id)
        assertEquals(light, coffeeAccent(id.uppercase()))
        assertNotEquals(light, coffeeAccent("00000000-0000-0000-0000-000000000003"))
        updateThemeColors(true)
        assertNotEquals(light, coffeeAccent(id))
        updateThemeColors(false)
    }

    @Test fun detailShowsSavedPreparationWithoutOpeningEditor() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val bean = SampleCoffee.bean().copy(id = java.util.UUID.randomUUID().toString())
        runBlocking { AppDatabase.getDatabase(app).beanDao().insertBean(bean) }
        val model = BaristaCalcViewModel(app)
        compose.setContent { MyApplicationTheme { BeanDetailSheet(bean, model, {}, {}) } }
        compose.onNodeWithText("Cómo lo preparo").assertIsDisplayed()
        compose.onNodeWithText("22").assertIsDisplayed()
        compose.onNodeWithText("92 °C").assertIsDisplayed()
        compose.onNodeWithText(bean.notes).assertDoesNotExist()
        val roots = compose.onAllNodes(isRoot())
        roots[roots.fetchSemanticsNodes().lastIndex].captureRoboImage(filePath = "/private/tmp/coffee-detail-compact-320.png")
        compose.onNodeWithContentDescription("Aumentar clics de molino").performClick()
        compose.waitUntil(10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            model.state.value.beansList.firstOrNull { it.id == bean.id }?.brewProfilesJSON?.contains("23") == true
        }
        compose.onNodeWithText("23").assertIsDisplayed()
        compose.onNodeWithText("Ficha completa").performClick()
        compose.onNodeWithText(bean.notes).assertExists()
    }
}
