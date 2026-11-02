package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Projected orbit dots use the same coordinates and exclusions as normal scene interaction. */
@Composable
internal fun SpaceCompassTutorialScene(targets: List<SpaceCompassCelestialSceneTarget>,
    excluded: List<SpaceCompassSunSceneFrame>) {
    val tutorial = LocalSpaceCompassTutorial.current
    if (tutorial?.active != true) return
    val id = remember { Any() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(Size.Zero) }
    val radius = with(LocalDensity.current) { 7.dp.toPx() }
    val visible = targets.filter { target ->
        val p = SpaceCompassSunScenePoint(target.projection.x, target.projection.y)
        p.x >= radius && p.y >= radius && p.x < size.width - radius && p.y < size.height - radius &&
            !spaceCompassScenePointIsCovered(p, excluded)
    }.take(96)
    DisposableEffect(tutorial, id) {
        onDispose { tutorial.targets.keys.filter { it is Pair<*, *> && it.first === id }.toList().forEach(tutorial.targets::remove) }
    }
    SideEffect {
        val keys = visible.indices.map { id to it }.toSet()
        tutorial.targets.keys.filter { it is Pair<*, *> && it.first === id && it !in keys }.toList().forEach(tutorial.targets::remove)
        visible.forEachIndexed { index, target ->
            val p = origin + Offset(target.projection.x.toFloat(), target.projection.y.toFloat())
            tutorial.targets[id to index] = SpaceCompassTutorialTarget(if (target.isAntipode) SpaceCompassTutorialFeature.ANTIPODES
                else SpaceCompassTutorialFeature.POINT,
                Rect(p.x - radius, p.y - radius, p.x + radius, p.y + radius), point = true)
        }
    }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft
        size = Size(it.size.width.toFloat(), it.size.height.toFloat()) })
}
