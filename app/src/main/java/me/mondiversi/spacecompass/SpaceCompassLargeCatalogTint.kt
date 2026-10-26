package me.mondiversi.spacecompass

import androidx.compose.ui.graphics.Color

/** Keep every released colour, and avoid 8-bit RGB collisions when the catalogue grows.
 * Saturation/value variation is necessary: a fixed pastel hue circle has fewer than 999 colours. */
private val largeCatalogTints: Map<SpaceCompassCelestialBody, Color> by lazy {
    val used = SpaceCompassCelestialBody.entries.filter { it.largeCatalogEntry == null }
        .map(::spaceCompassCelestialPathTint).toMutableSet()
    buildMap {
        spaceCompassLargeCatalogEntries.forEach { row ->
            var seed = row.body.name.hashCode()
            while (true) {
                seed = seed * 1664525 + 1013904223
                val hue = (seed.toUInt() % 36000u).toFloat() / 100
                val saturation = .32f + ((seed ushr 12) and 31) / 100f
                val brightness = .88f + ((seed ushr 20) and 7) / 100f
                val color = Color.hsv(hue, saturation, brightness)
                if (used.add(color)) { put(row.body, color); break }
            }
        }
    }
}
internal fun spaceCompassLargeCatalogTint(body: SpaceCompassCelestialBody): Color? =
    if (body.largeCatalogEntry == null) null else largeCatalogTints.getValue(body)
