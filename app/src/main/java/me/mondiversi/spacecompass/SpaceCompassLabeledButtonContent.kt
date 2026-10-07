package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Match UVIR: fixed leading icon slot, then a centered label in the remaining width. */
@Composable
internal fun RowScope.SpaceCompassLabeledButtonContent(label: String, icon: @Composable () -> Unit) {
    icon()
    Spacer(Modifier.width(8.dp))
    val caption = label.uppercase(LocalResources.current.configuration.locales[0])
    val measurer = rememberTextMeasurer()
    val style = LocalTextStyle.current.merge(androidx.compose.ui.text.TextStyle(
        fontWeight = if (LocalSpaceCompassSettingsActionButtons.current) FontWeight.SemiBold else null,
        textAlign = TextAlign.Center))
    BoxWithConstraints(Modifier.weight(1f)) {
        val available = constraints.maxWidth
        val fontSize = remember(measurer, available, style, caption) {
            fitSpaceCompassButtonFontSize(14f, 10f) { candidate ->
                !measurer.measure(caption, style.copy(fontSize = candidate.sp), maxLines = 1,
                    softWrap = false, constraints = Constraints(maxWidth = available)).hasVisualOverflow
            }.sp
        }
        Text(caption, Modifier.fillMaxWidth(), style = style.copy(fontSize = fontSize),
            maxLines = 1, softWrap = false)
    }
}
