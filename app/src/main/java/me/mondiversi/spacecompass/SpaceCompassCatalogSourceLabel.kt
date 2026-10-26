package me.mondiversi.spacecompass

/** Institutional names stay compact in the table; versions/URLs remain in export notes and provenance. */
internal val SpaceCompassLargeCatalogEntry.sourceLabel: String get() = when {
    source.startsWith("HYG") -> "HYG v4.1"
    source.startsWith("OpenNGC") -> if ("NASA" in source) "OpenNGC · NASA" else "OpenNGC"
    source.startsWith("Stellarium") -> "Stellarium · HYG"
    source.startsWith("SIMBAD") -> "SIMBAD · CDS"
    source.startsWith("NASA Exoplanet") -> "NASA Exoplanet Archive"
    source.startsWith("NASA/JPL") -> "NASA/JPL"
    source.startsWith("NORAD") -> "NORAD · CelesTrak"
    else -> source.substringBefore(" · ")
}
