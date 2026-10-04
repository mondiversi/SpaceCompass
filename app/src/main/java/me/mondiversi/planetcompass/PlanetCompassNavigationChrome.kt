package me.mondiversi.planetcompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val PlanetCompassTitleActionButtonSize = 40.dp
// Use the existing target more fully, without increasing toolbar spacing.
internal val PlanetCompassTitleActionPressedVisualSize = 38.dp
internal const val PlanetCompassTitleActionVisualScale = 0.96f
internal val PlanetCompassTitleActionIconSize = 24.dp
internal val PlanetCompassTitleActionIconStrokeWidth = 2.21.dp
// Only the back arrow keeps additional visual weight; other title actions use the base stroke.
internal val PlanetCompassTitleBackButtonSize = 40.dp
// Keep the back arrow independent: resizing the right-hand actions must not resize it.
internal val PlanetCompassTitleBackIconSize = 15.36.dp
internal val PlanetCompassTitleBackPressedVisualSize = 28.dp
internal val PlanetCompassTitleBackIconStrokeWidth = 2.6.dp
// The rounded chevron's ink lies slightly right of its canvas center.
// Move only the drawing, not the pressed area or the accessible touch target.
internal val PlanetCompassTitleBackIconOpticalOffset = PlanetCompassTitleBackIconSize * -0.0375f
internal val PlanetCompassTitleBarContentPadding = PaddingValues(
    start = 5.dp,
    top = 4.dp,
    end = 5.dp,
    bottom = 4.dp
)

/** Ordinary title actions use the same neutral color in both states. */
@Composable
internal fun planetCompassTitleActionColor(enabled: Boolean = true): Color {
    val neutral = planetCompassNeutralContentColor()
    return if (enabled) neutral else neutral.copy(alpha = 0.38f)
}

@Composable
internal fun planetCompassTitleActionStateColor(active: Boolean, enabled: Boolean = true): Color =
    if (active && enabled) MaterialTheme.colorScheme.primary else planetCompassTitleActionColor(enabled)

/** Bare themed glyph and circular pressed feedback, with the full touch target. */
@Composable
internal fun PlanetCompassTitleActionButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconColor: Color = planetCompassTitleActionColor(),
    content: @Composable () -> Unit
) {
    PlanetCompassAccessibleIconButton(
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier.size(PlanetCompassTitleActionButtonSize),
        enabled = enabled,
        pressedColor = iconColor,
        pressedVisualSize = PlanetCompassTitleActionPressedVisualSize,
        visualScale = PlanetCompassTitleActionVisualScale,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) iconColor
                else planetCompassTitleActionColor(enabled = false),
            content = content
        )
    }
}

@Composable
fun PlanetCompassMenuTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 20.sp
) {
    PlanetCompassScrollingText(text, modifier, color,
        style = LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = FontWeight.Bold))
}

@Composable
fun PlanetCompassBackButton(
    onClick: () -> Unit
) {
    val description =
        stringResource(
            R.string.navigate_back
        )

    val tint = planetCompassTitleActionColor()

    PlanetCompassAccessibleIconButton(
        contentDescription = description,
        onClick = onClick,
        modifier = Modifier.size(PlanetCompassTitleBackButtonSize),
        pressedColor = tint,
        pressedVisualSize = PlanetCompassTitleBackPressedVisualSize
    ) {
        Canvas(
            modifier =
                Modifier.size(PlanetCompassTitleBackIconSize)
        ) {
            val strokeWidth =
                PlanetCompassTitleBackIconStrokeWidth.toPx()
            val opticalOffset = PlanetCompassTitleBackIconOpticalOffset.toPx()
            val glyphWidth = size.width
            val glyphHeight = size.height
            val left = (size.width - glyphWidth) / 2f
            val top = (size.height - glyphHeight) / 2f

            val center =
                Offset(
                    x = left + glyphWidth * 0.34f + opticalOffset,
                    y = top + glyphHeight * 0.50f
                )

            drawLine(
                color = tint,
                start =
                    Offset(
                        x = left + glyphWidth * 0.68f + opticalOffset,
                        y = top + glyphHeight * 0.20f
                    ),
                end = center,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = tint,
                start = center,
                end =
                    Offset(
                        x = left + glyphWidth * 0.68f + opticalOffset,
                        y = top + glyphHeight * 0.80f
                    ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
