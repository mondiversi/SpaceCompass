package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import kotlin.math.*

/** Rebuild only the captured rays; sensor, panel and zoom changes after the shutter cannot alter this image. */
internal fun renderSpaceCompassSkyFrame(context: Context,snapshot: SpaceCompassPanoramaSnapshot,maxEdge: Int): Bitmap {
    val frame=requireNotNull(snapshot.skyFraming)
    val (width,height)=spaceCompassCaptureSize(frame.width,frame.height,maxEdge)
    val smallW=minOf(width,1024);val smallH=(height.toDouble()*smallW/width).roundToInt().coerceAtLeast(1)
    val pixels=IntArray(smallW*smallH)
    val sky=spaceCompassSunSkyColors(snapshot.phase,snapshot.weather,snapshot.solarLighting)
    val ground=spaceCompassSunGroundPalette(snapshot.phase,snapshot.solarLighting)
    val opacity=if(snapshot.observerPositionKnown) spaceCompassStarVisibility(
        calculateSpaceCompassSunPosition(snapshot.timeMs,snapshot.latitude,snapshot.longitude,snapshot.altitude).elevationDegrees,snapshot.weather) else 0f
    val basis=if(opacity>.001f) spaceCompassStarBasis(snapshot.timeMs,snapshot.latitude,snapshot.longitude,snapshot.altitude) else null
    val atlas=if(basis!=null) loadSpaceCompassStarAtlas(context) else null
    val aw=atlas?.width ?: 0;val ah=atlas?.height ?: 0
    val stars=atlas?.let { IntArray(aw*ah).also { values -> it.getPixels(values,0,aw,0,0,aw,ah);it.recycle() } }
    for(y in 0 until smallH) for(x in 0 until smallW) {
        val right=(x+.5-frame.perspective.principalX*smallW)/frame.perspective.focalX(smallW.toDouble())
        val up=-(y+.5-frame.perspective.principalY*smallH)/frame.perspective.focalY(smallH.toDouble())
        val o=frame.orientation
        val ray=SpaceCompassSunVector(o.forward.east+o.right.east*right+o.screenUp.east*up,
            o.forward.north+o.right.north*right+o.screenUp.north*up,o.forward.up+o.right.up*right+o.screenUp.up*up).normalized()
        val elevation=asin(ray.up.coerceIn(-1.0,1.0))/(PI/2)
        val a=if(elevation>=0) sky.second.toArgb() else ground.farArgb
        val b=if(elevation>=0) sky.first.toArgb() else ground.nearArgb
        val t=abs(elevation).coerceIn(0.0,1.0)
        var star=0;var alpha=0.0
        if(ray.up>0&&basis!=null&&stars!=null) {
            val uv=spaceCompassStarUv(basis.transform(ray))
            star=stars[(uv.second*ah).toInt().coerceIn(0,ah-1)*aw+(uv.first*aw).toInt().coerceIn(0,aw-1)]
            alpha=opacity*spaceCompassStarHorizonOpacity(ray.up)
        }
        val peak=max(Color.red(star),max(Color.green(star),Color.blue(star)))/255.0
        fun channel(shift: Int): Int {
            val base=((a ushr shift and 255)*(1-t)+(b ushr shift and 255)*t)
            return (base*(1-peak*alpha)+(star ushr shift and 255)*alpha).roundToInt().coerceIn(0,255)
        }
        pixels[y*smallW+x]=Color.rgb(channel(16),channel(8),channel(0))
    }
    val small=Bitmap.createBitmap(pixels,smallW,smallH,Bitmap.Config.ARGB_8888)
    val background=Bitmap.createScaledBitmap(small,width,height,true)
    try { return renderSpaceCompassProjectedCapture(context,background,frame.perspective,frame.orientation,true,snapshot) }
    finally { if(background!==small) background.recycle();small.recycle() }
}

internal fun renderSpaceCompassCaptureView(context: Context,snapshot: SpaceCompassPanoramaSnapshot,maxEdge: Int,
    projection: SpaceCompassCaptureProjection): Bitmap = when {
    snapshot.globe!=null -> renderSpaceCompassGlobe(snapshot,maxEdge,context,projection==SpaceCompassCaptureProjection.PANORAMA)
    projection==SpaceCompassCaptureProjection.FRAME -> renderSpaceCompassSkyFrame(context,snapshot,maxEdge)
    else -> renderSpaceCompassPanorama(snapshot,maxEdge,context)
}
