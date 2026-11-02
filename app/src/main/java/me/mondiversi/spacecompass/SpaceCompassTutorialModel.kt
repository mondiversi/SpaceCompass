package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** A tutorial camera preview must never open a real camera, even if it was active before entry. */
internal fun spaceCompassTutorialCameraEnabled(requested: Boolean, tutorialActive: Boolean): Boolean =
    requested && !tutorialActive

/** A little more reading room; leave space for sky controls wherever the window permits it. */
internal fun spaceCompassTutorialCardHeightLimit(viewportHeightDp: Float): Float =
    minOf(240f, maxOf(80f, viewportHeightDp - 112f), (viewportHeightDp - 24f).coerceAtLeast(0f))

/** Center the explanation in the actual gap between control groups, not the whole sky. */
internal fun spaceCompassTutorialCardBand(viewport: Rect, targets: List<SpaceCompassTutorialTarget>): Rect {
    val controls = targets.filter { it.circular && !it.point && it.bounds.right > viewport.left && it.bounds.left < viewport.right }
    val upper = controls.filter { it.feature in setOf(SpaceCompassTutorialFeature.INFO,
        SpaceCompassTutorialFeature.MENU, SpaceCompassTutorialFeature.FORWARD, SpaceCompassTutorialFeature.GUIDES,
        SpaceCompassTutorialFeature.LOCATION, SpaceCompassTutorialFeature.OBJECTS) }
    val lower = controls.filter { it.feature in setOf(SpaceCompassTutorialFeature.PANELS,
        SpaceCompassTutorialFeature.ANTIPODES, SpaceCompassTutorialFeature.CAMERA, SpaceCompassTutorialFeature.CAPTURE,
        SpaceCompassTutorialFeature.WEATHER, SpaceCompassTutorialFeature.AXIS,
        SpaceCompassTutorialFeature.TRANSPARENCY, SpaceCompassTutorialFeature.ZOOM) }
    val top = upper.maxOfOrNull { it.bounds.bottom } ?: viewport.top
    val bottom = lower.minOfOrNull { it.bounds.top } ?: viewport.bottom
    return if (bottom > top) Rect(viewport.left, top, viewport.right, bottom) else viewport
}

/** Grow around the fixed control-gap center without covering a button or its safety gap. */
internal fun spaceCompassTutorialCardClearHeight(area: Rect, width: Float, controls: List<Rect>,
    gap: Float, maximumHeight: Float): Float {
    val left = area.center.x - width / 2
    val right = area.center.x + width / 2
    var halfHeight = minOf(maximumHeight, (area.height - gap * 2).coerceAtLeast(0f)) / 2
    controls.filter { it.right > left && it.left < right }.forEach { control ->
        val clearance = when {
            control.bottom <= area.center.y -> area.center.y - control.bottom - gap
            control.top >= area.center.y -> control.top - area.center.y - gap
            else -> 0f
        }
        halfHeight = minOf(halfHeight, clearance.coerceAtLeast(0f))
    }
    return halfHeight * 2
}

/** One intermediate anchor, independent of the selected control or the text's natural height. */
internal fun spaceCompassTutorialCardPosition(area: Rect, card: Size, margin: Float): Offset {
    val minX = area.left + margin
    val minY = area.top + margin
    val maxX = (area.right - card.width - margin).coerceAtLeast(minX)
    val maxY = (area.bottom - card.height - margin).coerceAtLeast(minY)
    return Offset((area.center.x - card.width / 2).coerceIn(minX, maxX),
        (area.center.y - card.height / 2).coerceIn(minY, maxY))
}

internal data class SpaceCompassTutorialTarget(val feature: SpaceCompassTutorialFeature, val bounds: Rect,
    val circular: Boolean = true, val point: Boolean = false, val visualBounds: Rect? = null) {
    val highlightBounds: Rect get() = visualBounds ?: bounds
    fun translated(offset: Offset): SpaceCompassTutorialTarget = copy(bounds = bounds.translate(offset),
        visualBounds = visualBounds?.translate(offset))
}

internal fun spaceCompassTutorialHit(targets: List<SpaceCompassTutorialTarget>, position: Offset,
    pointRadius: Float): SpaceCompassTutorialTarget? = targets
    .filter { if (it.point) (position - it.bounds.center).getDistance() <= pointRadius else it.bounds.contains(position) }
    // Fixed controls take precedence over nearby orbit dots; smaller nested links win.
    .minWithOrNull(compareBy<SpaceCompassTutorialTarget> { it.point }
        .thenBy { if (it.point) (position - it.bounds.center).getDistance() else it.bounds.width * it.bounds.height })
