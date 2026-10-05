package me.mondiversi.spacecompass

import java.util.Locale

internal data class SpaceCompassUnits(
    val miles: Boolean = false, val distance: String = "default",
    val feet: Boolean = false, val fahrenheit: Boolean = false, val dms: Boolean = false
)
/** Device-region defaults are independent of the chosen interface language. */
internal fun spaceCompassResolveUnits(region: String, preference: (String) -> String?): SpaceCompassUnits {
    val imperial = region.uppercase(Locale.ROOT) in setOf("US", "LR", "MM")
    fun selected(key: String, option: String, fallback: Boolean): Boolean = when (val value = preference(key)) {
        "system", null -> fallback
        else -> value == option
    }
    val distance = when (preference("distance")) {
        "default", "system", null -> if (imperial) "mmi" else "mkm"
        "mi", "mmi" -> "mmi"
        else -> "mkm"
    }
    return SpaceCompassUnits(selected("speed", "mi", imperial), distance,
        selected("altitude", "ft", imperial),
        selected("temperature", "f", region.uppercase(Locale.ROOT) in setOf("US", "BS", "BZ", "KY", "PW", "FM", "MH")),
        selected("coordinates", "dms", false))
}
