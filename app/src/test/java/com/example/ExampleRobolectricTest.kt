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
    first.select(favorite.id)

    val restored = com.example.ui.viewmodel.CalculatorFavoriteStore(preferences)
    assertEquals(favorite, restored.selectedPreset(listOf(favorite)))
    restored.clearIfSelected(favorite.id)
    assertEquals(null, com.example.ui.viewmodel.CalculatorFavoriteStore(preferences).selectedId())
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
}
