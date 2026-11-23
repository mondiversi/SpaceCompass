package me.mondiversi.spacecompass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

internal const val SPACE_COMPASS_MAIN_DETAILS_KEY = "main_details_visible"
// Reversible layout trial: set true to restore the compact object header/side rail.
// Its portrait layout, landscape selector, viewport exclusion and edge inset remain below.
internal const val SPACE_COMPASS_KEEP_COLLAPSED_OBJECT_HEADER = false
private const val detailsAnimationMs = 300

/** Reclaim real layout space while data and sensor owners remain in the main screen. */
@Composable
internal fun SpaceCompassMainDetailsVisibility(visible: Boolean, landscape: Boolean,
    modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, modifier,
        enter = (if (landscape) expandHorizontally(tween(detailsAnimationMs), expandFrom = Alignment.End)
            else expandVertically(tween(detailsAnimationMs), expandFrom = Alignment.Bottom)) + fadeIn(tween(detailsAnimationMs)),
        exit = (if (landscape) shrinkHorizontally(tween(detailsAnimationMs), shrinkTowards = Alignment.End)
            else shrinkVertically(tween(detailsAnimationMs), shrinkTowards = Alignment.Bottom)) + fadeOut(tween(detailsAnimationMs)),
        label = "main information panels") { content() }
}
