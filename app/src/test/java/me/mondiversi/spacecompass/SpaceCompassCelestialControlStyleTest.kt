package me.mondiversi.spacecompass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialControlStyleTest {
    @Test fun allControlsHaveOneOutlineWhetherSelectedOrNot() {
        val normal = spaceCompassCelestialControlStyle(false)
        val selected = spaceCompassCelestialControlStyle(true)
        assertEquals(normal.border, selected.border)
        assertEquals(Color.White.copy(alpha = 0.65f), normal.border)
        assertNotEquals(normal.background, selected.background)
        assertTrue(selected.background.red > normal.background.red)
        for (isSelected in listOf(false, true)) {
            val resting = spaceCompassCelestialControlStyle(isSelected)
            val pressed = spaceCompassCelestialControlStyle(isSelected, pressed = true)
            assertEquals(resting.border, pressed.border)
            assertTrue(pressed.background.red > resting.background.red)
        }
        for (style in listOf(normal, selected, spaceCompassCelestialControlStyle(false, true), spaceCompassCelestialControlStyle(true, true))) {
            assertEquals(style.background.red, style.background.green, 1e-6f)
            assertEquals(style.background.red, style.background.blue, 1e-6f)
        }
    }

    @Test fun whiteSymbolsHaveReadableContrastOnLightAndDarkSkyOrModelImages() {
        val scenes = listOf(Color.White, Color.Black, Color(0xFF90CAF9), Color(0xFF28314A), Color(0xFFD0B080))
        for (selected in listOf(false, true)) for (pressed in listOf(false, true)) for (scene in scenes) {
            val fill = spaceCompassCelestialControlStyle(selected, pressed).background.compositeOver(scene)
            val contrast = (Color.White.luminance() + 0.05f) / (fill.luminance() + 0.05f)
            assertTrue("selected=$selected, pressed=$pressed on $scene: contrast=$contrast", contrast >= 3f)
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
