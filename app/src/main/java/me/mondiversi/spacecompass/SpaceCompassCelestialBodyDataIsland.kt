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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SpaceCompassObjectNameChevronGap = 12.dp

/** Navigation visits only the checked objects. No arrows for a single or empty selection. */
internal data class SpaceCompassCelestialBodyNavigation(val previousLabel: String, val nextLabel: String,
    val onPrevious: () -> Unit, val onNext: () -> Unit)

/** Mirrored navigation arrows share the island inset; the name fills the space between them. */
@Composable
internal fun SpaceCompassCelestialBodyDataIsland(bodyName: String?, primaryText: Color, background: Color,
    compact: Boolean, navigation: SpaceCompassCelestialBodyNavigation?,
    bodyNameColor: Color = primaryText, onBodyNameClick: (() -> Unit)? = null,
    onDailyPath: (() -> Unit)? = null, headerOnly: Boolean = false,
    dailyPathLeading: Boolean = false, markerRotation: Float = 0f, dailyPathRotation: Float = 0f,
    content: @Composable ColumnScope.() -> Unit) {
    val nameFontSize = if (compact) 12.sp else 14.sp
    val nameStyle = LocalTextStyle.current.copy(shadow = null, fontSize = nameFontSize, fontWeight = FontWeight.Medium)
    val nameMeasurer = rememberTextMeasurer()
    val measuredNameHeight = remember(bodyName, nameStyle, nameMeasurer) {
        nameMeasurer.measure(bodyName.orEmpty(), nameStyle, maxLines = 1, softWrap = false).size.height
    }
    val headerContentHeight = maxOf(measuredNameHeight, with(LocalDensity.current) { 20.dp.roundToPx() })
    val shape = RoundedCornerShape(20.dp)
    val nameTutorial = rememberSpaceCompassTutorialAnchor(SpaceCompassTutorialFeature.OBSERVATORY,
        enabled = onBodyNameClick != null)
    val tutorialNameHeightPx = if (nameTutorial != null) remember(nameTutorial) { mutableIntStateOf(0) } else null
    // Absorb blank-surface taps; child navigation and details actions keep priority.
    Column(Modifier.fillMaxWidth().testTag("sun-finder-body-data").clip(shape)
        .spaceCompassSceneInputShield().background(spaceCompassSettingsCardColor().copy(alpha = SPACE_COMPASS_MAIN_OVERLAY_ALPHA))
    ) {
        if (bodyName != null || navigation != null) {
            // Compact only the title bar; retain the normal insets around the data below it.
            Row(Modifier.fillMaxWidth().testTag("celestial-body-header").padding(horizontal = 16.dp)
                // Halve empty space around the unchanged text/glyphs; action children retain their full targets.
                .layout { measurable, constraints ->
                    val header = measurable.measure(constraints)
                    val previousHeight = header.height - if (headerOnly) 0 else 3.dp.roundToPx()
                    val visibleHeight = (headerContentHeight + (previousHeight-headerContentHeight).coerceAtLeast(0)/2)
                        .coerceAtMost(header.height)
                    layout(header.width, visibleHeight) {
                        header.placeRelative(0, (visibleHeight-header.height)/2)
                    }
                }
                .padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                // Match the actual painted outer edge to the opposite chevron, not the canvas edge.
                val dailyAction: @Composable () -> Unit = {
                    if (onDailyPath != null) {
                        val pathTutorial = rememberSpaceCompassTutorialAnchor(SpaceCompassTutorialFeature.DAILY)
                        val ink = spaceCompassExportIconInkSize(1.4.dp, 20.dp)
                        val axisInk = if (dailyPathRotation % 180f == 0f) ink.width else ink.height
                        // Retain the established inner gap independently of the outer island inset.
                        val halfGap = (2.dp + 34.dp - ink.width) / 2
                        val slotWidth = axisInk + halfGap - 1.dp
                        val edgeCorrection = (slotWidth - axisInk) / 2
                        if (!dailyPathLeading) Spacer(Modifier.width(1.dp))
                        SpaceCompassCelestialIconControl(false, stringResource(R.string.celestial_daily_path), onDailyPath,
                            Modifier.width(slotWidth).offset(x = if (dailyPathLeading) -edgeCorrection else edgeCorrection)
                                .testTag("celestial-body-daily-path").then(pathTutorial?.touchModifier ?: Modifier),
                            role = Role.Button, plainTint = primaryText) {
                            SpaceCompassTutorialIconHighlight(pathTutorial) {
                                Box(Modifier.rotate(dailyPathRotation)) { SpaceCompassDailyPathIcon(20.dp, 1.4.dp) }
                            }
                        }
                        if (dailyPathLeading) Spacer(Modifier.width(1.dp))
                    }
                }
                if (dailyPathLeading) dailyAction()
                // Fit the name between the unfilled glyphs, retaining 48 dp navigation targets.
                Box(Modifier.weight(1f).heightIn(min = 48.dp)) {
                    val nameInteraction = remember { MutableInteractionSource() }
                    // Match visible letter height rather than the full line box, including font scaling.
                    val markerSize = with(LocalDensity.current) { (nameFontSize * .75f).toDp() }
                    Row(Modifier.fillMaxWidth().align(Alignment.Center)
                        // Reserve equal name-to-arrow space at both content edges, including in RTL.
                        .padding(start = if (navigation == null) 0.dp else
                            SpaceCompassObjectHeaderGlyph.CHEVRON.width + SpaceCompassObjectNameChevronGap,
                            end = if (navigation == null) 0.dp else
                                SpaceCompassObjectHeaderGlyph.CHEVRON.width + SpaceCompassObjectNameChevronGap)
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
                                bodyNameColor, primaryText, Modifier.size(markerSize).rotate(markerRotation)
                                    .testTag("celestial-body-color-dot").clearAndSetSemantics {})
                            // The non-weighted dot is measured first; only the trailing name can truncate.
                            Text(bodyName.orEmpty(), Modifier.weight(1f, fill = false).testTag("celestial-body-name"),
                                color = primaryText, style = nameStyle,
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
                if (!dailyPathLeading) dailyAction()
            }
            if (!headerOnly) HorizontalDivider(color = primaryText.copy(alpha = 0.12f),modifier = Modifier.padding(horizontal = 8.dp))
        }
        if (!headerOnly) Column(Modifier.fillMaxWidth().testTag("celestial-body-content").padding(horizontal = 8.dp)
            .padding(vertical = if (compact) 3.dp else 4.dp),
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
        // Both visible tips meet the same content inset; the full 48 dp touch slots remain intact.
        Box(Modifier.fillMaxSize(),
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
