package me.mondiversi.spacecompass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Main sky overlays share translucency while their text and layout remain unchanged.
internal const val SPACE_COMPASS_MAIN_OVERLAY_ALPHA = .72f

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
