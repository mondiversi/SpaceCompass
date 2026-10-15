package me.mondiversi.spacecompass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics

internal const val SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS = 300

/** Retain outgoing controls for the fade, but make them inert as soon as the island is disabled. */
private fun Modifier.spaceCompassRevealInteraction(visible: Boolean): Modifier =
    if (visible) this else clearAndSetSemantics { }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }

/** Header owns its full padding; body side/bottom padding disappears with the controls. */
@Composable
internal fun SpaceCompassSettingsIslandReveal(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val direction = LocalLayoutDirection.current
    AnimatedVisibility(visible, modifier = Modifier.spaceCompassRevealInteraction(visible),
        enter = expandVertically(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top) + fadeIn(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS)),
        exit = shrinkVertically(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top) + fadeOut(tween(SPACE_COMPASS_ISLAND_REVEAL_DURATION_MS))) {
        Column(Modifier.fillMaxWidth().padding(
            start = spaceCompassIslandContentPadding.calculateStartPadding(direction),
            end = spaceCompassIslandContentPadding.calculateEndPadding(direction),
            bottom = spaceCompassIslandContentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsGroupGap), content = content)
    }
}
