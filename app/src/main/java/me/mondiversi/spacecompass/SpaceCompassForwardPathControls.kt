package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

@Composable
internal fun SpaceCompassForwardPathButton(forward24Hours: Boolean, onIntervalChange: (Boolean) -> Unit,
    color: Color, background: Color) {
    // This is a choice of interval, not an enabled/disabled state.
    SpaceCompassFloatingActionButton(stringResource(R.string.path_interval_title), { onIntervalChange(!forward24Hours) },
        color, background,
        Modifier.testTag("toggle-forward-path").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.FORWARD),
        stateText = stringResource(if (forward24Hours) R.string.scene_forward_path_on else R.string.scene_forward_path_off)) {
        Text(if (forward24Hours) "+24h" else "0–24", Modifier.padding(2.dp), color = color,
            fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, softWrap = false, textAlign = TextAlign.Center,
            style = TextStyle(textDirection = TextDirection.Ltr))
    }
}

/** Pair analysis uses the stable buffers; only this inexpensive trim follows the scenario clock. */
@Composable
internal fun rememberSpaceCompassForwardOverlays(overlays: Map<SpaceCompassCelestialBody,SpaceCompassCelestialOverlay>,
    enabled: Boolean,timeMs: Long,latitude: Double?,longitude: Double?,altitude: Double,
    remote: SpaceCompassCelestialRemoteData): Map<SpaceCompassCelestialBody,SpaceCompassCelestialOverlay> {
    if (!enabled) return overlays
    val paths=overlays.mapValues { it.value.path }
    val trimmed by produceState<Map<SpaceCompassCelestialBody,SpaceCompassSunDailyPath?>>(emptyMap(),
        paths,timeMs,latitude,longitude,altitude,remote.ephemerides,remote.satellites,remote.iss,remote.starlink) {
        value=if(latitude==null || longitude==null) emptyMap() else withContext(Dispatchers.Default) {
            paths.mapValues { (body,path) ->
                coroutineContext.ensureActive()
                path?.let { trimSpaceCompassForwardPath(it,timeMs) { t ->
                    calculateSpaceCompassCelestialObservation(body,t,latitude,longitude,altitude,remote)?.position
                } }
            }
        }
    }
    return overlays.mapValues { (body,overlay) -> overlay.copy(path=trimmed[body]?.takeIf {
        it.zone == overlay.path?.zone && it.forward24Hours
    }) }
}
