package me.mondiversi.spacecompass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.math.round
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/** One selected object/year is evaluated off the UI thread, independently of sensor/live ticks. */
@Composable
internal fun SpaceCompassObservationMomentsRows(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    secondaryText: Color) {
    val context = LocalContext.current
    val observer = LocalSpaceCompassObserver.current
    val zone = spaceCompassObservationZone()
    val year = Instant.ofEpochMilli(timeMs).atZone(zone).year
    // Same ~11 m / 10 m cells as the cached sky paths prevent GPS jitter cancelling annual work.
    val latitudeKey = latitude?.let { round(it * 10_000) / 10_000 }
    val longitudeKey = longitude?.let { round(it * 10_000) / 10_000 }
    val altitudeKey = round(altitude / 10) * 10
    val orbit = remote.satelliteOrbit(body)
    // key() prevents a previous place/year's links remaining clickable while the annual result loads.
    val result = key(body, year, zone, latitudeKey, longitudeKey, altitudeKey, orbit, remote.ephemerides[body]) {
        produceState<Pair<Boolean, List<SpaceCompassObservationMoment>?>>(false to null) {
            val moments = if (latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                val job = coroutineContext
                try { calculateSpaceCompassObservationMoments(body, timeMs, zone,
                    latitude, longitude, altitude, remote) { job.ensureActive() } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    SpaceCompassErrorLog.record(context, "observatory:annual_moments", error); null
                }
            }
            value = true to moments
        }.value
    }
    val formatting = rememberSpaceCompassDataExportProfiles().selected.formatting
    val timeZone = TimeZone.getTimeZone(zone)
    val appliedMessage = stringResource(R.string.observation_moment_applied)
    Text(stringResource(R.string.observation_moments_year), color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
    if (result.first && result.second == null) Text(stringResource(if (latitude == null || longitude == null)
        R.string.celestial_view_location_needed else R.string.observation_moments_unavailable),
        color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
    val placeholders = SpaceCompassObservationMomentKind.entries.map { it to null }
    val rows = result.second?.map { it.kind to it.point } ?: placeholders
    for ((index, entry) in rows.withIndex()) {
        val (kind, point) = entry
        val label = stringResource(when (kind) {
            SpaceCompassObservationMomentKind.MAXIMUM -> R.string.observation_height_maximum
            SpaceCompassObservationMomentKind.MINIMUM -> R.string.observation_height_minimum
            SpaceCompassObservationMomentKind.MID_RISING -> R.string.observation_height_mid_rising
            SpaceCompassObservationMomentKind.MID_SETTING -> R.string.observation_height_mid_setting
        })
        val enabled = point != null && observer != null &&
            Instant.ofEpochMilli(point.timeMs).atZone(zone).year in 1900..2100
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label, Modifier.weight(.52f), color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
            Column(Modifier.weight(.48f).defaultMinSize(minHeight = 48.dp)
                .testTag("observation-moment-${kind.name.lowercase()}-$index")
                .clickable(enabled = enabled, onClickLabel = label, role = Role.Button) {
                    if (point != null && observer?.observeAt(point.timeMs, ZoneId.systemDefault()) == true)
                        showSpaceCompassBottomMessage(context, appliedMessage)
                }, verticalArrangement = Arrangement.Center) {
                val dateText = point?.let { formatSpaceCompassDateOnly(it.timeMs, formatting.date,
                    formatting.deviceLocale, timeZone, formatting.numeric, formatting.deviceLocale, allowHistorical = true) }
                val clockText = point?.let { formatSpaceCompassTimeOnly(it.timeMs, formatting.time,
                    formatting.locale, timeZone, formatting.numeric, formatting.deviceLocale, includeSeconds = false,
                    allowHistorical = true) }
                val offset = point?.let { formatSpaceCompassObserverUtcOffset(zone.rules.getOffset(Instant.ofEpochMilli(it.timeMs))) }
                Text(if (point == null) { if (!result.first) stringResource(R.string.pc_loading) else "—" }
                    else "$dateText\n$clockText · $offset", color = if (enabled) MaterialTheme.colorScheme.primary else secondaryText,
                    fontSize = 12.sp, lineHeight = 16.sp, style = TextStyle(textDirection = TextDirection.ContentOrLtr))
            }
        }
    }
    if (result.second != null) Text(stringResource(R.string.observation_moments_midpoint_note),
        color = secondaryText, fontSize = 10.sp, lineHeight = 13.sp)
}
