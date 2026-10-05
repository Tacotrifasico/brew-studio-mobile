package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.Recipe
import com.example.ui.screens.QuickAccessActionHost
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
class QuickAccessFormsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun everyCreationShortcutOpensItsActualFormAndCanClose() {
        val model = BaristaCalcViewModel(ApplicationProvider.getApplicationContext<Application>())
        var action by mutableStateOf<String?>(null)
        compose.setContent { MyApplicationTheme {
            QuickAccessActionHost(action, model, { action = null }, { action = it })
        } }
        listOf("add_coffee" to "Registrar Nuevo Grano", "add_equipment" to "Registrar Equipo de Extracción",
            "add_recipe" to "Nueva Receta", "add_grinder" to "Registrar Molino del Taller",
            "add_technique" to "Nueva técnica").forEach { (id, title) ->
            compose.runOnIdle { action = id }
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.runOnIdle { action = null }
            compose.onNodeWithText(title).assertDoesNotExist()
        }
    }
    @Test fun recipeChoiceHandsOffOnlySelectedRecipeToNativeSharing() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val recipe = Recipe(name = "Receta para compartir", ingredientsSummary = "15 g café", stepsSummary = "Agregar 240 ml")
        runBlocking { AppDatabase.getDatabase(app).recipeDao().insertRecipe(recipe) }
        val model = BaristaCalcViewModel(app)
        var sent: String? = null
        var closed = false
        compose.setContent { MyApplicationTheme {
            QuickAccessActionHost("share_recipe", model, { closed = true }, {}, { sent = it })
        } }
        compose.waitUntil(10_000) { shadowOf(Looper.getMainLooper()).idle(); model.state.value.recipesList.any { it.id == recipe.id } }
        compose.onNodeWithText(recipe.name).performClick()
        compose.runOnIdle { assertTrue(closed); assertTrue(sent!!.contains("15 g café")); assertTrue(sent!!.contains("Agregar 240 ml")) }
    }
}
