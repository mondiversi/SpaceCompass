package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

internal data class SpaceCompassSettingSpec(val title: Int, val key: String, val default: String,
    val options: List<Pair<String, String>>, val enabled: Boolean = true, val description: String? = null,
    val examples: Map<String, String> = emptyMap())

@Composable
internal fun spaceCompassAppearanceSettings(): List<SpaceCompassSettingSpec> {
    val deviceDark = LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    return listOf(
    SpaceCompassSettingSpec(R.string.settings_theme_title, "theme", "system", listOf(
        "system" to stringResource(R.string.settings_theme_default), "light" to stringResource(R.string.settings_theme_light),
        "dark" to stringResource(R.string.settings_theme_dark)), description = stringResource(R.string.settings_theme_description),
        examples = mapOf("system" to stringResource(if (deviceDark) R.string.settings_theme_dark else R.string.settings_theme_light))),
    SpaceCompassSettingSpec(R.string.settings_section_display, "display", "small", listOf(
        "small" to stringResource(R.string.settings_mode_small_screen),
        "large" to stringResource(R.string.settings_mode_large_screen)), enabled = false,
        description = stringResource(R.string.settings_display_unavailable))
    )
}

@Composable
internal fun spaceCompassUnitSettings(): List<SpaceCompassSettingSpec> {
    val examples = spaceCompassUnitSettingExamples()
    val regional = listOf("system" to stringResource(R.string.numeric_format_system),
        "international" to stringResource(R.string.numeric_format_international),
        "european" to stringResource(R.string.numeric_format_european), "american" to stringResource(R.string.date_format_american))
    return listOf(
        SpaceCompassSettingSpec(R.string.units_distance_speed, SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY, "system", listOf(
            "system" to stringResource(R.string.numeric_format_system),
            "metric" to stringResource(R.string.units_metric),
            "imperial" to stringResource(R.string.units_imperial)), description = stringResource(R.string.units_distance_speed_description),
            examples = examples.getValue(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY)),
        SpaceCompassSettingSpec(R.string.units_mass_density, SPACE_COMPASS_MASS_UNIT_KEY, "system", listOf(
            "system" to stringResource(R.string.numeric_format_system),
            "kg" to stringResource(R.string.units_kilograms),
            "lb" to stringResource(R.string.units_pounds)), description = stringResource(R.string.units_mass_description),
            examples = examples.getValue(SPACE_COMPASS_MASS_UNIT_KEY)),
        SpaceCompassSettingSpec(R.string.units_pressure, SPACE_COMPASS_PRESSURE_UNIT_KEY, "system", listOf(
            "system" to stringResource(R.string.numeric_format_system),
            "bar" to stringResource(R.string.units_pressure_bar),
            "pa" to stringResource(R.string.units_pressure_pascal), "psi" to "PSI"),
            description = stringResource(R.string.units_pressure_description),
            examples = examples.getValue(SPACE_COMPASS_PRESSURE_UNIT_KEY)),
        SpaceCompassSettingSpec(R.string.pc_temperature, "temperature", "system", listOf(
            "system" to stringResource(R.string.numeric_format_system), "c" to stringResource(R.string.units_celsius),
            "f" to stringResource(R.string.units_fahrenheit)), description = stringResource(R.string.format_temperature_description),
            examples = examples.getValue("temperature")),
        SpaceCompassSettingSpec(R.string.pc_coordinates, "coordinates", "system", listOf("system" to stringResource(R.string.numeric_format_system),
            "decimal" to stringResource(R.string.pc_decimal_degrees), "dms" to stringResource(R.string.pc_dms)), description = stringResource(R.string.format_coordinates_description),
            examples = examples.getValue("coordinates")),
        SpaceCompassSettingSpec(R.string.format_numbers_heading, SPACE_COMPASS_NUMERIC_FORMAT_KEY, "system", regional, description = stringResource(R.string.format_numeric_description),
            examples = examples.getValue(SPACE_COMPASS_NUMERIC_FORMAT_KEY)),
        SpaceCompassSettingSpec(R.string.format_date_time_heading, SPACE_COMPASS_DATE_FORMAT_KEY, "system", regional, description = stringResource(R.string.format_date_description),
            examples = examples.getValue(SPACE_COMPASS_DATE_FORMAT_KEY)),
        SpaceCompassSettingSpec(R.string.format_time_heading, SPACE_COMPASS_TIME_FORMAT_KEY, "system", listOf(
            "system" to stringResource(R.string.numeric_format_system), "24_hour" to stringResource(R.string.time_format_24_hours),
            "12_hour" to stringResource(R.string.time_format_12_hours)),
            description = stringResource(R.string.format_time_description), examples = examples.getValue(SPACE_COMPASS_TIME_FORMAT_KEY))
    )
}
