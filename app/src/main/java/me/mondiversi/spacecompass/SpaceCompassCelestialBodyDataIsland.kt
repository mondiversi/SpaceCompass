package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Keep a 6.5 dp visible gap between 30 dp circles in their 48 dp target slots. */
internal val SpaceCompassObjectActionTargetSpacing = (-11.5).dp

/** Navigation visits only the checked objects. No arrows for a single or empty selection. */
internal data class SpaceCompassCelestialBodyNavigation(val previousLabel: String, val nextLabel: String,
    val onPrevious: () -> Unit, val onNext: () -> Unit)

/** The object selector fills the available header width; actions stay at its trailing edge. */
@Composable
internal fun SpaceCompassCelestialBodyDataIsland(bodyName: String?, primaryText: Color, background: Color,
    compact: Boolean, navigation: SpaceCompassCelestialBodyNavigation?,
    actions: @Composable () -> Unit = {}, onRemove: (() -> Unit)? = null,
    bodyNameColor: Color = primaryText,
    content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    // Absorb blank-surface taps; child navigation and details actions keep priority.
    Column(Modifier.fillMaxWidth().testTag("sun-finder-body-data").clip(shape)
        .spaceCompassSceneInputShield().background(spaceCompassSettingsCardColor().copy(alpha = SPACE_COMPASS_MAIN_OVERLAY_ALPHA))
        .padding(horizontal = 12.dp)) {
        if (bodyName != null || navigation != null) {
            // Compact only the title bar; retain the normal insets around the data below it.
            // Reclaim 6 dp from the first action's empty inset for the name and trailing chevron.
            // Its visible circle and the remaining actions stay at their existing positions.
            Row(Modifier.fillMaxWidth().testTag("celestial-body-header")
                .padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (navigation == null) 0.dp else (-6).dp)) {
                // Fit the name between the unfilled glyphs, retaining 48 dp navigation targets.
                Box(Modifier.weight(1f).heightIn(min = 48.dp)) {
                    var nameLayout by remember(bodyName) { mutableStateOf<TextLayoutResult?>(null) }
                    Text(bodyName.orEmpty(), Modifier.fillMaxWidth().align(Alignment.Center)
                        // Center between the visible arrow strokes, retaining the same text width.
                        .padding(start = if (navigation == null) 0.dp else 13.dp,
                            end = if (navigation == null) 0.dp else 23.dp)
                        .testTag("celestial-body-island-title").semantics { heading() }
                        .drawWithContent {
                            // Reuse the exact paragraph, including ellipsis and RTL. Paint the
                            // centered stroke first, then the original colored fill over its inner
                            // half: the visible contour grows only outward, from 0.6 to 1.2 dp.
                            if (bodyNameColor != primaryText) nameLayout?.let { layout ->
                                drawText(layout, color = Color(0xFF14191F), shadow = Shadow.None,
                                    drawStyle = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round))
                            }
                            drawContent()
                        },
                        color = bodyNameColor, style = LocalTextStyle.current.copy(shadow = null),
                        onTextLayout = { nameLayout = it },
                        fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (navigation != null) SpaceCompassCelestialBodyNavigationArrow(
                        navigation.previousLabel, navigation.onPrevious, primaryText, previous = true,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-previous").align(Alignment.CenterStart))
                    if (navigation != null) SpaceCompassCelestialBodyNavigationArrow(
                        navigation.nextLabel, navigation.onNext, primaryText, previous = false,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-next").align(Alignment.CenterEnd))
                }
                // Let the final circle reach the island's normal content edge; its empty
                // 9 dp target inset extends into the existing 12 dp island padding, also in RTL.
                Row(Modifier.layout { measurable, constraints ->
                    val controls = measurable.measure(constraints)
                    layout((controls.width - 9.dp.roundToPx()).coerceAtLeast(0), controls.height) {
                        controls.placeRelative(0, 0)
                    }
                }, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpaceCompassObjectActionTargetSpacing)) {
                    actions()
                    if (onRemove != null && bodyName != null)
                        SpaceCompassRemoveObjectButton(bodyName, onRemove)
                }
            }
            HorizontalDivider(color = primaryText.copy(alpha = 0.12f))
        }
        Column(Modifier.fillMaxWidth().testTag("celestial-body-content")
            .padding(vertical = if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp), content = content)
    }
}

/** Unfilled arrows keep their 48 dp targets; the previous tip meets the island content edge. */
@Composable
private fun SpaceCompassCelestialBodyNavigationArrow(label: String, onClick: () -> Unit, tint: Color,
    previous: Boolean, modifier: Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .spaceCompassAccessibleAction(label, onClick = onClick)) {
        // Keep the next arrow fixed beside Observatory. Moving only the previous glyph to
        // the content edge and reducing the label insets reclaims 20 dp for the name, also in RTL.
        Box(Modifier.fillMaxSize().padding(end = if (previous) 0.dp else 10.dp),
            contentAlignment = if (previous) Alignment.CenterStart else Alignment.CenterEnd) {
            SpaceCompassDisclosureChevron(Modifier.rotate(if (previous != rtl) 180f else 0f), tint)
        }
    }
}
