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
import kotlin.math.cos
import kotlin.math.sin

/** A hollow disc suggests transparency; a matching diagonal slash marks the opaque/off state. */
@Composable
internal fun SpaceCompassCameraObjectTransparencyButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    color: Color, background: Color, enabled: Boolean = true) {
    val tint = spaceCompassFloatingControlTint(color,checked,enabled)
    SpaceCompassFloatingActionButton(stringResource(R.string.camera_object_transparency),{ onCheckedChange(!checked) },
        tint,background,Modifier.testTag("toggle-camera-object-transparency"),enabled=enabled,selectedState=checked,
        stateText=stringResource(if (checked) R.string.camera_objects_translucent else R.string.camera_objects_opaque),
        glyphInk=spaceCompassFloatingInk(26f, if (checked) .14f else .12f, if (checked) .14f else .12f,
            if (checked) .86f else .88f, if (checked) .86f else .88f)) {
        val visualStroke = SpaceCompassFloatingIconStrokeWidth * LocalSpaceCompassFloatingStrokeScale.current
        SpaceCompassIconCanvas(tint.copy(alpha=if(enabled) 1f else .38f),Modifier.size(26.dp)) { ink ->
            val radius=size.minDimension*.36f
            val centre=Offset(size.width*.5f,size.height*.5f)
            val stroke=visualStroke.toPx()
            drawArc(ink,startAngle=90f,sweepAngle=180f,useCenter=false,
                topLeft=centre-Offset(radius,radius),size=Size(radius*2,radius*2),
                style=Stroke(stroke,cap=StrokeCap.Round))
            for (degrees in -65..65 step 26) {
                val angle=Math.toRadians(degrees.toDouble())
                val point=centre+Offset(cos(angle).toFloat()*radius,sin(angle).toFloat()*radius)
                drawCircle(ink,stroke*.5f,point)
            }
            if (!checked) {
                drawLine(ink,Offset(size.width*.12f,size.height*.12f),
                    Offset(size.width*.88f,size.height*.88f),stroke,cap=StrokeCap.Round)
            }
        }
    }
}
