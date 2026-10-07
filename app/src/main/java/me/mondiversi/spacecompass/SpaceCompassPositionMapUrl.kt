package me.mondiversi.spacecompass

import kotlin.math.cos

/** Public map opens only on the user's tap. URL numbers always use a dot, regardless of app units/locale. */
internal fun spaceCompassPositionMapUrl(latitude: Double?, longitude: Double?): String? {
    if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    val center = latitude.coerceIn(-85.04, 85.04)
    val span = .015 / cos(Math.toRadians(center)).coerceAtLeast(.1)
    val west = (longitude - span).coerceAtLeast(-180.0)
    val east = (longitude + span).coerceAtMost(180.0)
    val south = (center - .01).coerceAtLeast(-85.0511)
    val north = (center + .01).coerceAtMost(85.0511)
    return "https://www.openstreetmap.org/export/embed.html?bbox=$west,$south,$east,$north&layer=mapnik&marker=$latitude,$longitude"
}
