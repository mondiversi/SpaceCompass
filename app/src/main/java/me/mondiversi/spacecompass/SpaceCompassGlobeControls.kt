package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal const val SPACE_COMPASS_GLOBE_LABELS_KEY="globe_labels_visible"
internal const val SPACE_COMPASS_GLOBE_LABELS_DEFAULT=true
internal const val SPACE_COMPASS_GLOBE_AXIS_KEY="globe_axis_locked"
internal const val SPACE_COMPASS_GLOBE_ZOOM_VERSION_KEY="globe_zoom_scale_version"

@Composable
internal fun SpaceCompassGlobeLabelsButton(mode: SpaceCompassPanoramaLabelMode,
    onChoose: (SpaceCompassPanoramaLabelMode)->Unit,color: Color,background: Color) {
    var expanded by remember { mutableStateOf(false) }
    val tutorialActive=LocalSpaceCompassTutorial.current?.active==true
    val tint=spaceCompassFloatingControlTint(color,mode!=SpaceCompassPanoramaLabelMode.NONE)
    Box {
        SpaceCompassFloatingActionButton(stringResource(R.string.panorama_point_labels),{if(!tutorialActive) expanded=true},tint,background,
            Modifier.testTag("globe-labels").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.GLOBE_LABELS),selectedState=mode!=SpaceCompassPanoramaLabelMode.NONE,
            stateText=stringResource(mode.labelResource)) {
            SpaceCompassPanoramaLabelsGlyph()
        }
        SpaceCompassPanoramaLabelsDialog(expanded,mode,{expanded=false;onChoose(it)},{expanded=false},modes=SPACE_COMPASS_GLOBE_MAIN_LABEL_MODES)
    }
}

@Composable
internal fun SpaceCompassGlobeRotationButton(locked: Boolean,onToggle: ()->Unit,color: Color,background: Color) {
    SpaceCompassFloatingActionButton(stringResource(if(locked) R.string.globe_rotation_axis else R.string.globe_rotation_free),
        onToggle,color,background,Modifier.testTag("globe-rotation").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.GLOBE_ROTATION,
            title=if(locked) R.string.globe_rotation_axis else R.string.globe_rotation_free),
        glyphInk=spaceCompassFloatingInk(26f,.06f,.06f,.94f,.94f)) {
        SpaceCompassIconCanvas(color,Modifier.size(26.dp)) { ink ->
            val s=size.minDimension;val stroke=2.1.dp.toPx()
            drawCircle(ink,s*.30f,style=Stroke(stroke))
            if(locked) {
                val a=Offset(s*.35f,s*.06f);val b=Offset(s*.65f,s*.94f)
                drawLine(ink,a,b,stroke,StrokeCap.Round)
            } else {
                drawLine(ink,Offset(s*.5f,s*.05f),Offset(s*.5f,s*.95f),stroke,StrokeCap.Round)
                drawLine(ink,Offset(s*.05f,s*.5f),Offset(s*.95f,s*.5f),stroke,StrokeCap.Round)
            }
            drawArc(ink,25f,115f,false,Offset(s*.08f,s*.29f),Size(s*.84f,s*.42f),style=Stroke(stroke,cap=StrokeCap.Round))
            drawLine(ink,Offset(s*.11f,s*.55f),Offset(s*.09f,s*.70f),stroke,StrokeCap.Round)
        }
    }
}

/** One background covers the whole main view; the embedded Earth scene stays transparent. */
@Composable
internal fun SpaceCompassGlobeUniverseBackdrop(modifier: Modifier=Modifier) {
    val dark=isSystemInDarkTheme()
    Canvas(modifier) {
        val palette=SpaceCompassGlobeUniverse.palette(dark).map(::Color)
        // Shared colours, stops and star field are also used by the frozen capture.
        drawRect(Brush.radialGradient(0f to palette[0],.45f to palette[1],1f to palette[2],
            center=Offset(size.width*.7f,size.height*.7f),radius=size.width))
        SpaceCompassGlobeUniverse.stars.forEach { star ->
            drawCircle(Color(SpaceCompassGlobeUniverse.starColor),star.radius.dp.toPx(),
                Offset(size.width*star.x,size.height*star.y))
        }
    }
}
