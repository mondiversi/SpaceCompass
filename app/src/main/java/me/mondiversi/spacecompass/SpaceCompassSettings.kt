package me.mondiversi.spacecompass

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

internal val LocalSpaceCompassDeviceLocale = staticCompositionLocalOf { Locale.getDefault() }

internal val LocalSpaceCompassUnits = staticCompositionLocalOf { SpaceCompassUnits() }
internal val LocalSpaceCompassPreferences = staticCompositionLocalOf<SharedPreferences?> { null }

/** Presentation overrides preserve the activity context and foreground sensor jobs. */
@Composable
internal fun SpaceCompassPreferences(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, Context.MODE_PRIVATE).also { saved ->
            if (saved.getString(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY, null) !in spaceCompassDistanceSpeedOptions) {
                val migrated = spaceCompassDistanceSpeedPreference { saved.getString(it, null) }
                saved.edit().putString(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY, migrated).apply()
            }
        }
    }
    val settings by rememberSpaceCompassPresentationSettings(preferences)
    val system = LocalConfiguration.current
    val theme = settings["theme"]
    val language = settings["language"]
    val configuration = remember(system, theme, language) {
        Configuration(system).apply {
            when (theme) {
                "light" -> uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
                "dark" -> uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            language?.takeIf { it != "system" }?.let {
                val locale = Locale.forLanguageTag(it)
                setLocales(LocaleList(locale))
                setLayoutDirection(locale)
            }
        }
    }
    val resources = remember(configuration) { context.createConfigurationContext(configuration).resources }
    // Resolve regional defaults before applying the app language override.
    val region = system.locales[0].country.uppercase(Locale.ROOT)
    val units = remember(region, settings) { spaceCompassResolveUnits(region, settings::get) }
    CompositionLocalProvider(
        LocalConfiguration provides configuration, LocalResources provides resources,
        androidx.compose.ui.platform.LocalLayoutDirection provides if (configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL)
            androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
        LocalSpaceCompassPreferences provides preferences, LocalSpaceCompassPresentationSettings provides settings,
        LocalSpaceCompassUnits provides units,
        LocalSpaceCompassDeviceLocale provides system.locales[0],
        LocalSpaceCompassNumericFormat provides SpaceCompassNumericFormat.fromStoredValue(settings[SPACE_COMPASS_NUMERIC_FORMAT_KEY]),
        LocalSpaceCompassDateFormat provides SpaceCompassDateFormat.fromStoredValue(settings[SPACE_COMPASS_DATE_FORMAT_KEY]),
        LocalSpaceCompassTimeFormat provides resolveSpaceCompassTimeFormat(context, SpaceCompassTimeFormat.fromStoredValue(settings[SPACE_COMPASS_TIME_FORMAT_KEY]))
    ) { content() }
}

internal val spaceCompassLanguages = listOf(
    "en" to "English", "it" to "Italiano", "es" to "Español", "fr" to "Français", "de" to "Deutsch",
    "pt" to "Português", "ru" to "Русский", "tr" to "Türkçe", "ar" to "العربية", "he" to "עברית",
    "hi" to "हिन्दी", "zh-CN" to "中文", "ja" to "日本語", "ko" to "한국어", "sw" to "Kiswahili",
    "pl" to "Polski", "nl" to "Nederlands", "id" to "Bahasa Indonesia", "fa" to "فارسی", "el" to "Ελληνικά"
)
