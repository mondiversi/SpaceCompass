package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal data class SpaceCompassComparisonOverlay(val comparison: SpaceCompassTemporalComparison,
    val overlay: SpaceCompassCelestialOverlay)

internal val LocalSpaceCompassComparisonOverlays = staticCompositionLocalOf<List<SpaceCompassComparisonOverlay>> { emptyList() }
internal fun List<SpaceCompassComparisonOverlay>.activeSpaceCompassComparison(activeId: String?) =
    firstOrNull { it.comparison.id == activeId }?.comparison

internal val LocalSpaceCompassObservationZoneOverride = staticCompositionLocalOf<java.time.ZoneId?> { null }

internal fun spaceCompassComparisonTint(comparison: SpaceCompassTemporalComparison): Color {
    val base = spaceCompassCelestialPathTint(comparison.body)
    // A repeatable family of lighter/darker shades, independent of selection order and locale.
    val variant = (comparison.id.hashCode().toLong() and 0x7fffffff) % 997 / 996f
    return lerp(base, if (variant < .5f) Color.White else Color(0xff405060), .20f + .28f * variant)
}

internal fun spaceCompassComparisonName(comparison: SpaceCompassTemporalComparison, name: String,
    formatting: SpaceCompassPanoramaFormatting): String {
    val zone = TimeZone.getTimeZone(comparison.zone)
    val dateTime = comparison.personalDate?.atTime(12, 0)?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    val date = formatSpaceCompassDateOnly(dateTime ?: comparison.timeMs, formatting.date, formatting.deviceLocale,
        if (dateTime == null) zone else TimeZone.getTimeZone("UTC"), formatting.numeric,
        formatting.deviceLocale, allowHistorical = true)
    val title = comparison.personalName?.let { "$name · $it" } ?: name
    if (!comparison.hasReference) return "$title · $date"
    val time = formatSpaceCompassTimeOnly(comparison.timeMs, formatting.time, formatting.locale,
        zone, formatting.numeric, formatting.deviceLocale, includeSeconds = false, allowHistorical = true)
    val offset = formatSpaceCompassObserverUtcOffset(comparison.zone.rules.getOffset(Instant.ofEpochMilli(comparison.timeMs)))
    return "$title · $date · $time · $offset"
}

@Composable
internal fun spaceCompassComparisonDisplayName(comparison: SpaceCompassTemporalComparison): String {
    val units = LocalSpaceCompassUnits.current
    return spaceCompassComparisonName(comparison, androidx.compose.ui.res.stringResource(comparison.body.nameResource),
        SpaceCompassPanoramaFormatting(LocalConfiguration.current.locales[0], LocalSpaceCompassNumericFormat.current,
            LocalSpaceCompassDateFormat.current, LocalSpaceCompassTimeFormat.current, units.feet, units.dms,
            LocalSpaceCompassDeviceLocale.current))
}

/** Fixed instants do not subscribe to the live clock. Data validity is enforced by existing ephemeris functions. */
@Composable
internal fun rememberSpaceCompassComparisonOverlays(entries: List<SpaceCompassTemporalComparison>,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData): List<SpaceCompassComparisonOverlay> =
    entries.map { c -> key(c.id) {
        val result by produceState<SpaceCompassCelestialOverlay?>(null, c, latitude, longitude, altitude,
            remote.ephemerides[c.body], remote.satelliteOrbit(c.body)) {
            value = null
            if (latitude != null && longitude != null) value = withContext(Dispatchers.Default) {
                val job = coroutineContext
                val date = c.date
                if (date.atStartOfDay(c.zone).toLocalDate() != date) return@withContext null
                val observation = if (c.hasReference) runCatching {
                    calculateSpaceCompassCelestialObservation(c.body, c.timeMs, latitude, longitude, altitude, remote)
                }.getOrNull() else null
                job.ensureActive()
                val path = runCatching { calculateSpaceCompassCelestialPath(c.body, date, c.zone, c.timeMs,
                    latitude, longitude, altitude, remote)?.copy(comparison = c) }.getOrNull()
                SpaceCompassCelestialOverlay(c.body, observation, observation?.distanceKm,
                    runCatching { calculateSpaceCompassCelestialSpeed(c.body, c.timeMs, remote) }.getOrNull(), path, c.timeMs)
            }
        }
        SpaceCompassComparisonOverlay(c, result ?: SpaceCompassCelestialOverlay(c.body, null, null, null, null, c.timeMs))
    } }

internal fun projectSpaceCompassComparisonTargets(comparisons: List<SpaceCompassComparisonOverlay>,
    orientation: SpaceCompassSunOrientation?, width: Double, height: Double, perspective: SpaceCompassPerspective?):
    List<SpaceCompassCelestialSceneTarget> = comparisons.flatMap { (c, overlay) ->
    val current = overlay.observation?.position?.let { mapOf(c.body to SpaceCompassSunPathPoint(c.timeMs, it)) }.orEmpty()
    projectSpaceCompassCelestialSceneTargets(overlay.path?.let { mapOf(c.body to it) }.orEmpty(),
        current, orientation, width, height, perspective).map { it.copy(comparison = c) }
}
