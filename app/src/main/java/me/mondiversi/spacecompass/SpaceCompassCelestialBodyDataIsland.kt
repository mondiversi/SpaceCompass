package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Navigation visits only the checked objects. No arrows for a single or empty selection. */
internal data class SpaceCompassCelestialBodyNavigation(val previousLabel: String, val nextLabel: String,
    val onPrevious: () -> Unit, val onNext: () -> Unit)

/** The object selector fills the available header width; actions stay at its trailing edge. */
@Composable
internal fun SpaceCompassCelestialBodyDataIsland(bodyName: String?, primaryText: Color, background: Color,
    compact: Boolean, navigation: SpaceCompassCelestialBodyNavigation?,
    actions: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().testTag("sun-finder-body-data").clip(shape)
        .background(spaceCompassSettingsCardColor().copy(alpha = SPACE_COMPASS_MAIN_OVERLAY_ALPHA))
        .padding(horizontal = 12.dp)) {
        if (bodyName != null || navigation != null) {
            // Compact only the title bar; retain the normal insets around the data below it.
            Row(Modifier.fillMaxWidth().testTag("celestial-body-header")
                .padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                // Reserve only the visible chevrons around the name, retaining 48 dp action targets.
                Box(Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(bodyName.orEmpty(), Modifier.fillMaxWidth()
                        .padding(horizontal = if (navigation == null) 0.dp else 32.dp)
                        .align(Alignment.Center).testTag("celestial-body-island-title").semantics { heading() },
                        color = primaryText, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (navigation != null) SpaceCompassCelestialBodyNavigationArrow(
                        navigation.previousLabel, navigation.onPrevious, primaryText, previous = true,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-previous").align(Alignment.CenterStart))
                    if (navigation != null) SpaceCompassCelestialBodyNavigationArrow(
                        navigation.nextLabel, navigation.onNext, primaryText, previous = false,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-next").align(Alignment.CenterEnd))
                }
                actions()
            }
            HorizontalDivider(color = primaryText.copy(alpha = 0.12f))
        }
        Column(Modifier.fillMaxWidth().testTag("celestial-body-content")
            .padding(vertical = if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp), content = content)
    }
}

/** Retain the 48 dp target and chevron layout; only its 28 dp feedback is painted. */
@Composable
private fun SpaceCompassCelestialBodyNavigationArrow(label: String, onClick: () -> Unit, tint: Color,
    previous: Boolean, modifier: Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .spaceCompassAccessibleAction(label, onClick = onClick)) {
        // A 28 dp circle at the one-dp inset keeps its 10 dp glyph at the existing 10 dp inset.
        Box(Modifier.fillMaxSize().padding(horizontal = 1.dp),
            contentAlignment = if (previous) Alignment.CenterStart else Alignment.CenterEnd) {
            Box(Modifier.size(28.dp).clip(CircleShape)
                .indication(interaction, ripple(bounded = true, color = tint)), contentAlignment = Alignment.Center) {
                SpaceCompassDisclosureChevron(Modifier.rotate(if (previous != rtl) 180f else 0f), tint)
            }
        }
    }
}
