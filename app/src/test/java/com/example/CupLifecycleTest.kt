package com.example

import android.app.Application
import android.os.Looper
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
    @get:Rule val compose = createComposeRule()

    @Test fun lowTemperatureWarningIsVisibleAndDiagnosisAgrees() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { val state by model.state.collectAsState(); MyApplicationTheme {
            LabVariableDock(LabCategory.Extraccion, state, model, true, {})
        } }
        compose.runOnIdle { model.setPreparationSettings(0, true); model.updateLabVariables(preciseTemperature = (177.0 - 32) / 1.8) }
        compose.onNodeWithText("177 °F").assertIsDisplayed()
        compose.onNodeWithText("Agua demasiado fría").assertIsDisplayed()
        compose.onNodeWithText("Riesgo de subextracción", substring = true).assertIsDisplayed()
        assertEquals("Agua demasiado fría", model.state.value.labPreviewExtraction)
        assertTrue(model.state.value.labRecommendationText.contains("Riesgo de subextracción"))
        compose.onRoot().captureRoboImage(filePath = "/private/tmp/brew-lab-177f.png")
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
