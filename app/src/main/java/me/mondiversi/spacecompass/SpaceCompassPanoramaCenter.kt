package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_PANORAMA_CENTER_KEY = "panorama_center"

/** Only four explicit choices; the initial hemisphere choice is derived from the capture location. */
internal enum class SpaceCompassPanoramaCenter(val key: String, val azimuth: Double, val labelResource: Int) {
    NORTH("north", 0.0, R.string.panorama_north),
    EAST("east", 90.0, R.string.panorama_east),
    SOUTH("south", 180.0, R.string.panorama_south),
    WEST("west", 270.0, R.string.panorama_west);

    companion object {
        fun fromStored(value: String?, latitude: Double?): SpaceCompassPanoramaCenter =
            entries.firstOrNull { it.key == value } ?: if (latitude != null && latitude.isFinite() && latitude < 0)
                NORTH else SOUTH
    }
}

/** Frozen cardinal references are rotated with every other geometric layer, never renamed in place. */
internal fun spaceCompassPanoramaGridIndex(index: Int, count: Int, center: SpaceCompassPanoramaCenter): Int {
    require(count > 0)
    val start = wrapSpaceCompassSunDegrees(center.azimuth - 180.0)
    return ((index + (start / 360 * count).toInt()) % count + count) % count
}
