package me.mondiversi.spacecompass

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Keep every explanation scrollable within the fixed tutorial card. */
@Composable
internal fun SpaceCompassTutorialExplanation(title: String, description: String, scroll: ScrollState) {
    Column(Modifier.fillMaxSize()
        .scrollbarOverlay(scroll, MaterialTheme.colorScheme.onSurface.copy(alpha = .46f),
            trackTopInset = 8.dp, trackBottomInset = 8.dp, touchTargetWidth = 12.dp)
        .verticalScroll(scroll).padding(16.dp).testTag("tutorial-explanation-scroll"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("tutorial-explanation-title"))
        Text(description, fontSize = 14.sp, lineHeight = 19.sp)
    }
}
