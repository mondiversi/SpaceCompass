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
    // Context controls occupy the upper trailing edge in compact landscape; Menu sits below the logo.
    // Require a measured header before reclassifying controls during an incomplete layout pass.
    val headerMeasured = controls.any { it.feature == SpaceCompassTutorialFeature.MENU }
    val upperSlot = controls.filter { it.feature in setOf(SpaceCompassTutorialFeature.WEATHER,
        SpaceCompassTutorialFeature.SKY_LABELS, SpaceCompassTutorialFeature.GLOBE_LABELS,
        SpaceCompassTutorialFeature.AXIS, SpaceCompassTutorialFeature.GLOBE_ROTATION,
        SpaceCompassTutorialFeature.ANTIPODES, SpaceCompassTutorialFeature.TRANSPARENCY) &&
        headerMeasured && it.bounds.center.y < viewport.center.y }.toSet()
    val upper = controls.filter { it in upperSlot || it.feature in setOf(SpaceCompassTutorialFeature.INFO,
        SpaceCompassTutorialFeature.MENU, SpaceCompassTutorialFeature.FORWARD, SpaceCompassTutorialFeature.GUIDES,
        SpaceCompassTutorialFeature.LOCATION, SpaceCompassTutorialFeature.OBJECTS) }
    val lower = controls.filter { it !in upperSlot && it.feature in setOf(SpaceCompassTutorialFeature.PANELS,
        SpaceCompassTutorialFeature.ANTIPODES, SpaceCompassTutorialFeature.CAMERA, SpaceCompassTutorialFeature.CAPTURE,
        SpaceCompassTutorialFeature.WEATHER, SpaceCompassTutorialFeature.AXIS, SpaceCompassTutorialFeature.SKY_LABELS,
        SpaceCompassTutorialFeature.TRANSPARENCY, SpaceCompassTutorialFeature.GLOBE_LABELS,
        SpaceCompassTutorialFeature.GLOBE_ROTATION, SpaceCompassTutorialFeature.ZOOM) }
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
    val circular: Boolean = true, val point: Boolean = false, val visualBounds: Rect? = null,
    val title: Int = feature.title) {
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

/** Find a real blank circle without covering controls, orbit touch targets or the explanation. */
internal fun spaceCompassTutorialFreeArea(viewport: Rect, blocked: List<Rect>, preferredRadius: Float): Rect? {
    if (preferredRadius <= 0 || viewport.width <= 0 || viewport.height <= 0) return null
    for (scale in listOf(1f, .83f, .67f)) {
        val radius = preferredRadius * scale
        val left = viewport.left + radius + 4f
        val top = viewport.top + radius + 4f
        val right = viewport.right - radius - 4f
        val bottom = viewport.bottom - radius - 4f
        if (right < left || bottom < top) continue
        val columns = ((right - left) / radius).toInt().coerceAtLeast(1)
        val rows = ((bottom - top) / radius).toInt().coerceAtLeast(1)
        val candidates = (0..rows).flatMap { y -> (0..columns).map { x ->
            val center = Offset(left + (right-left)*x/columns, top + (bottom-top)*y/rows)
            Rect(center.x-radius, center.y-radius, center.x+radius, center.y+radius)
        } }
        candidates.filter { candidate -> blocked.none(candidate::overlaps) }
            .minByOrNull { (it.center - viewport.center).getDistance() }?.let { return it }
    }
    return null
}

/** One reserved anchor below the top icon row, independent of island visibility and orbit points. */
internal fun spaceCompassTutorialFixedFreeArea(viewport: Rect, radius: Float, topInset: Float): Rect {
    val r = minOf(radius, (viewport.width / 2 - 4f).coerceAtLeast(1f),
        (viewport.height / 4).coerceAtLeast(1f))
    val top = (viewport.top + topInset).coerceIn(viewport.top,
        (viewport.bottom - r * 2).coerceAtLeast(viewport.top))
    return Rect(viewport.center.x-r, top, viewport.center.x+r, top+r*2)
}

internal data class SpaceCompassTutorialExplanationLayout(val area: Rect, val height: Float)

/** Reserve the circle first; short landscape windows can place readable explanations beside it. */
internal fun spaceCompassTutorialExplanationLayout(sky: Rect, targets: List<SpaceCompassTutorialTarget>,
    circle: Rect, desiredWidth: Float, maximumHeight: Float, gap: Float,
    minimumSideWidth: Float): SpaceCompassTutorialExplanationLayout {
    val controls = targets.filter { it.circular && !it.point && it.feature !in setOf(
        SpaceCompassTutorialFeature.REMINDER, SpaceCompassTutorialFeature.OBSERVATORY,
        SpaceCompassTutorialFeature.DAILY) }.map { it.bounds }
    val band = spaceCompassTutorialCardBand(sky, targets)
    val below = Rect(band.left, maxOf(band.top, circle.bottom+gap).coerceAtMost(band.bottom), band.right, band.bottom)
    fun layout(area: Rect): SpaceCompassTutorialExplanationLayout {
        val width = minOf(desiredWidth, (area.width-gap*2).coerceAtLeast(1f))
        return SpaceCompassTutorialExplanationLayout(area, spaceCompassTutorialCardClearHeight(area, width,
            controls+circle, gap, maximumHeight))
    }
    val lower = layout(below)
    if (lower.height >= maximumHeight * .5f) return lower
    val edgeBand = circle.width
    val left = maxOf(sky.left, (controls.filter { it.center.x < sky.left+edgeBand }
        .maxOfOrNull { it.right+gap } ?: sky.left))
    val right = minOf(sky.right, (controls.filter { it.center.x > sky.right-edgeBand }
        .minOfOrNull { it.left-gap } ?: sky.right))
    val sideAreas = listOf(
        Rect(left,sky.top,maxOf(left,minOf(right,circle.left-gap)),sky.bottom),
        Rect(minOf(right,maxOf(left,circle.right+gap)),sky.top,right,sky.bottom))
    val sides = sideAreas.filter { it.width-gap*2 >= minimumSideWidth }.map(::layout)
    return (sides+lower).filter { it.height > 0 }.maxByOrNull {
        minOf(desiredWidth,it.area.width-gap*2)*it.height
    } ?: lower.copy(height = (below.height-gap*2).coerceAtLeast(1f))
}
