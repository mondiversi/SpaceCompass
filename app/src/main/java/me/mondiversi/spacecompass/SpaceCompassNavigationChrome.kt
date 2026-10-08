package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

internal val SpaceCompassTitleActionButtonSize = 40.dp
// Use the existing target more fully, without increasing toolbar spacing.
internal val SpaceCompassTitleActionPressedVisualSize = 38.dp
internal const val SpaceCompassTitleActionVisualScale = 0.96f
internal val SpaceCompassTitleActionIconSize = 24.dp
internal val SpaceCompassTitleActionIconStrokeWidth = 2.dp
// Only the back arrow keeps additional visual weight; other title actions use the base stroke.
internal val SpaceCompassTitleBackButtonSize = 40.dp
// Keep the back arrow independent: resizing the right-hand actions must not resize it.
internal val SpaceCompassTitleBackIconSize = 15.36.dp
internal val SpaceCompassTitleBackPressedVisualSize = 28.dp
internal val SpaceCompassTitleBackIconStrokeWidth = 2.6.dp
// The rounded chevron's ink lies slightly right of its canvas center.
// Move only the drawing, not the pressed area or the accessible touch target.
internal val SpaceCompassTitleBackIconOpticalOffset = SpaceCompassTitleBackIconSize * -0.0375f
internal val SpaceCompassTitleBarContentPadding = PaddingValues(
    start = 5.dp,
    top = 4.dp,
    end = 5.dp,
    bottom = 4.dp
)

/** Ordinary title actions use the same neutral color in both states. */
@Composable
internal fun spaceCompassTitleActionColor(enabled: Boolean = true): Color {
    val neutral = spaceCompassNeutralContentColor()
    return if (enabled) neutral else neutral.copy(alpha = 0.38f)
}

@Composable
internal fun spaceCompassTitleActionStateColor(active: Boolean, enabled: Boolean = true): Color =
    if (active && enabled) MaterialTheme.colorScheme.primary else spaceCompassTitleActionColor(enabled)

/** Bare themed glyph and circular pressed feedback, with the full touch target. */
@Composable
internal fun SpaceCompassTitleActionButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconColor: Color = spaceCompassTitleActionColor(),
    content: @Composable () -> Unit
) {
    SpaceCompassAccessibleIconButton(
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier.size(SpaceCompassTitleActionButtonSize),
        enabled = enabled,
        pressedColor = iconColor,
        pressedVisualSize = SpaceCompassTitleActionPressedVisualSize,
        visualScale = SpaceCompassTitleActionVisualScale,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) iconColor
                else spaceCompassTitleActionColor(enabled = false),
            content = content
        )
    }
}

/** All secondary pages share the celestial detail toolbar's compact title spacing. */
@Composable
internal fun SpaceCompassPageToolbar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    titleColor: Color = Color.Unspecified,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier.fillMaxWidth().padding(SpaceCompassTitleBarContentPadding),
        verticalAlignment = Alignment.CenterVertically) {
        SpaceCompassBackButton(onBack)
        SpaceCompassMenuTitle(title, Modifier.weight(1f).then(titleModifier), color = titleColor)
        actions()
    }
}

@Composable
fun SpaceCompassMenuTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 20.sp
) {
    SpaceCompassScrollingText(text, modifier, color,
        style = LocalTextStyle.current.copy(fontSize = fontSize, fontWeight = FontWeight.Bold))
}

@Composable
fun SpaceCompassBackButton(
    onClick: () -> Unit
) {
    val description =
        stringResource(
            R.string.navigate_back
        )

    val tint = spaceCompassTitleActionColor()

    SpaceCompassAccessibleIconButton(
        contentDescription = description,
        onClick = onClick,
        modifier = Modifier.size(SpaceCompassTitleBackButtonSize),
        pressedColor = tint,
        pressedVisualSize = SpaceCompassTitleBackPressedVisualSize
    ) {
        Canvas(
            modifier =
                Modifier.size(SpaceCompassTitleBackIconSize)
        ) {
            val strokeWidth =
                SpaceCompassTitleBackIconStrokeWidth.toPx()
            val opticalOffset = SpaceCompassTitleBackIconOpticalOffset.toPx()
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
