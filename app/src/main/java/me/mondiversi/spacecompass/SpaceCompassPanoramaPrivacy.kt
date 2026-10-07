package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_PANORAMA_POSITION_KEY = "panorama_position"
/** Compatibility key: the switch controls time/elevation, object and celestial-guide annotations. */
internal const val SPACE_COMPASS_PANORAMA_POINT_LABELS_KEY = "panorama_point_labels"

internal enum class SpaceCompassPanoramaPosition(val key: String) {
    COMPLETE("complete"), AREA("area"), HIDDEN("hidden");

    companion object {
        fun fromStored(value: String?): SpaceCompassPanoramaPosition =
            if (value == null) AREA else entries.firstOrNull { it.key == value } ?: HIDDEN
    }
}

/** Frozen at capture time; changing disclosure never consults live GPS, units or the clock. */
internal data class SpaceCompassPanoramaCaptionData(val timestamp: String, val completePlace: String?,
    val areaPlace: String?, val rows: List<SpaceCompassSunDataRow>, val cloudPercent: String?)

internal fun spaceCompassPanoramaCaptionForPosition(data: SpaceCompassPanoramaCaptionData,
    position: SpaceCompassPanoramaPosition): String {
    val place = when (position) {
        SpaceCompassPanoramaPosition.COMPLETE -> data.completePlace
        SpaceCompassPanoramaPosition.AREA -> data.areaPlace
        SpaceCompassPanoramaPosition.HIDDEN -> null
    }
    val rows = if (position == SpaceCompassPanoramaPosition.COMPLETE) data.rows
        else data.rows.filter { it.tag == "celestial-environment-weather" }
    return formatSpaceCompassPanoramaCaption(data.timestamp, place, rows, data.cloudPercent)
}

/** Coarse disclosure never falls back to a city, district or a parsed full address. */
internal fun formatSpaceCompassPanoramaArea(parts: SpaceCompassPlaceParts?): String? =
    parts?.let { formatSpaceCompassEstimatedPlace(SpaceCompassPlaceParts(adminArea = it.adminArea, country = it.country)) }
