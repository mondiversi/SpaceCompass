package me.mondiversi.planetcompass

import android.content.res.Configuration

/** Fixture-only theme override; it does not change device or application preferences. */
internal enum class PlanetCompassAppTheme { LIGHT, DARK }

internal fun planetCompassThemeConfiguration(source: Configuration, theme: PlanetCompassAppTheme): Configuration =
    Configuration(source).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (theme == PlanetCompassAppTheme.DARK) Configuration.UI_MODE_NIGHT_YES
            else Configuration.UI_MODE_NIGHT_NO
    }
