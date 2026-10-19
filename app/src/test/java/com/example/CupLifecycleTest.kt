package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.*
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
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
@Config(sdk = [36], qualifiers = "w360dp-h780dp")
class CupLifecycleTest {
    @Test fun quantityLabelsAndGeometryStayStableAcrossDigitChanges() {
        var coffee by mutableIntStateOf(99)
        var water by mutableIntStateOf(999)
        compose.setContent { MyApplicationTheme {
            Row(Modifier.width(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabQuantityControl("Café", coffee, "g", 1..100, Modifier.weight(1f)) { coffee = it }
                LabQuantityControl("Agua", water, "ml", 10..2000, Modifier.weight(1f)) { water = it }
            }
        } }
        compose.onNodeWithText("Café", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Agua", substring = true).assertIsDisplayed()
        val before = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("All quantity controls must fit in one level", before.height <= 36f * compose.density.density)
        val valueCenter = compose.onNodeWithTag("lab_quantity_ml").fetchSemanticsNode().boundsInRoot.center.y
        assertEquals(valueCenter, compose.onNodeWithContentDescription("Más 1 ml de Agua").fetchSemanticsNode().boundsInRoot.center.y, 1f)
        compose.onNodeWithContentDescription("Más 1 ml de Agua").performClick()
        compose.onNodeWithContentDescription("Más 1 g de Café").performClick()
        compose.onNodeWithText("1000 ml", substring = true).assertIsDisplayed()
        compose.onNodeWithText("100 g", substring = true).assertIsDisplayed()
        assertEquals(before, compose.onRoot().fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Más 1 g de Café").assertIsNotEnabled()
        compose.runOnIdle { water = 1999 }
        compose.onNodeWithContentDescription("Más 1 ml de Agua").performClick()
        compose.onNodeWithText("2000 ml", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Más 1 ml de Agua").assertIsNotEnabled()
        assertEquals(before, compose.onRoot().fetchSemanticsNode().boundsInRoot)
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-quantities.png")
        for (unit in listOf("g", "ml")) {
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag("lab_quantity_$unit").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty()); assertFalse("$unit: ${layouts.first().size}, width overflow ${layouts.first().didOverflowWidth}, height overflow ${layouts.first().didOverflowHeight}", layouts.first().hasVisualOverflow)
        }
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-quantities.png")
    }
    @get:Rule val compose = createComposeRule()

    @Test fun equalizerNamesAreNotClippedWithLargerSystemText() {
        val profile = calculateLabProfile(15f, 240, 16f, 92, 24, "en ventana")
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.35f)) {
                MyApplicationTheme { Box(Modifier.width(320.dp)) { SensoryMixerCard(profile) } }
            }
        }
        for (label in listOf("Aroma", "Acidez", "Dulzor", "Cuerpo", "Amargor", "Final")) for (part in listOf("label", "value")) {
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithTag("equalizer_${part}_$label").assertIsDisplayed()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("$label ($part) no debe quedar cortado: ${layouts.single().size}", layouts.single().hasVisualOverflow)
        }
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-equalizer-large-text.png")
    }

    @Test fun lowTemperatureWarningIsVisibleAndDiagnosisAgrees() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { val state by model.state.collectAsState(); MyApplicationTheme {
            Column(Modifier.width(360.dp)) {
                LabVariableDock(LabCategory.Extraccion, state, model, true, {})
                LabHeatAdviceCard(com.example.data.engine.LabTemperatureGuide(state.labPreciseTemp ?: state.labTemp.toDouble(), state.labAltitudeMeters, true, state.labMethod))
            }
        } }
        compose.runOnIdle { model.setPreparationSettings(0, true); model.updateLabVariables(preciseTemperature = (177.0 - 32) / 1.8) }
        compose.onNodeWithText("177 °F").assertIsDisplayed()
        compose.onNodeWithText("Calor bajo para V60").assertIsDisplayed()
        compose.onNodeWithText("Sube el agua a 198–205 °F. Puede faltar extracción.").assertIsDisplayed()
        compose.onNodeWithText("puede ralentizar", substring = true).assertDoesNotExist()
        assertEquals("Calor bajo para V60", model.state.value.labPreviewExtraction)
        assertTrue(model.state.value.labRecommendationText.contains("puede ralentizar"))
        compose.onNodeWithText("Ref. 24").assertIsDisplayed()
        compose.onNodeWithText("← Menos gruesa").assertIsDisplayed()
        compose.onNodeWithText("Más gruesa →").assertIsDisplayed()
        compose.onNodeWithText("Hario · receta V60 ↗").assertDoesNotExist()
        compose.onNodeWithText("Clics de molienda").assertDoesNotExist()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-177f.png")
        compose.runOnIdle { model.updateLabVariables(preciseTemperature = 70.0) }
        compose.onNodeWithText("158 °F").assertIsDisplayed()
        compose.onNodeWithText("Sube el agua a 198–205 °F. Puede faltar extracción.").assertIsDisplayed()
    }

    @Test fun temperatureRecipeMarksAndLegendRemainVisibleAtHighAltitude() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { val state by model.state.collectAsState(); MyApplicationTheme {
            Column(Modifier.width(360.dp)) { LabVariableDock(LabCategory.Extraccion, state, model, true, {}) }
        } }
        compose.runOnIdle { model.setPreparationSettings(2600, true); model.updateLabVariables(preciseTemperature = 90.0) }
        compose.onNodeWithText("Clásico · 198–205 °F").assertIsDisplayed()
        compose.onNodeWithText("4:6 · oscuro 181 · medio 190 · claro 199 °F").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-temperature-high-altitude.png")
    }

    @Test fun equalizerAndBothSlidersFitCompactViewportAndInfoOwnsLongCopy() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { MyApplicationTheme {
            Box(Modifier.width(360.dp).height(640.dp)) { LabScreen(model, {}) }
        } }
        compose.runOnIdle { model.setPreparationSettings(0, true); model.updateLabVariables(preciseTemperature = (195.0 - 32) / 1.8) }
        compose.onNodeWithContentDescription("Calor").performClick()
        // Exercise drag inside the real vertically-scrollable workspace, not just an isolated slider.
        compose.onNodeWithContentDescription("Temperatura del agua").performTouchInput {
            swipe(Offset(width * .15f, center.y), Offset(width * .94f, center.y), 600)
        }
        compose.runOnIdle { assertTrue(model.state.value.labPreciseTemp!! > (205.0 - 32) / 1.8) }
        compose.onNodeWithContentDescription("Temperatura del agua").performTouchInput {
            swipe(Offset(width * .94f, center.y), Offset(width * .15f, center.y), 600)
        }
        compose.runOnIdle {
            assertTrue(model.state.value.labPreciseTemp!! < (180.0 - 32) / 1.8)
            model.updateLabVariables(preciseTemperature = (195.0 - 32) / 1.8)
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        compose.onNodeWithText("Aroma").assertIsDisplayed()
        compose.onNodeWithText("Amargor").assertIsDisplayed()
        compose.onNodeWithTag("equalizer_advice").assertDoesNotExist()
        compose.onNodeWithText("V60 4:6: calor según tueste; ajusta molienda y vertidos.").assertIsDisplayed()
        val s = model.state.value
        val estimated = calculateLabProfile(s.labCoffee, s.labWater, s.labRatio, s.labTemp, s.labClicks, s.labBeanFreshness, preciseTemperatureC = s.labPreciseTemp, useFahrenheit = true)
        compose.onAllNodesWithText(estimated.aroma.toString()).onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Temperatura del agua").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ajuste de molienda").assertIsDisplayed()
        compose.onNodeWithText("Más gruesa →").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-compact.png")
        assertTrue("Molienda ${compose.onNodeWithText("Más gruesa →").fetchSemanticsNode().boundsInRoot} debe quedar por encima de las acciones ${compose.onNodeWithTag("lab_actions").fetchSemanticsNode().boundsInRoot}, sin desplazarse",
            compose.onNodeWithText("Más gruesa →").fetchSemanticsNode().boundsInRoot.bottom <=
                compose.onNodeWithTag("lab_actions").fetchSemanticsNode().boundsInRoot.top)
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-compact.png")
        compose.runOnIdle { model.updateLabVariables(preciseTemperature = 70.0) }
        compose.onNodeWithText("158 °F").assertIsDisplayed()
        compose.onNodeWithText("Sube el agua a 198–205 °F. Puede faltar extracción.").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-158f.png")
        compose.onNodeWithTag("lab_advice_toggle").performClick()
        compose.onNodeWithText("V60 4:6 · Kasuya.", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ejemplo 4:6 ·", substring = true).performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-advice-expanded.png")
        compose.onNodeWithTag("lab_advice_toggle").performScrollTo().performClick()
        compose.onNodeWithText("Laboratorio").performScrollTo()
        compose.runOnIdle { model.updateLabVariables(preciseTemperature = (195.0 - 32) / 1.8) }
        compose.onNodeWithContentDescription("Proporción").performClick()
        compose.onNodeWithContentDescription("Proporción de café y agua").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tiempo de extracción").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-compact-ratio.png")
        assertTrue("Tiempo ${compose.onNodeWithText("Más contacto").fetchSemanticsNode().boundsInRoot} debe quedar por encima de las acciones ${compose.onNodeWithTag("lab_actions").fetchSemanticsNode().boundsInRoot}, sin desplazarse",
            compose.onNodeWithText("Más contacto").fetchSemanticsNode().boundsInRoot.bottom <=
                compose.onNodeWithTag("lab_actions").fetchSemanticsNode().boundsInRoot.top)
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-compact-ratio.png")
        compose.onNodeWithText("Ilustración orientativa", substring = true).assertDoesNotExist()
        println("Lab compact: sliders and advice fit; opening information")
        compose.onNodeWithContentDescription("Acerca del ecualizador").performClick()
        println("Lab compact: information opened")
        compose.onNodeWithText("Cómo leer la estimación").assertIsDisplayed()
        compose.onNodeWithText("no porcentajes", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Listo").performScrollTo().performClick()
        println("Lab compact: information closed")
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
    }

    @Test fun nativeSliderKeepsRespondingAcrossZoneAndRepeatedDrags() {
        var selected by mutableFloatStateOf(190f)
        val changes = mutableListOf<Float>()
        compose.setContent { MyApplicationTheme {
            LabCalibratedSlider(selected, { selected = it; changes += it }, 158f..212f, 198f..205f, activeColor = androidx.compose.ui.graphics.Color(0xFFC26638), accessibilityLabel = "Prueba de calor", accessibilityValue = "$selected °F", temperatureZones = true)
        } }
        val slider = compose.onNodeWithContentDescription("Prueba de calor")
        slider.performTouchInput { swipe(Offset(width * .15f, center.y), Offset(width * .94f, center.y), 600) }
        compose.runOnIdle { assertTrue(selected > 205); assertTrue(changes.size > 5); assertTrue(changes.all { it == it.toInt().toFloat() }) }
        slider.performTouchInput { swipe(Offset(width * .94f, center.y), Offset(width * .15f, center.y), 600) }
        compose.runOnIdle { assertTrue(selected < 180) }
        slider.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(195f) }
        compose.runOnIdle { assertEquals(195f, selected) }
    }

    @Test fun preparationIdentityIsCompactAndClearlyLabeled() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { MyApplicationTheme {
            BrewSetupView(model, com.example.ui.viewmodel.BaristaCalcState(
                activePrepBean = "Ronpotrero · Reserva de la montaña",
                activePrepTechniqueName = "Clásica en 3 vertidos",
                activePrepMethod = "V60", activePrepCoffee = 15f, activePrepWater = 241,
                activePrepRatio = 241f / 15f, activePrepClicks = 24,
                activePrepPreciseTemp = (195.0 - 32) / 1.8, useFahrenheit = true
            ))
        } }
        compose.onNodeWithText("Ronpotrero · Reserva de la montaña").assertIsDisplayed()
        compose.onNodeWithText("TEMPERATURA").assertIsDisplayed()
        compose.onNodeWithText("195 °F").assertIsDisplayed()
        compose.onNodeWithText("24 clics").assertIsDisplayed()
        compose.onNodeWithText("1:16.07").assertIsDisplayed()
        compose.onNodeWithText("Iniciar preparación").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-preparation-design.png")
    }

    @Test fun laboratoryDegreesWaterAndRatioAdvanceByOne() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { val state by model.state.collectAsState(); MyApplicationTheme {
            LabVariableDock(LabCategory.Extraccion, state, model, true, {})
        } }
        compose.runOnIdle { model.setPreparationSettings(0, true); model.updateLabVariables(temperature = 91) }
        compose.onNodeWithText("196 °F").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menos 1 grado").performClick()
        compose.onNodeWithText("195 °F").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menos 1 grado").performClick()
        compose.onNodeWithText("194 °F").assertIsDisplayed()
        compose.onNodeWithContentDescription("Más 1 grado").performClick()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-195f.png")
        compose.runOnIdle {
            model.updateLabVariables(coffee = 15f, ratio = 16f)
            model.updateLabVariables(water = model.state.value.labWater + 1)
            assertEquals(241, model.state.value.labWater)
            assertEquals(241f / 15f, model.state.value.labRatio, 0.00001f)
            assertEquals((195.0 - 32) / 1.8, model.state.value.labPreciseTemp!!, 0.000001)
            assertTrue(model.playLabIdeaAsPrep()); assertTrue(model.state.value.activePrepSteps.isEmpty())
            model.startTimer(); assertFalse(model.state.value.timerRunning)
        }
    }

    @Test fun completionPersistsCupAndCataUpdatesSameCupAndReplicationRecoversSteps() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        var navigateCata = false
        compose.setContent { val state by model.state.collectAsState(); MyApplicationTheme {
            if (state.preparationCompleted) ActiveBrewTimerView(model, state) { navigateCata = true } else Text("Prueba")
        } }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.techniquesList.any { it.name == "Clásica en 3 vertidos" } }
        compose.runOnIdle {
            model.setPreparationSettings(0, true)
            model.onMethodSelected("V60"); model.onActionLab()
            model.updateLabVariables(coffee = 15f, water = 241, preciseTemperature = (195.0 - 32) / 1.8)
            model.playLabIdeaAsPrep()
            model.loadPrepTechnique(model.state.value.techniquesList.first { it.name == "Clásica en 3 vertidos" }.id)
        }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.activePrepSteps.isNotEmpty() }
        compose.runOnIdle { model.startTimer(); model.finishPreparationForTasting() }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.preparationCupSaved && model.state.value.cupsList.any { it.id == model.state.value.activePreparationSessionId } }
        val original = model.state.value.cupsList.first { it.id == model.state.value.activePreparationSessionId }
        assertFalse(navigateCata); assertNull(original.rating)
        assertEquals(241, original.executedWaterMl)
        assertEquals(195.0, original.preciseTemperatureC!! * 1.8 + 32, 0.000001)
        compose.onNodeWithText("Taza guardada en Almacén").assertIsDisplayed()
        compose.onNodeWithText("Guardar taza").assertIsDisplayed()
        compose.onNodeWithText("Ir a Cata y completar perfil").assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-cup-completion.png")
        var saved = false
        compose.runOnIdle { model.saveCup("Chocolate", "Caramelo", 5f, "Dulce") { saved = it } }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); saved && model.state.value.cupsList.any { it.id == original.id && it.comment == "Dulce" } }
        assertEquals(1, model.state.value.cupsList.count { it.id == original.id })
        val cup = model.state.value.cupsList.first { it.id == original.id }
        compose.runOnIdle {
            assertTrue(model.replicateCup(cup)); assertFalse(model.state.value.preparationCompleted)
            assertNotEquals(original.id, model.state.value.activePreparationSessionId)
            assertEquals(241, model.state.value.activePrepWater)
            assertEquals(241, model.state.value.activePrepSteps.sumOf { it.waterAddedMl })
            assertEquals(original.beanId, model.state.value.activePrepBeanId)
        }
    }

    @Test fun additivePrecisionMigrationKeepsExistingCupsAndExperiments() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(app)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(10) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE cups (id TEXT PRIMARY KEY NOT NULL, executedTemperatureC INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE lab_experiments (id TEXT PRIMARY KEY NOT NULL, temperatureC INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO cups VALUES ('existing', 93)")
                        db.execSQL("INSERT INTO lab_experiments VALUES ('existing', 92)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build())
        val db = helper.writableDatabase; MIGRATION_10_11.migrate(db)
        db.query("SELECT executedTemperatureC, preciseTemperatureC FROM cups WHERE id = 'existing'").use { assertTrue(it.moveToFirst()); assertEquals(93, it.getInt(0)); assertTrue(it.isNull(1)) }
        db.query("SELECT temperatureC, preciseTemperatureC FROM lab_experiments WHERE id = 'existing'").use { assertTrue(it.moveToFirst()); assertEquals(92, it.getInt(0)); assertTrue(it.isNull(1)) }
        helper.close()
    }

    @Test fun exactTemperatureSurvivesDomainAndNetworkRoundTrips() {
        val precise = (195.0 - 32) / 1.8
        com.example.data.mappers.EntityMappers.run {
            val cup = Cup(preciseTemperatureC = precise)
            assertEquals(precise, cup.toDomain().toDto().toDomain().toEntity().preciseTemperatureC!!, 0.000001)
            val experiment = LabExperiment(preciseTemperatureC = precise)
            assertEquals(precise, experiment.toDomain().toDto().toDomain().toEntity().preciseTemperatureC!!, 0.000001)
        }
    }
}
