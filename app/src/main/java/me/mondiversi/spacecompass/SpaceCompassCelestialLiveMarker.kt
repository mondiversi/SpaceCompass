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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import kotlin.math.roundToInt

internal const val SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP = 32f

/** Same cached catalogue preview, with the live lunar phase; not a live photograph or spacecraft attitude solution.
 * Pointer hits are resolved with the path dots; this node also supplies a TalkBack action.
 */
@Composable
internal fun SpaceCompassCelestialLiveMarker(body: SpaceCompassCelestialBody, projection: SpaceCompassSunProjection,
    belowHorizon: Boolean, point: SpaceCompassSunPathPoint?, state: SpaceCompassSunDailyPathUiState,
    onActivate: (() -> Unit)? = null, moonPhase: SpaceCompassMoonPhase? = null) {
    val name = stringResource(body.nameResource)
    val timeFormat = resolveSpaceCompassTimeFormat(LocalContext.current, LocalSpaceCompassTimeFormat.current)
    val locale = LocalConfiguration.current.locales[0]
    val moment = point?.let { formatSpaceCompassCelestialMoment(it.timeMs, it.timeMs, spaceCompassObservationZone(),
        timeFormat, LocalSpaceCompassDateFormat.current, locale, LocalSpaceCompassDeviceLocale.current,
        LocalSpaceCompassNumericFormat.current) }
    val current = stringResource(R.string.celestial_current_position)
    val label = moment?.let { "$name, ${stringResource(R.string.celestial_point_time, current, it)}" } ?: name
    Layout(modifier = Modifier.fillMaxSize(), content = {
        Box(Modifier.size((SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP * 2).dp).testTag("celestial-current-point")
            .then(if (onActivate == null) Modifier else Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onActivate(); point?.let { state.selectCurrent(body, it) }
            })
            .semantics(mergeDescendants = true) {
                contentDescription = label
                if (point != null) {
                    role = Role.Button
                    onClick { onActivate?.invoke(); state.selectCurrent(body, point); true }
                }
            }, contentAlignment = Alignment.Center) {
            SpaceCompassCelestialThumbnail(body, Modifier.size(SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP.dp)
                .border(0.8.dp, (if (belowHorizon) Color.LightGray else Color.White).copy(alpha = 0.65f), CircleShape)
                .testTag("celestial-live-preview-${body.name}"), muted = belowHorizon, moonPhase = moonPhase)
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
