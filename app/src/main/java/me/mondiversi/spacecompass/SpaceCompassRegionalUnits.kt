package me.mondiversi.spacecompass

import java.util.Locale

internal const val SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY = "distance_speed_unit"
internal val spaceCompassDistanceSpeedOptions = setOf("system", "metric", "imperial")
internal const val SPACE_COMPASS_MASS_UNIT_KEY = "mass_unit"

internal data class SpaceCompassUnits(
    val miles: Boolean = false, val distance: String = "default",
    val feet: Boolean = false, val fahrenheit: Boolean = false, val dms: Boolean = false,
    val pounds: Boolean = false
)

/** Migrate an explicit distance choice first, then nearby length, then speed.
 * A saved unified choice, including System, always wins over legacy preferences.
 */
internal fun spaceCompassDistanceSpeedPreference(preference: (String) -> String?): String {
    preference(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY)?.takeIf { it in spaceCompassDistanceSpeedOptions }?.let { return it }
    for (key in listOf("distance", "altitude", "speed")) {
        when (preference(key)) {
            "mi", "mmi", "ft" -> return "imperial"
            "km", "mkm", "m" -> return "metric"
        }
    }
    return "system"
}

/** Device-region defaults are independent of the chosen interface language.
 * All distance/length/speed displays derive from one family; mass and temperature stay separate.
 */
internal fun spaceCompassResolveUnits(region: String, preference: (String) -> String?): SpaceCompassUnits {
    val deviceRegion = region.uppercase(Locale.ROOT)
    val imperial = when (spaceCompassDistanceSpeedPreference(preference)) {
        "imperial" -> true
        "metric" -> false
        else -> deviceRegion in setOf("US", "LR", "MM")
    }
    fun selected(key: String, option: String, fallback: Boolean): Boolean = when (preference(key)) {
        "system", null -> fallback
        else -> preference(key) == option
    }
    return SpaceCompassUnits(imperial, if (imperial) "mmi" else "mkm", imperial,
        selected("temperature", "f", deviceRegion in setOf("US", "BS", "BZ", "KY", "PW", "FM", "MH")),
        selected("coordinates", "dms", false), pounds = when (preference(SPACE_COMPASS_MASS_UNIT_KEY)) {
            "kg" -> false
            "lb" -> true
            else -> deviceRegion in setOf("US", "LR", "MM")
        })
}
