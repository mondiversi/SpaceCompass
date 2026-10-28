package me.mondiversi.spacecompass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

internal enum class SpaceCompassCatalogRowFamily { PLANETARY, STELLAR, DEEP_SKY, COMPACT, SMALL_BODY, ARTIFICIAL }

/** Fictional references use their physical category; origin remains an independent filter. */
internal val SpaceCompassCelestialBody.catalogRowFamily: SpaceCompassCatalogRowFamily get() =
    (fictionalReference?.filterType ?: catalogType).catalogRowFamily

private val SpaceCompassCatalogType.catalogRowFamily: SpaceCompassCatalogRowFamily get() =
    when (this) {
        SpaceCompassCatalogType.PLANET, SpaceCompassCatalogType.DWARF_PLANET,
        SpaceCompassCatalogType.EXOPLANET, SpaceCompassCatalogType.NATURAL_SATELLITE -> SpaceCompassCatalogRowFamily.PLANETARY
        SpaceCompassCatalogType.STAR -> SpaceCompassCatalogRowFamily.STELLAR
        SpaceCompassCatalogType.GALAXY, SpaceCompassCatalogType.NEBULA, SpaceCompassCatalogType.STAR_CLUSTER,
        SpaceCompassCatalogType.CONSTELLATION -> SpaceCompassCatalogRowFamily.DEEP_SKY
        SpaceCompassCatalogType.BLACK_HOLE, SpaceCompassCatalogType.NEUTRON_STAR -> SpaceCompassCatalogRowFamily.COMPACT
        SpaceCompassCatalogType.COMET, SpaceCompassCatalogType.ASTEROID -> SpaceCompassCatalogRowFamily.SMALL_BODY
        SpaceCompassCatalogType.SPACE_STATION, SpaceCompassCatalogType.ARTIFICIAL_SATELLITE,
        SpaceCompassCatalogType.SPACE_PROBE, SpaceCompassCatalogType.SPACECRAFT,
        SpaceCompassCatalogType.SCIENCE_FICTION -> SpaceCompassCatalogRowFamily.ARTIFICIAL
    }

/** Recognisable category tints remain visible on selected rows, alongside checkbox, bold name and thumbnail rim. */
internal fun spaceCompassCatalogRowBackground(body: SpaceCompassCelestialBody, selected: Boolean,
    base: Color): Color = spaceCompassCatalogFamilyBackground(body.catalogRowFamily, selected, base)

/** Type choices share catalogue row colors. Fiction spans physical categories, so its choice stays neutral. */
internal fun spaceCompassCatalogTypeBackground(type: SpaceCompassCatalogType, selected: Boolean,
    base: Color): Color = if (type == SpaceCompassCatalogType.SCIENCE_FICTION) base
    else spaceCompassCatalogFamilyBackground(type.catalogRowFamily, selected, base)

private fun spaceCompassCatalogFamilyBackground(family: SpaceCompassCatalogRowFamily, selected: Boolean,
    base: Color): Color {
    val dark = base.luminance() < .4f
    val tint = when (family) {
        SpaceCompassCatalogRowFamily.PLANETARY -> if (dark) Color(0xFF79B6E9) else Color(0xFF5B9DD6)
        SpaceCompassCatalogRowFamily.STELLAR -> if (dark) Color(0xFFFFCE72) else Color(0xFFE4A235)
        SpaceCompassCatalogRowFamily.DEEP_SKY -> if (dark) Color(0xFFC4A2F2) else Color(0xFF9F81CF)
        SpaceCompassCatalogRowFamily.COMPACT -> if (dark) Color(0xFF78A5B7) else Color(0xFF477C91)
        SpaceCompassCatalogRowFamily.SMALL_BODY -> if (dark) Color(0xFF9BD1B0) else Color(0xFF5F9B75)
        SpaceCompassCatalogRowFamily.ARTIFICIAL -> if (dark) Color(0xFFA3B4C5) else Color(0xFF8898A7)
    }
    return tint.copy(alpha = if (dark) { if (selected) .28f else .22f }
        else { if (selected) .30f else .22f }).compositeOver(base)
}
