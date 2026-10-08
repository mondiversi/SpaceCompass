package me.mondiversi.spacecompass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal const val SPACE_COMPASS_MAIN_DETAILS_KEY = "main_details_visible"
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

/** Follow the panels' collapse axis; match the other floating controls. */
@Composable
internal fun SpaceCompassMainDetailsToggleButton(expanded: Boolean, onClick: () -> Unit,
    color: Color, background: Color, modifier: Modifier = Modifier, landscape: Boolean = false) {
    val label = stringResource(if (expanded) R.string.main_details_hide else R.string.main_details_show)
    val horizontalDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 90f else -90f
    val targetAngle = if (landscape) (if (expanded) horizontalDirection else -horizontalDirection)
        else if (expanded) 0f else 180f
    val angle by animateFloatAsState(targetAngle, tween(detailsAnimationMs), label = "main details chevron")
    SpaceCompassFloatingControlHitRegion {
        Surface(onClick = onClick, modifier = modifier.spaceCompassFloatingControlVisual().size(48.dp)
            .testTag("toggle-main-details").spaceCompassAccessibleAction(label, onClick = onClick),
            shape = CircleShape, color = background.copy(alpha = .94f), contentColor = color,
            border = BorderStroke(1.dp, color.copy(alpha = .35f)), shadowElevation = 3.dp) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(26.dp)) {
                    // The requested optical correction stays above the circle center in both states.
                    val pivot = Offset(size.width / 2f, size.height / 2f - 1.dp.toPx())
                    val halfWidth = size.width * .27f
                    val halfHeight = size.height * .14f
                    val chevron = Path().apply {
                        moveTo(pivot.x - halfWidth, pivot.y - halfHeight)
                        lineTo(pivot.x, pivot.y + halfHeight)
                        lineTo(pivot.x + halfWidth, pivot.y - halfHeight)
                    }
                    // Rotate around the optical centre while preserving the reviewed portrait alignment.
                    val radians = Math.toRadians(angle.toDouble())
                    val opticalCorrection = halfHeight / 3f
                    translate(left = -opticalCorrection * sin(radians).toFloat(),
                        top = opticalCorrection * (1f + cos(radians).toFloat())) {
                        rotate(angle, pivot) {
                            drawPath(chevron, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        }
    }
}
