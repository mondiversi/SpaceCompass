package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import kotlin.math.roundToInt

internal const val SPACE_COMPASS_GLOBE_LOCATION_AZURE=0xff55c8db.toInt()

/** Same colour/filter/opacity as the screensaver SVG; independent of the application theme. */
internal fun spaceCompassGlobeLocationMarkerColor(depth: Double,rgb: Int=SPACE_COMPASS_GLOBE_LOCATION_AZURE): Int {
    val front=((depth+.08)/.16).coerceIn(0.0,1.0)
    val red=(rgb ushr 16) and 255;val green=(rgb ushr 8) and 255;val blue=rgb and 255
    val grey=.2126*red+.7152*green+.0722*blue
    fun channel(value: Int)=(grey*(1-front)+value*front).roundToInt().coerceIn(0,255)
    val alpha=((.35+.65*front)*255).roundToInt()
    return (alpha shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)
}

private val globeLocationPinPath: Path by lazy {
    requireNotNull(PathParser.createPathFromPathData("M12 31C10 26 2 19 2 12a10 10 0 1 1 20 0c0 7-8 14-10 19Z"))
}

internal fun drawSpaceCompassGlobeLocationMarker(canvas: Canvas,x: Float,y: Float,width: Float,depth: Double) {
    canvas.save();canvas.translate(x-width/2,y-width*32/24);canvas.scale(width/24,width/24)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color=spaceCompassGlobeLocationMarkerColor(depth)
    paint.setShadowLayer(1.2f,0f,.8f,0x66000000)
    canvas.drawPath(globeLocationPinPath,paint);paint.clearShadowLayer()
    paint.style=Paint.Style.STROKE;paint.strokeWidth=1.5f
    paint.color=spaceCompassGlobeLocationMarkerColor(depth,0xffffffff.toInt());canvas.drawPath(globeLocationPinPath,paint)
    paint.style=Paint.Style.FILL;paint.color=spaceCompassGlobeLocationMarkerColor(depth,0xff10242d.toInt())
    canvas.drawCircle(12f,12f,3.5f,paint);canvas.restore()
}
