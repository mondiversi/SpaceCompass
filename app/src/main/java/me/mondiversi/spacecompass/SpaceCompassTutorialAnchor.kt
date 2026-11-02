package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

/** Pair a generous touch region with the actual visible text, glyph or circle. */
@Stable
internal class SpaceCompassTutorialAnchor(private val tutorial: SpaceCompassTutorialState,
    private val feature: SpaceCompassTutorialFeature, private val circular: Boolean) {
    private var touch: Rect? = null
    private var visual: Rect? = null
    val touchModifier = Modifier.onGloballyPositioned { touch = it.boundsInRoot(); publish() }
    val visualModifier = Modifier.onGloballyPositioned { visual = it.boundsInRoot(); publish() }

    private fun publish() {
        val hit = touch ?: return
        val ink = visual ?: return
        if (tutorial.active && hit.width > 0 && hit.height > 0 && ink.width > 0 && ink.height > 0)
            tutorial.targets[this] = SpaceCompassTutorialTarget(feature, hit, circular, visualBounds = ink)
    }

    fun dispose() { tutorial.targets.remove(this) }
}

/** No layout callbacks or anchor allocations outside the tutorial. */
@Composable
internal fun rememberSpaceCompassTutorialAnchor(feature: SpaceCompassTutorialFeature,
    enabled: Boolean = true, circular: Boolean = false): SpaceCompassTutorialAnchor? {
    val tutorial = LocalSpaceCompassTutorial.current
    if (tutorial?.active != true || !enabled) return null
    val anchor = remember(tutorial, feature, circular) { SpaceCompassTutorialAnchor(tutorial, feature, circular) }
    DisposableEffect(anchor) { onDispose { anchor.dispose() } }
    return anchor
}

/** Default bounds fit the 30 dp island circles; custom bounds can align smaller glyphs with text. */
@Composable
internal fun SpaceCompassTutorialIconHighlight(anchor: SpaceCompassTutorialAnchor?,
    modifier: Modifier = Modifier.size(30.dp), content: @Composable () -> Unit) {
    if (anchor == null) content()
    else Box(modifier.then(anchor.visualModifier), contentAlignment = Alignment.Center) { content() }
}
