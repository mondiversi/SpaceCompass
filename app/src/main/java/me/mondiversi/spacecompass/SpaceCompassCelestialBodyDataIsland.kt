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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Measure the visible chevron-to-X gap; retain independent arrow and remove hit regions.
internal val SpaceCompassObjectNavigationActionGap = 16.dp
private val SpaceCompassObjectNameChevronGap = 12.dp

/** Navigation visits only the checked objects. No arrows for a single or empty selection. */
internal data class SpaceCompassCelestialBodyNavigation(val previousLabel: String, val nextLabel: String,
    val onPrevious: () -> Unit, val onNext: () -> Unit)

/** The object selector fills the available header width; actions stay at its trailing edge. */
@Composable
internal fun SpaceCompassCelestialBodyDataIsland(bodyName: String?, primaryText: Color, background: Color,
    compact: Boolean, navigation: SpaceCompassCelestialBodyNavigation?,
    actions: @Composable () -> Unit = {},
    bodyNameColor: Color = primaryText, onBodyNameClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val nameTutorial = rememberSpaceCompassTutorialAnchor(SpaceCompassTutorialFeature.OBSERVATORY,
        enabled = onBodyNameClick != null)
    val tutorialNameHeightPx = if (nameTutorial != null) remember(nameTutorial) { mutableIntStateOf(0) } else null
    // Absorb blank-surface taps; child navigation and details actions keep priority.
    Column(Modifier.fillMaxWidth().testTag("sun-finder-body-data").clip(shape)
        .spaceCompassSceneInputShield().background(spaceCompassSettingsCardColor().copy(alpha = SPACE_COMPASS_MAIN_OVERLAY_ALPHA))
        .padding(horizontal = 10.dp)) {
        if (bodyName != null || navigation != null) {
            // Compact only the title bar; retain the normal insets around the data below it.
            // The next chevron ends 10 dp before its slot edge. Measure the smaller X
            // rather than the former action circle when reclaiming space for the name.
            // Keep the trailing actions anchored at the island's content edge.
            Row(Modifier.fillMaxWidth().testTag("celestial-body-header")
                // Bring the divider into the unused lower touch inset; targets retain their full height.
                .layout { measurable, constraints ->
                    val header = measurable.measure(constraints)
                    layout(header.width, (header.height - 3.dp.roundToPx()).coerceAtLeast(0)) {
                        header.placeRelative(0, 0)
                    }
                }
                .padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (navigation == null) 0.dp
                    else SpaceCompassObjectNavigationActionGap - 10.dp -
                        (48.dp - SpaceCompassObjectHeaderGlyph.REMOVE.width) / 2)) {
                // Fit the name between the unfilled glyphs, retaining 48 dp navigation targets.
                Box(Modifier.weight(1f).heightIn(min = 48.dp)) {
                    val nameInteraction = remember { MutableInteractionSource() }
                    val nameFontSize = if (compact) 12.sp else 14.sp
                    // Match visible letter height rather than the full line box, including font scaling.
                    val markerSize = with(LocalDensity.current) { (nameFontSize * .75f).toDp() }
                    Row(Modifier.fillMaxWidth().align(Alignment.Center)
                        // Keep equal caption insets independently of the trailing action spacing.
                        // The next glyph ends 10 dp before its slot edge, including in RTL.
                        .padding(start = if (navigation == null) 0.dp else
                            SpaceCompassObjectHeaderGlyph.CHEVRON.width + SpaceCompassObjectNameChevronGap,
                            end = if (navigation == null) 0.dp else
                                SpaceCompassObjectHeaderGlyph.CHEVRON.width + 10.dp + SpaceCompassObjectNameChevronGap)
                        .then(if (onBodyNameClick != null) Modifier.heightIn(min = 48.dp)
                            .clickable(interactionSource = nameInteraction, indication = null,
                                role = Role.Button, onClickLabel = stringResource(R.string.celestial_details_title),
                                onClick = onBodyNameClick).wrapContentHeight() else Modifier)
                        .testTag("celestial-body-island-title").then(nameTutorial?.touchModifier ?: Modifier).semantics(mergeDescendants = true) { heading() },
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically) {
                        // Highlight the visible marker and name together, without the selector's empty touch insets.
                        Row(Modifier.wrapContentWidth().then(nameTutorial?.visualModifier ?: Modifier)
                            .then(tutorialNameHeightPx?.let { height ->
                                Modifier.onSizeChanged { height.intValue = it.height }
                            } ?: Modifier),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (!bodyName.isNullOrEmpty()) SpaceCompassCelestialColorDot(
                                bodyNameColor, primaryText, Modifier.size(markerSize)
                                    .testTag("celestial-body-color-dot").clearAndSetSemantics {})
                            // The non-weighted dot is measured first; only the trailing name can truncate.
                            Text(bodyName.orEmpty(), Modifier.weight(1f, fill = false).testTag("celestial-body-name"),
                                color = primaryText, style = LocalTextStyle.current.copy(shadow = null),
                                fontSize = nameFontSize, fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (navigation != null) SpaceCompassCelestialBodyNavigationArrow(
                        navigation.previousLabel, navigation.onPrevious, primaryText, previous = true,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-previous").align(Alignment.CenterStart),
                        tutorialHighlightHeightPx = tutorialNameHeightPx?.intValue ?: 0)
                    if (navigation != null) SpaceCompassCelestialBodyNavigationArrow(
                        navigation.nextLabel, navigation.onNext, primaryText, previous = false,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-next").align(Alignment.CenterEnd),
                        tutorialHighlightHeightPx = tutorialNameHeightPx?.intValue ?: 0)
                }
                // Align the trailing action with the divider's edge.
                // Only the target's empty inset extends into the island padding, also in RTL.
                Row(Modifier.layout { measurable, constraints ->
                    val controls = measurable.measure(constraints)
                    val trailingInset = (48.dp - 30.dp) / 2
                    layout((controls.width - trailingInset.roundToPx()).coerceAtLeast(0), controls.height) {
                        controls.placeRelative(0, 0)
                    }
                }, verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
            HorizontalDivider(color = primaryText.copy(alpha = 0.12f))
        }
        Column(Modifier.fillMaxWidth().testTag("celestial-body-content")
            .padding(vertical = if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp), content = content)
    }
}

/** Unfilled arrows keep their 48 dp targets; the previous tip meets the island content edge. */
@Composable
private fun SpaceCompassCelestialBodyNavigationArrow(label: String, onClick: () -> Unit, tint: Color,
    previous: Boolean, modifier: Modifier, tutorialHighlightHeightPx: Int) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val interaction = remember { MutableInteractionSource() }
    val tutorial = rememberSpaceCompassTutorialAnchor(SpaceCompassTutorialFeature.NAVIGATION)
    Box(modifier.then(tutorial?.touchModifier ?: Modifier).clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .spaceCompassAccessibleAction(label, onClick = onClick)) {
        // Keep the next arrow fixed beside the trailing actions. Moving only the previous glyph to
        // the content edge and reducing the label insets reclaims 20 dp for the name, also in RTL.
        Box(Modifier.fillMaxSize().padding(end = if (previous) 0.dp else 10.dp),
            contentAlignment = if (previous) Alignment.CenterStart else Alignment.CenterEnd) {
            SpaceCompassTutorialIconHighlight(tutorial, modifier = if (tutorial == null) Modifier else
                Modifier.requiredSize(SpaceCompassObjectHeaderGlyph.CHEVRON.width,
                    with(LocalDensity.current) { tutorialHighlightHeightPx.toDp() }.coerceAtLeast(
                        SpaceCompassObjectHeaderGlyphHeight * SpaceCompassObjectHeaderGlyph.CHEVRON.visualScale))) {
                SpaceCompassObjectHeaderIcon(SpaceCompassObjectHeaderGlyph.CHEVRON,
                    Modifier.rotate(if (previous != rtl) 180f else 0f), tint)
            }
        }
    }
}
