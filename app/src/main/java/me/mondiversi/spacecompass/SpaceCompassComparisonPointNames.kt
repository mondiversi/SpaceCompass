package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlin.math.abs

/** Names describe the source event, without a second generic Reference/Current heading. */
internal fun spaceCompassComparisonPointNameResource(c: SpaceCompassTemporalComparison): Int? {
    val kind = c.origin.substringBefore(':')
    if (c.source == SpaceCompassComparisonSource.EVENT)
        return SpaceCompassKeyEventKind.entries.firstOrNull { it.name == kind }?.title
    // Legacy extrema do not retain annual/cycle title metadata; their known physical event is an upper culmination.
    return when (kind) {
        "MONTHLY" -> R.string.sun_path_culmination
        "MAXIMUM" -> if (c.annualExtreme) R.string.observation_height_maximum else R.string.sun_path_culmination
        "MINIMUM" -> if (c.annualExtreme) R.string.observation_height_minimum else R.string.sun_path_culmination
        "MID_RISING" -> R.string.observation_height_mid_rising_single
        "MID_SETTING" -> R.string.observation_height_mid_setting_single
        else -> null
    }
}

@Composable
internal fun spaceCompassComparisonPointName(c: SpaceCompassTemporalComparison): String =
    c.personalName ?: stringResource(spaceCompassComparisonPointNameResource(c) ?: R.string.comparison_reference_point)

internal fun SpaceCompassPanoramaSnapshot.comparisonPointName(c: SpaceCompassTemporalComparison): String =
    c.personalName ?: comparisonPointNames[c.id] ?: if (c.source == SpaceCompassComparisonSource.HEIGHT && c.origin.startsWith("MONTHLY:"))
        pointEventNames[SpaceCompassSunPathEvent.CULMINATION] ?: referencePointName else referencePointName

/** Coalesce captions only for the same instance, close in time AND direction, never mere curve crossings. */
internal fun spaceCompassComparisonAnchorMatches(c: SpaceCompassTemporalComparison, position: SpaceCompassSunPosition,
    point: SpaceCompassSunPathPoint): Boolean = abs(point.timeMs - c.timeMs) <= 120_000L &&
    spaceCompassAngularSeparation(position, point.position) < .05

internal fun SpaceCompassPanoramaSnapshot.hasComparisonAnchorLabel(item: SpaceCompassPanoramaObject,
    point: SpaceCompassSunPathPoint, position: SpaceCompassSunPosition?, selections: List<SpaceCompassPanoramaSelectedPoint>): Boolean {
    val c = item.comparison ?: return false
    if (position == null || !spaceCompassComparisonAnchorMatches(c, position, point)) return false
    return labelMode.showsCurrentPoint(isActive(item)) || labelMode.showsSelected && selections.any {
        !it.isAntipode && it.body == item.body && it.comparison?.id == c.id && it.isCurrent
    }
}

internal fun SpaceCompassPanoramaSnapshot.comparisonAnchorSelected(item: SpaceCompassPanoramaObject,
    position: SpaceCompassSunPosition, selections: List<SpaceCompassPanoramaSelectedPoint>): Boolean = selections.any {
    !it.isAntipode && it.body == item.body && it.comparison?.id == item.comparison?.id &&
        (it.isCurrent || item.comparison?.let { c -> spaceCompassComparisonAnchorMatches(c, position, it.point) } == true)
}

/** Only captions coalesce; selection records and their exported values remain independent. */
internal fun spaceCompassUniqueComparisonCaptionPins(selected: List<SpaceCompassPanoramaSelectedPoint>):
    List<SpaceCompassPanoramaSelectedPoint> = selected.filter { pin -> pin.isCurrent || pin.comparison == null ||
        selected.none { current -> current.isCurrent && current.isAntipode == pin.isAntipode &&
            current.body == pin.body && current.comparison?.id == pin.comparison.id &&
            spaceCompassComparisonAnchorMatches(pin.comparison, current.point.position, pin.point) }
    }
