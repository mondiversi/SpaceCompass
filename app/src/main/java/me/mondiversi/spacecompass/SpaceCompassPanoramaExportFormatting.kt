package me.mondiversi.spacecompass

import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

internal const val SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY = "panorama_export_mode"

internal enum class SpaceCompassPanoramaExportMode(val key: String) {
    SELECTED("selected"), INTERNATIONAL("international");

    companion object {
        fun fromStored(value: String?): SpaceCompassPanoramaExportMode =
            entries.firstOrNull { it.key == value } ?: INTERNATIONAL
    }
}

/** Frozen presentation only. All geometry and measurements remain in their canonical units. */
internal data class SpaceCompassPanoramaFormatting(
    val locale: Locale = Locale.ENGLISH,
    val numeric: SpaceCompassNumericFormat = SpaceCompassNumericFormat.INTERNATIONAL,
    val date: SpaceCompassDateFormat = SpaceCompassDateFormat.INTERNATIONAL,
    val time: SpaceCompassTimeFormat = SpaceCompassTimeFormat.H24,
    val feet: Boolean = false,
    val dms: Boolean = false,
    val deviceLocale: Locale = locale
)

internal val spaceCompassPanoramaInternationalFormatting = SpaceCompassPanoramaFormatting()
internal val spaceCompassPanoramaExportLocale: Locale = Locale.ENGLISH
internal val spaceCompassPanoramaExportNumeric = SpaceCompassNumericFormat.INTERNATIONAL

internal fun formatSpaceCompassPanoramaExportTime(timeMs: Long, zone: TimeZone,
    formatting: SpaceCompassPanoramaFormatting = spaceCompassPanoramaInternationalFormatting): String =
    formatSpaceCompassTimeOnly(timeMs, formatting.time, formatting.locale,
        zone, formatting.numeric, formatting.deviceLocale, includeSeconds = false)

/** Keep the observation zone explicit in both profiles, including fractional and seasonal offsets. */
internal fun formatSpaceCompassPanoramaExportTimestamp(timeMs: Long, zone: TimeZone,
    formatting: SpaceCompassPanoramaFormatting = spaceCompassPanoramaInternationalFormatting): String {
    val date = formatSpaceCompassDateOnly(timeMs, formatting.date,
        formatting.deviceLocale, zone, formatting.numeric, formatting.deviceLocale)
    val time = formatSpaceCompassPanoramaExportTime(timeMs, zone, formatting)
    val offsetMinutes = zone.getOffset(timeMs) / 60_000
    val offset = String.format(Locale.ROOT, "%s%02d:%02d", if (offsetMinutes < 0) "-" else "+",
        abs(offsetMinutes) / 60, abs(offsetMinutes) % 60)
    return "Space Compass · $date · $time (UTC$offset)"
}

/** Format raw measurements directly; never reconvert strings from the other profile. */
internal fun spaceCompassPanoramaExportRows(latitude: Double?, longitude: Double?, altitudeMeters: Double?,
    accuracyMeters: Double?, altitudeTemplate: String, weatherText: String,
    formatting: SpaceCompassPanoramaFormatting = spaceCompassPanoramaInternationalFormatting): List<SpaceCompassSunDataRow> = listOf(
    SpaceCompassSunDataRow("", formatSpaceCompassSelectedCoordinates(latitude, longitude,
        formatting.numeric, formatting.dms, formatting.deviceLocale) ?: "—", "", "sun-info-coordinates"),
    SpaceCompassSunDataRow("", formatSpaceCompassPhysicalLength(accuracyMeters, 0,
        formatting.numeric, formatting.feet, systemLocale = formatting.deviceLocale), "", "sun-info-accuracy"),
    spaceCompassSunDataRow(altitudeTemplate, formatSpaceCompassCelestialAltitude(altitudeMeters, null,
        formatting.numeric, formatting.feet, formatting.deviceLocale) ?: "—", "sun-info-altitude"),
    SpaceCompassSunDataRow("", weatherText, "", "celestial-environment-weather")
)

internal fun formatSpaceCompassPanoramaExportCloudCover(cover: Float?,
    formatting: SpaceCompassPanoramaFormatting = spaceCompassPanoramaInternationalFormatting): String? =
    cover?.takeIf { it.isFinite() && it in 0f..1f }?.let {
        "${formatSpaceCompassNumber(it * 100.0, 0, formatting.numeric, systemLocale = formatting.deviceLocale)}%"
    }

internal fun formatSpaceCompassPanoramaElevation(degrees: Double, formatting: SpaceCompassPanoramaFormatting): String =
    formatSpaceCompassNumber(if (abs(degrees) < .05) 0.0 else degrees, 1, formatting.numeric,
        grouping = false, systemLocale = formatting.deviceLocale)

/** Each option tuple has a separate file and gallery identity, even when text happens to match. */
internal fun spaceCompassPanoramaVariantKey(position: SpaceCompassPanoramaPosition, showLabels: Boolean,
    mode: SpaceCompassPanoramaExportMode, center: SpaceCompassPanoramaCenter = SpaceCompassPanoramaCenter.SOUTH): String =
    "${mode.key}-${position.key}-${if (showLabels) "labels" else "no-labels"}-${center.key}"
