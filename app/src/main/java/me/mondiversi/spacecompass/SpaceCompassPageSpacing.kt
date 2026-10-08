package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/** Shared defaults for current and future pages containing islands. */
internal val spaceCompassPageContentPadding = PaddingValues(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 16.dp)
internal val spaceCompassIslandContentPadding = PaddingValues(12.dp)

/** Keep detail cards and external notes clear of the viewport scrollbar. */
internal val spaceCompassDetailScrollInset = 8.dp

/** Match UVIR's independent settings/island metrics; do not apply these to sky overlays. */
internal val SpaceCompassSettingsIslandGap = 9.dp
internal val SpaceCompassSettingsGroupGap = 12.dp
internal val SpaceCompassSettingsControlGap = 8.dp
internal val SpaceCompassSettingsChoiceSpacing = 2.dp
internal val SpaceCompassSettingsChoiceHorizontalPadding = 10.dp
internal val SpaceCompassSettingsChoiceVerticalPadding = 6.dp
internal val SpaceCompassSettingsControlSize = 24.dp
internal val SpaceCompassSettingsActionGap = 6.dp
