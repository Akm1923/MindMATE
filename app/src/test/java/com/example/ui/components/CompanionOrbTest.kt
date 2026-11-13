package com.example.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.example.data.model.OrbState
import com.example.ui.theme.MindMateTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompanionOrbTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun companionOrb_rendersAllStatesCleanly() {
        composeRule.setContent {
            MindMateTheme {
                OrbState.entries.forEach { state ->
                    CompanionOrb(
                        orbState = state,
                        audioAmplitude = 0.5f,
                        reducedMotion = true
                    )
                }
            }
        }

        OrbState.entries.forEach { state ->
            composeRule
                .onNodeWithContentDescription("MindMate 3D Companion Orb, state: ${state.label}")
                .assertIsDisplayed()
        }
    }
}
