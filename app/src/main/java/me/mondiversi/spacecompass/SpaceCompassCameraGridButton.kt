package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal const val SPACE_COMPASS_CAMERA_GRID_KEY="camera_grid_visible"
internal const val SPACE_COMPASS_CAMERA_GRID_DEFAULT=true

/** The camera uses its optical axis; its configuration slot becomes a live angular-grid switch. */
@Composable
internal fun SpaceCompassCameraGridButton(checked: Boolean,onToggle: ()->Unit,color: Color,background: Color) {
    val tint=spaceCompassFloatingControlTint(color,checked)
    SpaceCompassFloatingActionButton(stringResource(R.string.camera_grid),onToggle,tint,background,
        Modifier.testTag("toggle-camera-grid").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.CAMERA_GRID),
        selectedState=checked,stateText=stringResource(if(checked) R.string.camera_grid_on else R.string.camera_grid_off),
        glyphInk=spaceCompassFloatingInk(26f,.12f,.12f,.88f,.88f)) {
        val visualStroke=SpaceCompassFloatingIconStrokeWidth*LocalSpaceCompassFloatingStrokeScale.current
        SpaceCompassIconCanvas(tint,Modifier.size(26.dp)) { ink ->
            val s=size.minDimension;val stroke=Stroke(visualStroke.toPx(),cap=StrokeCap.Round)
            drawCircle(ink,s*.38f,style=stroke)
            drawOval(ink,Offset(s*.30f,s*.12f),Size(s*.40f,s*.76f),style=stroke)
            drawLine(ink,Offset(s*.12f,s*.50f),Offset(s*.88f,s*.50f),stroke.width,StrokeCap.Round)
        }
    }
}
