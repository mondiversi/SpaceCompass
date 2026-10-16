package me.mondiversi.spacecompass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Main sky overlays share translucency while their text and layout remain unchanged.
internal const val SPACE_COMPASS_MAIN_OVERLAY_ALPHA = .72f

// A point caption and its adjacent reminder button share the same subtle translucency.
internal const val SPACE_COMPASS_POINT_CAPTION_ALPHA = .88f

/** High-contrast attention text for light and dark status notices. */
internal fun spaceCompassAttentionColor(dark: Boolean): Color =
    if (dark) Color(0xFFFF8F1F) else Color(0xFFAB3D00)

/** Reviewed day/night accents for the active scenario icon and its count badge. */
internal fun spaceCompassScenarioAccentColor(dark: Boolean): Color =
    if (dark) Color(0xFFF2AA45) else Color(0xFFFF9630)

/** Shared page/card neutrals for Compose and native screens; the app keeps its blue/cyan accent. */
internal data class SpaceCompassSurfacePalette(val background: Color, val island: Color)

private val lightSurfacePalette = SpaceCompassSurfacePalette(Color(0xFFF4F7F9), Color.White)
private val darkSurfacePalette = SpaceCompassSurfacePalette(Color(0xFF101418), Color(0xFF1C242B))

internal fun spaceCompassSurfacePalette(dark: Boolean): SpaceCompassSurfacePalette =
    if (dark) darkSurfacePalette else lightSurfacePalette

@Composable
internal fun spaceCompassPageBackground(): Color = spaceCompassSurfacePalette(isSystemInDarkTheme()).background

@Composable
internal fun spaceCompassSettingsCardColor(): Color = spaceCompassSurfacePalette(isSystemInDarkTheme()).island
