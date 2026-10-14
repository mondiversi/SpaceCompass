package me.mondiversi.spacecompass

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal const val SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP = 32f

/** Same cached catalogue preview, with the live lunar phase; not a live photograph or spacecraft attitude solution.
 * Pointer and TalkBack actions only toggle the live point; the inspected object stays unchanged.
 */
@Composable
internal fun SpaceCompassCelestialLiveMarker(body: SpaceCompassCelestialBody, projection: SpaceCompassSunProjection,
    belowHorizon: Boolean, point: SpaceCompassSunPathPoint?, state: SpaceCompassSunDailyPathUiState,
    moonPhase: SpaceCompassMoonPhase? = null,
    path: SpaceCompassSunDailyPath? = null, interactive: Boolean = true, focused: Boolean = false) {
    val name = stringResource(body.nameResource)
    val timeFormat = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val locale = LocalConfiguration.current.locales[0]
    val moment = point?.let { formatSpaceCompassCelestialMoment(it.timeMs, it.timeMs, spaceCompassObservationZone(),
        timeFormat, LocalSpaceCompassDateFormat.current, locale, LocalSpaceCompassDeviceLocale.current,
        LocalSpaceCompassNumericFormat.current) }
    val zone = path?.zone ?: spaceCompassObservationZone()
    val selected = point?.let { state.isCurrentSelected(body, it, zone) } == true
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val current = stringResource(R.string.celestial_current_position)
    val label = moment?.let { "$name, ${stringResource(R.string.celestial_point_time, current, it)}" } ?: name
    Layout(modifier = Modifier.fillMaxSize(), content = {
        Box(Modifier.size((SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP * 2).dp)
            .then(if (interactive) Modifier.testTag("celestial-current-point") else Modifier)
            .then(if (point == null || !interactive) Modifier else Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null) {
                state.toggleCurrent(body, point, path, zone)
            })
            .semantics(mergeDescendants = true) {
                if (interactive) { contentDescription = label; this.selected = selected }
                if (point != null && interactive) {
                    role = Role.Button
                    onClick { state.toggleCurrent(body, point, path, zone); true }
                }
            }, contentAlignment = Alignment.Center) {
            SpaceCompassCelestialThumbnail(body, Modifier.size(SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP.dp)
                .border(if (selected || focused) 1.8.dp else 0.8.dp,
                    when {
                        selected -> accent
                        focused -> Color.White
                        else -> (if (belowHorizon) Color.LightGray else Color.White).copy(alpha = 0.65f)
                    }, CircleShape)
                .testTag(if (interactive) "celestial-live-preview-${body.name}" else "celestial-background-preview-${body.name}"),
                muted = belowHorizon, moonPhase = moonPhase)
        }
    }) { measurables, constraints ->
        val targetSize = (SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP * 2).dp.roundToPx()
        val marker = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0,
            maxWidth = minOf(targetSize, constraints.maxWidth), maxHeight = minOf(targetSize, constraints.maxHeight)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Absolute placement: sky projection coordinates must never mirror in RTL locales.
            marker.place((projection.x * density).roundToInt() - marker.width / 2,
                (projection.y * density).roundToInt() - marker.height / 2)
        }
    }
}
