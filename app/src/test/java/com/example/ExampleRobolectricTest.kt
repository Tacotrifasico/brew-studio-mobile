package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {
  @Test
  fun `cancelling preparation returns to setup without creating a completed brew`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    viewModel.onMethodSelected("V60")
    viewModel.startTimer()
    viewModel.advanceStep()
    val steps = viewModel.state.value.activePrepSteps
    viewModel.cancelPreparation()
    val cancelled = viewModel.state.value
    assertEquals(false, cancelled.timerRunning)
    assertEquals(false, cancelled.preparationCompleted)
    assertEquals(0, cancelled.elapsedSeconds)
    assertEquals(0, cancelled.activeStepIndex)
    assertEquals(steps, cancelled.activePrepSteps)
    viewModel.startTimer()
    assertTrue(viewModel.state.value.timerRunning)
    viewModel.cancelPreparation()
  }

  @Test
  fun `pour choice preserves quantities and tasting handoff preserves completed preparation`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    viewModel.onMethodSelected("V60")
    val before = viewModel.state.value.activePrepSteps
    viewModel.setPreparationPour("CENTER_POUR")
    val chosen = viewModel.state.value.activePrepSteps
    assertEquals(before.map { it.waterAddedMl }, chosen.map { it.waterAddedMl })
    assertEquals(before.map { it.durationSeconds }, chosen.map { it.durationSeconds })
    assertTrue(chosen.filter { it.waterAddedMl > 0 && it.stepNumber > 1 }.all { it.gesture == "CENTER_POUR" })
    viewModel.startTimer()
    val sessionId = viewModel.state.value.activePreparationSessionId
    viewModel.finishPreparationForTasting()
    assertTrue(viewModel.state.value.preparationCompleted)
    assertEquals(sessionId, viewModel.state.value.activePreparationSessionId)
    assertEquals(chosen, viewModel.state.value.activePrepSteps)
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Cupa", appName)
  }

  @Test
  fun `calculator quantities feed preparation without replacing a running brew`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)

    viewModel.onCoffeeChanged("21")
    viewModel.onRatioChanged("15")
    val ready = viewModel.state.value
    assertEquals(21f, ready.activePrepCoffee, 0.001f)
    assertEquals(315, ready.activePrepWater)
    assertEquals(15f, ready.activePrepRatio, 0.001f)
    assertEquals(315, ready.activePrepSteps.last().waterAccumulatedMl)

    viewModel.startTimer()
    viewModel.onCoffeeChanged("18")
    assertEquals(21f, viewModel.state.value.activePrepCoffee, 0.001f)
    viewModel.stopTimer()
  }

  @Test
  fun `finishing the last guided step keeps the cata handoff visible`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    viewModel.onMethodSelected("V60")
    viewModel.startTimer()

    val stepCount = viewModel.state.value.activePrepSteps.size
    repeat(stepCount) { viewModel.advanceStep() }

    val completed = viewModel.state.value
    assertTrue(stepCount > 0)
    assertEquals(false, completed.timerRunning)
    assertEquals(true, completed.preparationCompleted)
    assertEquals(stepCount - 1, completed.activeStepIndex)
    assertEquals(completed.activePrepWater, completed.activePrepSteps.last().waterAccumulatedMl)
    viewModel.stopTimer()
  }

  @Test
  fun `each preparation owns one stable tasting identity until a new tasting begins`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)

    val initialSession = viewModel.state.value.activePreparationSessionId
    val initialCata = viewModel.state.value.activeCataId
    viewModel.startTimer()

    val brewing = viewModel.state.value
    assertTrue(initialSession != brewing.activePreparationSessionId)
    assertTrue(initialCata != brewing.activeCataId)
    assertEquals(null, brewing.savedCataCupId)

    val session = brewing.activePreparationSessionId
    val cata = brewing.activeCataId
    viewModel.beginNewCata()
    val next = viewModel.state.value
    assertTrue(session != next.activePreparationSessionId)
    assertTrue(cata != next.activeCataId)
    assertEquals(null, next.savedCataCupId)
    assertEquals(0, next.cataMinutesElapsed)
  }

  @Test
  fun `saving the same tasting twice creates only one cup and preserves real duration`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    viewModel.startTimer()
    viewModel.advanceStep()
    val expectedDuration = viewModel.state.value.elapsedSeconds
    val cupId = viewModel.state.value.activePreparationSessionId
    viewModel.stopTimer()
    val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())

    val firstResult = kotlinx.coroutines.CompletableDeferred<Boolean>()
    viewModel.saveCup("Cacao", "Chocolate", 4f, "Balanceada") { firstResult.complete(it) }
    repeat(100) {
      mainLooper.idle()
      if (firstResult.isCompleted) return@repeat
      Thread.sleep(20)
    }
    assertTrue(withTimeout(5_000) { firstResult.await() })
    repeat(100) {
      mainLooper.idle()
      if (viewModel.state.value.cupsList.any { it.id == cupId }) return@repeat
      Thread.sleep(20)
    }
    assertTrue(viewModel.state.value.cupsList.any { it.id == cupId })

    val secondResult = kotlinx.coroutines.CompletableDeferred<Boolean>()
    viewModel.saveCup("Cacao", "Chocolate", 4f, "Balanceada") { secondResult.complete(it) }
    mainLooper.idle()
    assertTrue(withTimeout(2_000) { secondResult.await() })

    val saved = viewModel.state.value.cupsList.filter { it.id == cupId }
    assertEquals(1, saved.size)
    assertEquals(expectedDuration, saved.single().executedDurationSeconds)
    assertEquals(cupId, viewModel.state.value.savedCataCupId)
  }

  @Test
  fun `double tapping bean save creates only one inventory record`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
    val uniqueName = "Café doble toque ${java.util.UUID.randomUUID()}"

    repeat(2) {
      viewModel.saveBean(
        roaster = "Tostador prueba",
        name = uniqueName,
        origin = "México",
        altitude = "1800",
        process = "Lavado",
        roastDate = "2026-09-20",
        firstUseDate = "",
        notes = "Protección contra doble guardado",
        status = "cerrado",
        stockGrams = 250f
      )
    }

    repeat(200) {
      mainLooper.idle()
      if (viewModel.state.value.beansList.any { it.name == uniqueName }) return@repeat
      Thread.sleep(20)
    }
    assertEquals(1, viewModel.state.value.beansList.count { it.name == uniqueName })
  }

  @Test
  fun `double tapping storage saves creates one grinder equipment recipe and experiment`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
    val suffix = java.util.UUID.randomUUID().toString()
    val grinderModel = "Molino-$suffix"
    val equipmentName = "Báscula-$suffix"
    val recipeName = "Receta-$suffix"
    val experimentName = "Hipótesis-$suffix"

    repeat(2) { viewModel.addGrinder("Marca", grinderModel, "0–40", "Calibrado") }
    repeat(2) { viewModel.addEquipment(equipmentName, "SCALE", "Precisión 0.1 g") }
    repeat(2) {
      viewModel.addRecipe(
        name = recipeName,
        ingredientsList = listOf(com.example.data.engine.RecipeIngredientInput(name = "Café", amount = "18,5", unit = "G")),
        stepsList = listOf(com.example.data.engine.RecipeStepInput(instruction = "Preparar"))
      )
    }
    repeat(2) { viewModel.addExperiment(experimentName, 15f, 240, 16f, 92, "Media", "Prueba") }

    repeat(300) {
      mainLooper.idle()
      val state = viewModel.state.value
      val allVisible = state.grindersList.any { it.model == grinderModel } &&
        state.equipmentList.any { it.name == equipmentName } &&
        state.recipesList.any { it.name == recipeName } &&
        state.experimentsList.any { it.experimentHypothesis == experimentName }
      if (allVisible) return@repeat
      Thread.sleep(20)
    }

    val state = viewModel.state.value
    assertEquals(1, state.grindersList.count { it.model == grinderModel })
    assertEquals(1, state.equipmentList.count { it.name == equipmentName })
    assertEquals(1, state.recipesList.count { it.name == recipeName })
    assertEquals(1, state.experimentsList.count { it.experimentHypothesis == experimentName })
    val savedRecipe = state.recipesList.single { it.name == recipeName }
    val database = com.example.data.database.AppDatabase.getDatabase(application)
    assertEquals(18.5f, database.recipeIngredientDao().getIngredientsForRecipeSync(savedRecipe.id).single().amount, 0.001f)
    assertEquals("Preparar", database.recipeStepDao().getStepsForRecipeSync(savedRecipe.id).single().instruction)
  }

  @Test
  fun `selected calculator favorite persists until it is removed`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preferences = context.getSharedPreferences("favorite_test", Context.MODE_PRIVATE)
    preferences.edit().clear().commit()
    val favorite = com.example.data.database.RatioPreset(
      id = "favorite-18-15",
      methodName = "V60",
      coffeeGrams = 18f,
      ratio = 15f,
      label = "V60 · 18g · 1:15"
    )
    val first = com.example.ui.viewmodel.CalculatorFavoriteStore(preferences)
    first.select(favorite.id, "owner-a")

    val restored = com.example.ui.viewmodel.CalculatorFavoriteStore(preferences)
    assertEquals(favorite, restored.selectedPreset(listOf(favorite), "owner-a"))
    assertEquals(null, restored.selectedPreset(listOf(favorite), "owner-b"))
    restored.select("favorite-owner-b", "owner-b")
    assertEquals(favorite.id, restored.selectedId("owner-a"))
    assertEquals("favorite-owner-b", restored.selectedId("owner-b"))
    restored.clearIfSelected(favorite.id, "owner-a")
    assertEquals(null, com.example.ui.viewmodel.CalculatorFavoriteStore(preferences).selectedId("owner-a"))
    assertEquals("favorite-owner-b", restored.selectedId("owner-b"))
  }

  @Test
  fun `preparation step accessibility says action total time and status without relying on color`() {
    val step = com.example.data.database.TechniqueStep(
      techniqueId = "technique-a",
      stepNumber = 2,
      title = "Vertido central",
      durationSeconds = 75,
      waterAddedMl = 120,
      waterAccumulatedMl = 180
    )

    assertEquals(
      "Paso 2 de 3. Activo. Vertido central. Agrega 120 mililitros. Total en báscula 180 mililitros. Tiempo del paso 1:15.",
      com.example.ui.screens.preparationStepAccessibilityLabel(step, 2, 3, "Activo")
    )
  }

  @Test
  fun `owner scope changes reset account-bound screen identity`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val session = com.example.data.remote.SessionManager(application)
    session.clearSession()
    try {
      val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
      assertEquals("guest", withTimeout(2_000) { viewModel.state.first { it.ownerScopeKey == "guest" }.ownerScopeKey })
      viewModel.startTimer()
      val guestSessionId = viewModel.state.value.activePreparationSessionId

      session.saveSession("token-a", "owner-a", "a@example.com", "A", "a", null)
      assertEquals("owner-a", withTimeout(2_000) { viewModel.state.first { it.ownerScopeKey == "owner-a" }.ownerScopeKey })
      val accountState = viewModel.state.value
      assertEquals(false, accountState.timerRunning)
      assertEquals(null, accountState.activePrepTechniqueId)
      assertEquals(null, accountState.activePrepBeanId)
      assertEquals(null, accountState.activePrepGrinderId)
      assertEquals(null, accountState.savedCataCupId)
      assertTrue(guestSessionId != accountState.activePreparationSessionId)

      session.clearSession()
      assertEquals("guest", withTimeout(2_000) { viewModel.state.first { it.ownerScopeKey == "guest" }.ownerScopeKey })
    } finally {
      session.clearSession()
    }
  }

  @Test
  fun `calculator lab preparation and tasting preserve the method identity`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)

    viewModel.onMethodSelected("AeroPress")
    viewModel.onCoffeeChanged("18")
    viewModel.onActionLab()
    val bean = com.example.data.database.Bean(
      id = "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee",
      roaster = "Tostador prueba",
      name = "Etiopía prueba",
      origin = "Etiopía",
      altitude = "1900",
      process = "Lavado",
      roastDate = "2026-09-01",
      firstUseDate = "",
      notes = "Jazmín",
      stockGrams = 250f
    )
    viewModel.selectBeanForLab(bean)
    viewModel.selectMethodForLab("11111111-1111-4000-8000-000000000002", "AeroPress")
    assertEquals("AeroPress", viewModel.state.value.labMethod)
    assertEquals("11111111-1111-4000-8000-000000000002", viewModel.state.value.labMethodId)
    assertEquals(18f, viewModel.state.value.labCoffee, 0.001f)
    assertEquals(bean.id, viewModel.state.value.labBeanId)

    viewModel.updateLabVariables(
      water = 234,
      ratio = 13f,
      temperature = 91,
      clicks = 17,
      notes = "Prueba integral"
    )
    viewModel.playLabIdeaAsPrep()
    val preparation = viewModel.state.value
    assertEquals("AeroPress", preparation.activePrepMethod)
    assertEquals("11111111-1111-4000-8000-000000000002", preparation.activePrepMethodId)
    assertEquals(null, preparation.activePrepTechniqueId)
    assertEquals(bean.id, preparation.activePrepBeanId)
    assertEquals(234, preparation.activePrepWater)
    assertEquals(234, preparation.activePrepSteps.last().waterAccumulatedMl)

    viewModel.setCataTexture("sedosa")
    viewModel.setCataCleanliness("alta")
    viewModel.pullCataToLab()
    assertEquals("AeroPress", viewModel.state.value.labMethod)
    assertEquals(bean.id, viewModel.state.value.labBeanId)
    assertEquals(234, viewModel.state.value.labWater)
    assertTrue(viewModel.state.value.labNotes.contains("Textura: sedosa"))
    assertTrue(viewModel.state.value.labNotes.contains("Limpieza: alta"))
  }

  @Test
  fun `laboratory technique stays consistent through storage preparation and tasting`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val session = com.example.data.remote.SessionManager(application)
    session.clearSession()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
    val techniqueName = "Laboratorio integral ${java.util.UUID.randomUUID()}"

    viewModel.selectMethodForLab("11111111-1111-4000-8000-000000000002", "AeroPress")
    viewModel.updateLabVariables(coffee = 20f, ratio = 15f)
    assertEquals(300, viewModel.state.value.labWater)
    assertEquals(15f, viewModel.state.value.labRatio, 0.001f)
    viewModel.updateLabVariables(water = 280)
    assertEquals(14f, viewModel.state.value.labRatio, 0.001f)
    viewModel.updateLabVariables(coffee = 18f, ratio = 13f, temperature = 91, clicks = 17, estTimeSeconds = 155)
    assertEquals(234, viewModel.state.value.labWater)

    val saved = kotlinx.coroutines.CompletableDeferred<Boolean>()
    viewModel.saveLabAsTechnique(techniqueName) { saved.complete(it) }
    viewModel.saveLabAsTechnique(techniqueName)
    repeat(250) {
      mainLooper.idle()
      if (saved.isCompleted && viewModel.state.value.techniquesList.any { it.name == techniqueName }) return@repeat
      Thread.sleep(20)
    }
    assertTrue(withTimeout(5_000) { saved.await() })
    assertEquals(1, viewModel.state.value.techniquesList.count { it.name == techniqueName })

    val technique = viewModel.state.value.techniquesList.single { it.name == techniqueName }
    val storedSteps = viewModel.getTechniqueSteps(technique.id)
    assertEquals(18f, technique.doseG, 0.001f)
    assertEquals(234, technique.waterMl)
    assertEquals(13f, technique.ratio, 0.001f)
    assertEquals(155, technique.totalTimeSeconds)
    assertEquals(234, storedSteps.sumOf { it.waterAddedMl })
    assertEquals(234, storedSteps.last().waterAccumulatedMl)
    assertEquals(155, storedSteps.sumOf { it.durationSeconds })
    assertEquals(storedSteps.runningFold(0) { total, step -> total + step.waterAddedMl }.drop(1), storedSteps.map { it.waterAccumulatedMl })

    viewModel.loadPrepTechnique(technique.id)
    repeat(200) {
      mainLooper.idle()
      if (viewModel.state.value.activePrepTechniqueId == technique.id) return@repeat
      Thread.sleep(20)
    }
    val prepared = viewModel.state.value
    assertEquals(technique.id, prepared.activePrepTechniqueId)
    assertEquals(234, prepared.activePrepWater)
    assertEquals(13f, prepared.activePrepRatio, 0.001f)
    assertEquals(storedSteps.map { it.waterAccumulatedMl }, prepared.activePrepSteps.map { it.waterAccumulatedMl })

    viewModel.startTimer()
    repeat(viewModel.state.value.activePrepSteps.size) { viewModel.advanceStep() }
    assertTrue(viewModel.state.value.preparationCompleted)
    assertEquals(155, viewModel.state.value.elapsedSeconds)

    val cupId = viewModel.state.value.activePreparationSessionId
    val cupSaved = kotlinx.coroutines.CompletableDeferred<Boolean>()
    viewModel.saveCup("Cacao", "Dulzor", 4.5f, "Recorrido integral") { cupSaved.complete(it) }
    repeat(250) {
      mainLooper.idle()
      if (cupSaved.isCompleted && viewModel.state.value.cupsList.any { it.id == cupId }) return@repeat
      Thread.sleep(20)
    }
    assertTrue(withTimeout(5_000) { cupSaved.await() })
    val cup = viewModel.state.value.cupsList.single { it.id == cupId }
    assertEquals(technique.id, cup.techniqueId)
    assertEquals(technique.methodId, cup.methodId)
    assertEquals(18f, cup.executedDoseG, 0.001f)
    assertEquals(234, cup.executedWaterMl)
    assertEquals(13f, cup.executedRatio, 0.001f)
    assertEquals(155, cup.executedDurationSeconds)
    assertEquals(techniqueName, cup.techniqueNameSnapshot)
  }

  @Test
  fun `laboratory recipe saves ingredients and ordered instructions only once`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val session = com.example.data.remote.SessionManager(application)
    session.clearSession()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    val database = com.example.data.database.AppDatabase.getDatabase(application)
    val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
    val recipeName = "Receta laboratorio ${java.util.UUID.randomUUID()}"

    viewModel.selectMethodForLab("11111111-1111-4000-8000-000000000001", "V60")
    viewModel.updateLabVariables(coffee = 18.5f, water = 296, temperature = 92, estTimeSeconds = 175)
    val saved = kotlinx.coroutines.CompletableDeferred<Boolean>()
    viewModel.saveLabAsRecipe(recipeName) { saved.complete(it) }
    viewModel.saveLabAsRecipe(recipeName)

    repeat(250) {
      mainLooper.idle()
      if (saved.isCompleted && viewModel.state.value.recipesList.any { it.name == recipeName }) return@repeat
      Thread.sleep(20)
    }
    assertTrue(withTimeout(5_000) { saved.await() })
    assertEquals(1, viewModel.state.value.recipesList.count { it.name == recipeName })

    val recipe = viewModel.state.value.recipesList.single { it.name == recipeName }
    val ingredients = database.recipeIngredientDao().getIngredientsForRecipeSync(recipe.id)
    val steps = database.recipeStepDao().getStepsForRecipeSync(recipe.id)
    assertEquals(listOf("Café", "Agua"), ingredients.map { it.name })
    assertEquals(18.5f, ingredients.first().amount, 0.001f)
    assertEquals(296f, ingredients.last().amount, 0.001f)
    assertTrue(steps.size >= 2)
    assertEquals((1..steps.size).toList(), steps.map { it.stepNumber })
    assertTrue(steps.all { it.instruction.isNotBlank() })
  }

  @Test
  fun `storage technique opens preparation with its own quantities and pours`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val session = com.example.data.remote.SessionManager(application)
    session.clearSession()
    val viewModel = com.example.ui.viewmodel.BaristaCalcViewModel(application)
    val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
    val techniqueName = "Técnica almacén ${java.util.UUID.randomUUID()}"
    val saved = kotlinx.coroutines.CompletableDeferred<Boolean>()

    viewModel.createAndSaveTechnique(
      name = techniqueName,
      methodId = "11111111-1111-4000-8000-000000000002",
      coffee = 20f,
      temp = 88,
      grinderId = null,
      grinderName = "Manual",
      clicks = 17,
      notes = "Prueba directa",
      stepTitles = listOf("Carga", "Presión"),
      stepTimes = listOf(40, 50),
      stepWaters = listOf(80, 180)
    ) { saved.complete(it) }

    repeat(200) {
      mainLooper.idle()
      if (saved.isCompleted) return@repeat
      Thread.sleep(20)
    }
    assertTrue(saved.isCompleted && saved.await())
    repeat(200) {
      mainLooper.idle()
      if (viewModel.state.value.techniquesList.any { it.name == techniqueName }) return@repeat
      Thread.sleep(20)
    }
    val technique = viewModel.state.value.techniquesList.first { it.name == techniqueName }
    viewModel.onCoffeeChanged("15")
    viewModel.onRatioChanged("16")
    viewModel.startTimer()

    viewModel.loadPrepTechnique(technique.id)
    repeat(200) {
      mainLooper.idle()
      if (viewModel.state.value.activePrepTechniqueId == technique.id && viewModel.state.value.activePrepSteps.size == 2) return@repeat
      Thread.sleep(20)
    }
    val loaded = viewModel.state.value

    assertEquals(20f, loaded.activePrepCoffee)
    assertEquals(260, loaded.activePrepWater)
    assertEquals(13f, loaded.activePrepRatio)
    assertEquals(88, loaded.activePrepTemp)
    assertEquals(listOf(80, 260), loaded.activePrepSteps.map { it.waterAccumulatedMl })
    assertEquals(false, loaded.timerRunning)
    assertEquals(0, loaded.elapsedSeconds)
    assertEquals(0, loaded.activeStepIndex)

    viewModel.duplicateTechnique(technique.id)
    viewModel.duplicateTechnique(technique.id)
    repeat(200) {
      mainLooper.idle()
      if (viewModel.state.value.techniquesList.count { it.name == "Copia de $techniqueName" } == 1) return@repeat
      Thread.sleep(20)
    }
    val copies = viewModel.state.value.techniquesList.filter { it.name == "Copia de $techniqueName" }
    assertEquals(1, copies.size)
    assertEquals(2, viewModel.getTechniqueSteps(copies.single().id).size)
  }
}
