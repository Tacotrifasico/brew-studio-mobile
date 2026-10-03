package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.example.ui.viewmodel.SocialViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class PreparationNavigationTest {
    @get:Rule val compose = createComposeRule()
    private fun selectTab(title: String) {
        compose.onNode(hasContentDescription(title) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
    }

    @Test fun returningFromTastingShowsPreparationInsteadOfRestoringTastingAboveIt() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val brew = BaristaCalcViewModel(application)
        brew.onMethodSelected("V60")
        val social = SocialViewModel(application)
        compose.setContent { MyApplicationTheme { BrewStudioAppShell(brew, social) } }
        selectTab("Preparar")
        compose.onNodeWithText("Iniciar preparación").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(brew.state.value.timerRunning) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Completar extracción e ir a Cata"))
        compose.onNodeWithText("Completar extracción e ir a Cata").performClick()
        selectTab("Preparar")
        compose.onNodeWithText("Finalizada").assertIsDisplayed()
        selectTab("Cata")
        selectTab("Preparar")
        compose.onNodeWithText("Finalizada").assertIsDisplayed()
    }
}
