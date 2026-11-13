package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class SpaceCompassViewMode { PANORAMA, SKY_PANORAMA, CAMERA, GLOBE, GLOBE_PANORAMA;
    val globe: Boolean get() = this == GLOBE || this == GLOBE_PANORAMA
    val panoramic: Boolean get() = this == SKY_PANORAMA || this == GLOBE_PANORAMA
}
internal const val SPACE_COMPASS_LIVE_PROJECTION_KEY = "main_live_projection_v2"
internal fun spaceCompassLiveViewMode(globe: Boolean, panoramic: Boolean) = when {
    globe && panoramic -> SpaceCompassViewMode.GLOBE_PANORAMA
    globe -> SpaceCompassViewMode.GLOBE
    panoramic -> SpaceCompassViewMode.SKY_PANORAMA
    else -> SpaceCompassViewMode.PANORAMA
}

internal val SpaceCompassViewMode.labelResource: Int get()=when(this) {
    SpaceCompassViewMode.PANORAMA -> R.string.view_mode_sky_frame
    SpaceCompassViewMode.SKY_PANORAMA -> R.string.view_mode_sky_panorama
    SpaceCompassViewMode.CAMERA -> R.string.view_mode_camera
    SpaceCompassViewMode.GLOBE -> R.string.view_mode_globe_frame
    SpaceCompassViewMode.GLOBE_PANORAMA -> R.string.view_mode_globe_panorama
}
internal val SpaceCompassViewMode.tutorialDescriptionResource: Int get()=when(this) {
    SpaceCompassViewMode.PANORAMA -> R.string.tutorial_mode_sky_frame
    SpaceCompassViewMode.SKY_PANORAMA -> R.string.tutorial_mode_sky_panorama
    SpaceCompassViewMode.CAMERA -> R.string.tutorial_mode_camera
    SpaceCompassViewMode.GLOBE -> R.string.tutorial_mode_globe_frame
    SpaceCompassViewMode.GLOBE_PANORAMA -> R.string.tutorial_mode_globe_panorama
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
    color: Color,background: Color,tutorial: Boolean=false,weather: Boolean=true,
    onWeatherChoose: (Boolean)->Unit={}) {
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
                SpaceCompassViewMode.SKY_PANORAMA, SpaceCompassViewMode.GLOBE_PANORAMA ->
                    SpaceCompassFloatingGlyph(spaceCompassFloatingInk(26f,.10f,.175f,.90f,.825f)) {
                        SpaceCompassSettingsMenuIcon("panorama-image",LocalContentColor.current,Modifier.size(26.dp),iconStrokeWidth=2.1.dp)
                    }
                SpaceCompassViewMode.CAMERA -> SpaceCompassFloatingGlyph(spaceCompassFloatingVectorInk(R.drawable.ic_camera)) {
                    SpaceCompassFloatingVectorIcon(R.drawable.ic_camera)
                }
                SpaceCompassViewMode.PANORAMA -> SpaceCompassFloatingGlyph(spaceCompassFloatingInk(26f,.10f,.175f,.90f,.825f)) {
                    SpaceCompassSkyModeIcon(LocalContentColor.current,26.dp,2.1.dp)
                }
            }
        }
        SpaceCompassViewModeDialog(expanded,mode,{expanded=false},weather) { value, weatherValue ->
            expanded=false;onWeatherChoose(weatherValue);onChoose(value)
        }
    }
}


/** Mode and projection remain a draft until Apply; dismissing never changes the live view. */
@Composable
internal fun SpaceCompassViewModeDialog(expanded: Boolean,mode: SpaceCompassViewMode,
    onDismiss: ()->Unit,weather: Boolean=true,onChoose: (SpaceCompassViewMode,Boolean)->Unit) {
    if(!expanded) return
    var family by rememberSaveable(mode) { mutableStateOf(
        if(mode==SpaceCompassViewMode.CAMERA) SpaceCompassViewMode.CAMERA
        else if(mode.globe) SpaceCompassViewMode.GLOBE else SpaceCompassViewMode.PANORAMA) }
    var panoramic by rememberSaveable(mode) { mutableStateOf(mode.panoramic) }
    var draftWeather by rememberSaveable(weather) { mutableStateOf(weather) }
    val projectionEnabled=family!=SpaceCompassViewMode.CAMERA
    val selectedPanorama=projectionEnabled&&panoramic
    SpaceCompassAlertDialog(onDismissRequest=onDismiss,
        confirmButton={ SpaceCompassDialogActionButton(stringResource(R.string.view_mode_apply),{
            onChoose(if(family==SpaceCompassViewMode.CAMERA) SpaceCompassViewMode.CAMERA
                else spaceCompassLiveViewMode(family.globe,panoramic),draftWeather)
        },Modifier.testTag("view-mode-apply")) },
        dismissButton={ SpaceCompassDialogActionButton(stringResource(R.string.close),onDismiss,
            Modifier.testTag("view-mode-cancel")) },
        modifier=Modifier.testTag("view-mode-dialog"),
        title={Text(stringResource(R.string.view_mode_choose))},text={
            Column(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().selectableGroup().testTag("view-mode-options")) {
                    listOf(SpaceCompassViewMode.PANORAMA,SpaceCompassViewMode.CAMERA,SpaceCompassViewMode.GLOBE).forEach { value ->
                        val checked=value==family
                        SpaceCompassSettingsRadioRow(checked,{family=value},
                            Modifier.fillMaxWidth().testTag("view-mode-${value.name.lowercase()}")) {
                            val tint=if(checked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            when(value) {
                                SpaceCompassViewMode.GLOBE -> SpaceCompassGlobeIcon(tint,SpaceCompassMenuIconSize,SpaceCompassMenuIconStrokeWidth)
                                SpaceCompassViewMode.CAMERA -> Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_camera),null,
                                    Modifier.size(SpaceCompassMenuIconSize),tint=tint)
                                else -> SpaceCompassSkyModeIcon(tint,SpaceCompassMenuIconSize,SpaceCompassMenuIconStrokeWidth)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(when(value) {
                                SpaceCompassViewMode.CAMERA -> R.string.view_mode_camera
                                SpaceCompassViewMode.GLOBE -> R.string.view_mode_globe
                                else -> R.string.view_mode_sky
                            }),Modifier.weight(1f))
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical=8.dp),color=LocalContentColor.current.copy(alpha=.12f))
                Column(Modifier.fillMaxWidth().selectableGroup().testTag("view-mode-projection")) {
                    for(value in listOf(false,true)) {
                        SpaceCompassSettingsRadioRow(selectedPanorama==value,{panoramic=value},
                            Modifier.fillMaxWidth().testTag(if(value) "view-mode-panoramic" else "view-mode-framed"),
                            enabled=projectionEnabled) {
                            val tint=when {
                                !projectionEnabled -> LocalContentColor.current.copy(alpha=.45f)
                                selectedPanorama==value -> MaterialTheme.colorScheme.primary
                                else -> LocalContentColor.current
                            }
                            SpaceCompassCaptureProjectionIcon(if(value) SpaceCompassCaptureProjection.PANORAMA
                                else SpaceCompassCaptureProjection.FRAME,tint,SpaceCompassMenuIconSize,SpaceCompassMenuIconStrokeWidth)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(if(value) R.string.panorama_preview_title else R.string.capture_projection_frame),
                                Modifier.weight(1f),color=LocalContentColor.current.copy(alpha=if(projectionEnabled) 1f else .45f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SpaceCompassSettingsCheckbox(stringResource(R.string.view_mode_weather),draftWeather,{draftWeather=it},
                    Modifier.padding(horizontal=SpaceCompassSettingsChoiceHorizontalPadding).testTag("view-mode-weather"),
                    enabled=family==SpaceCompassViewMode.PANORAMA,compact=true,leadingIcon={
                        Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_weather),null,
                            Modifier.size(SpaceCompassMenuIconSize),tint=LocalContentColor.current.copy(alpha=if(family==SpaceCompassViewMode.PANORAMA) 1f else .45f))
                    })
            }
        })
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
