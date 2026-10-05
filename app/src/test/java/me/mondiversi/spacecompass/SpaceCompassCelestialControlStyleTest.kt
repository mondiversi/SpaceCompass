package me.mondiversi.spacecompass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialControlStyleTest {
    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)

    @Test fun viewerTabsHaveStronglyDifferentSelectedAndInactiveStatesEvenWhenPressed() {
        for (pressed in listOf(false, true)) {
            val inactive = spaceCompassCelestialControlStyle(false, pressed)
            val selected = spaceCompassCelestialControlStyle(true, pressed)
            assertTrue(contrast(inactive.background, selected.background) >= 4.5f)
            assertNotEquals(inactive.content, selected.content)
            assertNotEquals(inactive.border, selected.border)
            assertEquals(1f, selected.background.alpha)
            assertEquals(1f, inactive.background.alpha)
        }
    }

    @Test fun actualGlyphTintHasReadableContrastOnLightAndDarkSkyOrModelImages() {
        val scenes = listOf(Color.White, Color.Black, Color(0xFF90CAF9), Color(0xFF28314A), Color(0xFFD0B080))
        for (selected in listOf(false, true)) for (pressed in listOf(false, true)) for (scene in scenes) {
            val style = spaceCompassCelestialControlStyle(selected, pressed)
            val fill = style.background.compositeOver(scene)
            assertTrue("selected=$selected, pressed=$pressed on $scene", contrast(style.content, fill) >= 4.5f)
        }
    }

    @Test fun telescopeAndOrbitActionsHaveExactlyTheSameDarkGreyFillEvenWhenPathIsShown() {
        for (pressed in listOf(false, true)) {
            val view = spaceCompassCelestialControlStyle(false, pressed, Role.Button)
            val orbit = spaceCompassCelestialControlStyle(true, pressed, Role.Button)
            assertEquals(view, orbit)
        }
        assertEquals(Color(0xFF424242), spaceCompassCelestialControlStyle(false, role = Role.Button).background)
    }

    @Test fun pressFeedbackStaysInsideTheVisibleCircleWithoutShrinkingTheTouchTarget() {
        val source = listOf(File("src/main/java/me/mondiversi/spacecompass/SpaceCompassCelestialIconControl.kt"),
            File("app/src/main/java/me/mondiversi/spacecompass/SpaceCompassCelestialIconControl.kt")).first { it.isFile }.readText()
        assertTrue(source.contains("modifier.size(48.dp)"))
        assertTrue(source.contains("Modifier.size(30.dp).clip(CircleShape)"))
        assertTrue(source.contains(".border(1.dp, style.border, CircleShape)"))
        assertFalse(source.contains("if (selected) Modifier.border"))
        assertTrue(source.contains("pressSource.collectIsPressedAsState()"))
        assertTrue(source.contains("spaceCompassCelestialControlStyle(selected, pressed, role)"))
        assertFalse(source.contains(".indication("))
        assertEquals(2, Regex("interactionSource = pressSource").findAll(source).count())
        assertEquals(2, Regex("indication = null").findAll(source).count())
    }
}
