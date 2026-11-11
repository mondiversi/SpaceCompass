package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class SpaceCompassViewMode { PANORAMA, CAMERA, GLOBE }

internal val SpaceCompassViewMode.labelResource: Int get()=when(this) {
    SpaceCompassViewMode.PANORAMA -> R.string.view_mode_sky
    SpaceCompassViewMode.CAMERA -> R.string.view_mode_camera
    SpaceCompassViewMode.GLOBE -> R.string.view_mode_globe
}
internal val SpaceCompassViewMode.tutorialDescriptionResource: Int get()=when(this) {
    SpaceCompassViewMode.PANORAMA -> R.string.sun_finder_sky_description
    SpaceCompassViewMode.CAMERA -> R.string.camera_view_description
    SpaceCompassViewMode.GLOBE -> R.string.globe_direction_note
}

@Composable
internal fun SpaceCompassGlobeIcon(tint: Color = LocalContentColor.current,size: Dp = 24.dp,stroke: Dp = 1.6.dp) {
    Canvas(Modifier.size(size)) {
        val edge=stroke.toPx();val radius=this.size.minDimension*.43f
        drawCircle(tint,radius,style=Stroke(edge))
        drawOval(tint,Offset(center.x-radius*.45f,center.y-radius),Size(radius*.9f,radius*2),style=Stroke(edge))
        drawLine(tint,Offset(center.x-radius,center.y),Offset(center.x+radius,center.y),edge)
        drawOval(tint,Offset(center.x-radius,center.y-radius*.4f),Size(radius*2,radius*.8f),style=Stroke(edge))
    }
}

@Composable
internal fun SpaceCompassViewModeButton(mode: SpaceCompassViewMode,onChoose: (SpaceCompassViewMode)->Unit,
    color: Color,background: Color,tutorial: Boolean=false) {
    var expanded by remember { mutableStateOf(false) }
    val guide=LocalSpaceCompassTutorial.current
    Box {
        SpaceCompassFloatingActionButton(stringResource(R.string.view_mode_choose)+": "+stringResource(mode.labelResource),
            { if(tutorial) guide?.explore(SpaceCompassTutorialFeature.CAMERA) else expanded=true },color,background,
            Modifier.testTag("toggle-camera").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.CAMERA),
            overlay={
                // Symmetric menu cues preserve the fitted mode glyph and the existing touch target.
                for(up in listOf(true,false)) {
                    Canvas(Modifier.align(if(up) Alignment.TopCenter else Alignment.BottomCenter)
                        .padding(vertical=4.dp).size(10.dp,5.dp).clearAndSetSemantics {}) {
                        val outerY=if(up) .80f else .20f
                        val tipY=if(up) .25f else .75f
                        val path=Path().apply {
                            moveTo(size.width*.15f,size.height*outerY)
                            lineTo(size.width*.50f,size.height*tipY)
                            lineTo(size.width*.85f,size.height*outerY)
                        }
                        drawPath(path,color,style=Stroke(1.5.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
                    }
                }
            }) {
            when(mode) {
                SpaceCompassViewMode.GLOBE -> SpaceCompassFloatingGlyph(spaceCompassFloatingVectorInk(R.drawable.ic_globe_view)) {
                    SpaceCompassFloatingVectorIcon(R.drawable.ic_globe_view)
                }
                SpaceCompassViewMode.CAMERA -> SpaceCompassFloatingGlyph(spaceCompassFloatingVectorInk(R.drawable.ic_camera)) {
                    SpaceCompassFloatingVectorIcon(R.drawable.ic_camera)
                }
                SpaceCompassViewMode.PANORAMA -> SpaceCompassFloatingGlyph(spaceCompassFloatingInk(26f,.10f,.175f,.90f,.825f)) {
                    SpaceCompassSkyModeIcon(LocalContentColor.current,26.dp,2.1.dp)
                }
            }
        }
        SpaceCompassViewModeMenu(expanded,mode,{expanded=false},{value -> expanded=false;onChoose(value)})
    }
}


/** Reused by the real selector and the tutorial overlay, with identical icons and positioning. */
@Composable
internal fun SpaceCompassViewModeMenu(expanded: Boolean,mode: SpaceCompassViewMode,
    onDismiss: ()->Unit,onChoose: (SpaceCompassViewMode)->Unit) {
        SpaceCompassAdaptiveDropdownMenu(expanded,onDismiss,shape=RoundedCornerShape(16.dp),
            containerColor=spaceCompassSettingsCardColor(),modifier=Modifier.testTag("view-mode-options")) {
            SpaceCompassViewMode.entries.forEachIndexed { index,value ->
                if(index>0) HorizontalDivider(color=MaterialTheme.colorScheme.onSurface.copy(alpha=.10f))
                SpaceCompassNavigationMenuItem(stringResource(value.labelResource),{onChoose(value)},
                    Modifier.testTag("view-mode-${value.name.lowercase()}").semantics { selected=value==mode }) { tint ->
                    when(value) {
                        SpaceCompassViewMode.GLOBE -> SpaceCompassGlobeIcon(tint,SpaceCompassMenuIconSize,SpaceCompassMenuIconStrokeWidth)
                        SpaceCompassViewMode.PANORAMA -> SpaceCompassSkyModeIcon(tint,SpaceCompassMenuIconSize,SpaceCompassMenuIconStrokeWidth)
                        SpaceCompassViewMode.CAMERA -> Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_camera),null,
                            Modifier.size(SpaceCompassMenuIconSize),tint=tint)
                    }
                }
            }
        }
}

/** Horizon and a star distinguish observing the sky from a panoramic output format. */
@Composable
internal fun SpaceCompassSkyModeIcon(tint: Color,size: Dp,stroke: Dp) {
    Canvas(Modifier.size(size)) {
        val s=this.size.minDimension;val edge=stroke.toPx()
        drawArc(tint,180f,180f,false,Offset(s*.12f,s*.19f),Size(s*.76f,s*1.18f),style=Stroke(edge))
        drawLine(tint,Offset(s*.10f,s*.78f),Offset(s*.90f,s*.78f),edge)
        val p=androidx.compose.ui.graphics.Path().apply {
            moveTo(s*.61f,s*.10f);lineTo(s*.65f,s*.25f);lineTo(s*.80f,s*.29f)
            lineTo(s*.65f,s*.33f);lineTo(s*.61f,s*.48f);lineTo(s*.57f,s*.33f)
            lineTo(s*.42f,s*.29f);lineTo(s*.57f,s*.25f);close()
        }
        drawPath(p,tint)
    }
}
