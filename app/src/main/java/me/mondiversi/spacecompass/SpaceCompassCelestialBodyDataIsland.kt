package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
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
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth().testTag("sun-finder-body-data").clip(shape)
        .background(background.copy(alpha = 0.84f))
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
                    if (navigation != null) IconButton(onClick = navigation.onPrevious,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-previous")
                            .align(Alignment.CenterStart).semantics { contentDescription = navigation.previousLabel }) {
                        Box(Modifier.fillMaxSize().padding(start = 10.dp), contentAlignment = Alignment.CenterStart) {
                            SpaceCompassDisclosureChevron(Modifier.rotate(if (rtl) 0f else 180f), primaryText)
                        }
                    }
                    if (navigation != null) IconButton(onClick = navigation.onNext,
                        modifier = Modifier.size(48.dp).testTag("celestial-body-next")
                            .align(Alignment.CenterEnd).semantics { contentDescription = navigation.nextLabel }) {
                        Box(Modifier.fillMaxSize().padding(end = 10.dp), contentAlignment = Alignment.CenterEnd) {
                            SpaceCompassDisclosureChevron(Modifier.rotate(if (rtl) 180f else 0f), primaryText)
                        }
                    }
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
