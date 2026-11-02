package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Generated black-hole symbol only: no position, camera, downloadable assets or saved preferences. */
class SpaceCompassCurrentOutlineUiTest {
    @get:Rule val compose = createComposeRule()
    private val body=SpaceCompassCelestialBody.SAGITTARIUS_A
    private val zone=ZoneId.of("UTC")
    private val date=LocalDate.parse("2026-11-02")
    private val point=SpaceCompassSunPathPoint(date.atTime(12,0).atZone(zone).toInstant().toEpochMilli(),
        SpaceCompassSunPosition(180.0,30.0))
    private val path=SpaceCompassSunDailyPath(date,zone,listOf(point),listOf(point),body)
    private val state=SpaceCompassSunDailyPathUiState()
    private val night=mutableStateOf(false)
    private val opacity=mutableStateOf(1f)
    private val edge=mutableStateOf(false)
    private fun accent()=if(night.value) Color(0xFF66CCFF) else Color(0xFF0066FF)
    private fun show() {
        compose.setContent {
            val color=accent()
            MaterialTheme(colorScheme=if(night.value) darkColorScheme(primary=color) else lightColorScheme(primary=color)) {
                Box(Modifier.size(240.dp).background(Color(0xFF304050))) {
                    if(edge.value) SpaceCompassCelestialOffscreenMarker(body,
                        SpaceCompassSunProjection(false,300.0,120.0,90.0),false,Color.White,0,
                        objectOpacity=opacity.value,selected=state.isCurrentSelected(body,point,zone))
                    else SpaceCompassCelestialLiveMarker(body,SpaceCompassSunProjection(true,120.0,120.0,0.0),
                        false,point,state,path=path,objectOpacity=opacity.value)
                }
            }
        }
    }
    private fun borderPixels(tag:String,color:Color):Int {
        val bitmap=compose.onNodeWithTag(tag,useUnmergedTree=true).captureToImage().asAndroidBitmap()
        val pixels=IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        return pixels.count { it==color.toArgb() }
    }
    @Test fun selectedCurrentPositionKeepsItsOneOpaqueAccentOutlineWithTransparentImages() {
        show()
        for(dark in listOf(false,true)) for(alpha in listOf(1f,.1f,0f)) {
            compose.runOnIdle { night.value=dark;opacity.value=alpha;state.selectCurrent(body,point,path) }
            assertTrue(borderPixels("celestial-live-preview-${body.name}",accent())>10)
            compose.runOnIdle { state.toggleCurrent(body,point,path) }
            assertEquals(0,borderPixels("celestial-live-preview-${body.name}",accent()))
        }
    }
    @Test fun edgeLocatorUsesTheSameSelectedCurrentOutlineInBothThemes() {
        edge.value=true;show()
        for(dark in listOf(false,true)) {
            compose.runOnIdle { night.value=dark;opacity.value=.1f;state.selectCurrent(body,point,path) }
            assertTrue(borderPixels("celestial-offscreen-preview-${body.name}",accent())>10)
            compose.runOnIdle { state.clearSelection() }
            assertEquals(0,borderPixels("celestial-offscreen-preview-${body.name}",accent()))
        }
    }
}
