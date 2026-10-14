package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Only the reticle target has a caption; selected points retain their marker highlights. */
@Composable
internal fun SpaceCompassCelestialSceneBadges(pinned: List<SpaceCompassCelestialSceneTarget>,
    focused: SpaceCompassCelestialSceneTarget?, nowMs: Long, primaryText: Color, secondaryText: Color,
    backgroundColor: Color, excluded: List<SpaceCompassSunSceneFrame>) {
    val selected = focused != null && pinned.any { it.body == focused.body && it.isCurrent == focused.isCurrent &&
        (it.isCurrent || it.point.timeMs == focused.point.timeMs && it.point.event == focused.point.event) }
    val items = listOfNotNull(focused)
    if (items.isEmpty()) return
    val accent = MaterialTheme.colorScheme.primary
    // Plain measured geometry avoids a second composition/measure cycle for connector positions.
    val connectors = remember { mutableListOf<Pair<Offset, Offset>>() }
    Layout(modifier = Modifier.drawBehind {
        connectors.forEach { (start, end) -> drawLine(accent.copy(alpha = .65f), start, end, .8.dp.toPx()) }
    }, content = {
        items.forEachIndexed { index, target -> key(target.body, target.point.timeMs, target.point.event, target.isCurrent) {
            val zone = target.path?.zone ?: spaceCompassObservationZone()
            val name = if (target.isCurrent) stringResource(R.string.celestial_point_current)
                else if (target.point.event == SpaceCompassSunPathEvent.HOUR) ""
                else target.path?.let { rememberSpaceCompassSunPathLabels(it).name(target.point) }.orEmpty()
            SpaceCompassCelestialBadgeContent(target.point.timeMs, nowMs, zone, target.point.position,
                primaryText, secondaryText, backgroundColor, name, pinned = selected)
        } }
    }) { measurables, constraints ->
        val margin = 4.dp.toPx().toDouble()
        val gap = 24.dp.toPx().toDouble()
        val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0,
            maxWidth = minOf(220.dp.roundToPx(), (constraints.maxWidth - margin * 2).toInt().coerceAtLeast(0)))) }
        val obstacles = excluded + (pinned + items).distinct().map { target ->
            val radius = (if (target.isCurrent) 20.dp else 10.dp).toPx().toDouble()
            SpaceCompassSunSceneFrame(target.projection.x - radius, target.projection.y - radius, radius * 2, radius * 2)
        }
        val occupied = mutableListOf<SpaceCompassSunSceneFrame>()
        val placements = children.mapIndexed { index, label ->
            placeSpaceCompassOrbitBadge(constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble(),
                label.width.toDouble(), label.height.toDouble(), items[index].projection, margin, gap,
                obstacles, occupied, selected)?.also { point ->
                occupied += SpaceCompassSunSceneFrame(point.x - margin, point.y - margin,
                    label.width + margin * 2, label.height + margin * 2)
            }
        }
        connectors.clear()
        placements.forEachIndexed { index, point -> if (selected && point != null) {
            val anchor = items[index].projection
            val label = children[index]
            connectors += Offset(anchor.x.toFloat(), anchor.y.toFloat()) to Offset(
                anchor.x.coerceIn(point.x, point.x + label.width).toFloat(),
                anchor.y.coerceIn(point.y, point.y + label.height).toFloat())
        } }
        layout(constraints.maxWidth, constraints.maxHeight) {
            children.forEachIndexed { index, label -> placements[index]?.let { label.place(it.x.roundToInt(), it.y.roundToInt()) } }
        }
    }
}
