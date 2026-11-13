package me.mondiversi.spacecompass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/** One mutually exclusive, themed projection selector; its full capsule is a gesture exclusion. */
@Composable
internal fun SpaceCompassCaptureProjectionSwitch(projection: SpaceCompassCaptureProjection,
    enabled: Boolean, onSelect: (SpaceCompassCaptureProjection) -> Unit) {
    val foreground=MaterialTheme.colorScheme.onSurface
    val primary=MaterialTheme.colorScheme.primary
    val activeInk=spaceCompassCounterTextColor(isSystemInDarkTheme())
    val choices: @Composable () -> Unit = {
        ProjectionChoice(projection==SpaceCompassCaptureProjection.FRAME,
            stringResource(R.string.capture_panorama_off),enabled,foreground,primary,activeInk,
            { onSelect(SpaceCompassCaptureProjection.FRAME) },"capture-projection-frame") { tint ->
            SpaceCompassCaptureProjectionIcon(SpaceCompassCaptureProjection.FRAME,tint)
        }
        ProjectionChoice(projection==SpaceCompassCaptureProjection.PANORAMA,
            stringResource(R.string.capture_panorama_on),enabled,foreground,primary,activeInk,
            { onSelect(SpaceCompassCaptureProjection.PANORAMA) },"capture-projection-panorama") { tint ->
            SpaceCompassCaptureProjectionIcon(SpaceCompassCaptureProjection.PANORAMA,tint)
        }
    }
    CompositionLocalProvider(LocalSpaceCompassFloatingControlMultiplier provides SPACE_COMPASS_COMPACT_CONTROL_MULTIPLIER) {
        SpaceCompassFloatingControlHitRegion {
            Surface(Modifier.spaceCompassFloatingControlVisual().testTag("capture-projection-switch"),
                shape=RoundedCornerShape(26.dp),color=spaceCompassPageBackground(),
                border=BorderStroke(1.dp,foreground.copy(alpha=.35f)),shadowElevation=3.dp) {
                Row(Modifier.padding(3.dp).selectableGroup()) { choices() }
            }
        }
    }
}

/** Shared projection glyphs keep the chooser and capture switch visually identical. */
@Composable
internal fun SpaceCompassCaptureProjectionIcon(projection: SpaceCompassCaptureProjection,tint: Color,
    size: Dp = 26.dp,stroke: Dp = 2.1.dp) {
    if(projection==SpaceCompassCaptureProjection.PANORAMA) {
        SpaceCompassSettingsMenuIcon("panorama-image",tint,Modifier.size(size),iconStrokeWidth=stroke)
    } else Canvas(Modifier.size(size)) {
        val low=this.size.minDimension*.13f;val high=this.size.minDimension*.87f
        val arm=this.size.minDimension*.22f;val edge=stroke.toPx()
        for(x in listOf(low,high)) for(y in listOf(low,high)) {
            val dx=if(x==low) arm else -arm;val dy=if(y==low) arm else -arm
            drawLine(tint,Offset(x,y),Offset(x+dx,y),edge,StrokeCap.Round)
            drawLine(tint,Offset(x,y),Offset(x,y+dy),edge,StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectionChoice(selected: Boolean,label: String,enabled: Boolean,foreground: Color,
    primary: Color,activeInk: Color,onClick: () -> Unit,tag: String,icon: @Composable (Color) -> Unit) {
    val tint=(if(selected) activeInk else foreground).copy(alpha=if(enabled) 1f else .38f)
    TooltipBox(positionProvider=TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip={ PlainTooltip { Text(label) } },state=rememberTooltipState()) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(23.dp))
            .background(if(selected) primary.copy(alpha=if(enabled) 1f else .38f) else Color.Transparent)
            .selectable(selected=selected,enabled=enabled,role=Role.Tab,onClick=onClick)
            .testTag(tag).spaceCompassAccessibleAction(label,enabled=enabled,role=Role.Tab,
                selectedState=selected,onClick=onClick),contentAlignment=Alignment.Center) { icon(tint) }
    }
}
