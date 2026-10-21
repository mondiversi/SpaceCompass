package me.mondiversi.spacecompass

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Emphasize thin action glyphs without changing their viewport or shared toolbar assets. */
@DrawableRes
internal fun spaceCompassActionIconResource(@DrawableRes original: Int, emphasized: Boolean): Int =
    if (!emphasized) original else when (original) {
        R.drawable.ic_check -> R.drawable.ic_action_check
        R.drawable.ic_update -> R.drawable.ic_action_update
        // The filled repository logo is already solid; preserve its internal cutouts.
        else -> original
    }

@Composable
internal fun SpaceCompassActionIcon(@DrawableRes resource: Int, modifier: Modifier = Modifier.size(20.dp)) {
    Icon(painterResource(spaceCompassActionIconResource(resource,
        LocalSpaceCompassActionGlyphStrokeScale.current > 1f)), null, modifier)
}
