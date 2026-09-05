package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
