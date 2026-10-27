package me.mondiversi.spacecompass

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** A private temporary preference file exercises an unfinished gesture and object-view recreation. */
class SpaceCompassTravelPersistenceUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName = "test_travel_speed_" + UUID.randomUUID()
    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val shown = mutableStateOf(true)
    private val body = mutableStateOf(SpaceCompassCelestialBody.SUN)

    @After fun disposeFixture() {
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        context.deleteSharedPreferences(preferenceName)
    }

    @Test fun aDiscreteStepIsSavedBeforeReleaseAndRestoredForAnotherObject() {
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences) {
                Box(Modifier.width(340.dp)) {
                    if (shown.value) key(body.value) {
                        SpaceCompassTravelTimeIsland(body.value, 1e9, MaterialTheme.colorScheme.onSurface,
                            MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } }
        compose.onNodeWithTag("travel-speed-slider").performTouchInput {
            down(center)
            moveTo(Offset(width - 1f, centerY))
        }
        try {
            compose.runOnIdle { assertEquals(spaceCompassTravelSpeeds.last().key,
                preferences.getString(SPACE_COMPASS_TRAVEL_SPEED_KEY, null)) }
        } finally {
            compose.onNodeWithTag("travel-speed-slider").performTouchInput { up() }
        }
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        compose.runOnIdle { body.value = SpaceCompassCelestialBody.MOON; shown.value = true }
        val restored = compose.onNodeWithTag("travel-speed-slider").fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo].current
        assertEquals(spaceCompassTravelSpeeds.lastIndex.toFloat(), restored, 0f)
    }
}
