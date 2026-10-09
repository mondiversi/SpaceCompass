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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics

private const val expansionDurationMillis = 300

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

/** The stable heading keeps its original padding; all body spacing collapses with the controls. */
@Composable
internal fun SpaceCompassSettingsIslandReveal(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    AnimatedVisibility(visible, modifier = Modifier.spaceCompassRevealInteraction(visible),
        enter = expandVertically(tween(expansionDurationMillis, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top) + fadeIn(tween(expansionDurationMillis)),
        exit = shrinkVertically(tween(expansionDurationMillis, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top) + fadeOut(tween(expansionDurationMillis))) {
        Column(Modifier.fillMaxWidth().padding(top = SpaceCompassSettingsGroupGap),
            verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsGroupGap), content = content)
    }
}
