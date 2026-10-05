package me.mondiversi.spacecompass

import android.content.res.Configuration

/** Fixture-only theme override; it does not change device or application preferences. */
internal enum class SpaceCompassAppTheme { LIGHT, DARK }

internal fun spaceCompassThemeConfiguration(source: Configuration, theme: SpaceCompassAppTheme): Configuration =
    Configuration(source).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (theme == SpaceCompassAppTheme.DARK) Configuration.UI_MODE_NIGHT_YES
            else Configuration.UI_MODE_NIGHT_NO
    }
