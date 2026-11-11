package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Geographic longitude/latitude, not azimuth/elevation. Reserve all four numbered edges for labels. */
internal fun drawSpaceCompassGlobeMapGrid(canvas: Canvas, width: Int, height: Int,
    snapshot: SpaceCompassPanoramaSnapshot): List<SpaceCompassSunSceneFrame> {
    val scale = width / 4096f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, major: Boolean) {
        val thickness = SpaceCompassAngularGridAppearance.lineWidth(width,height) * if(major) 1.5f else 1f
        paint.color = SpaceCompassAngularGridAppearance.shadowArgb; paint.strokeWidth = thickness * 2
        canvas.drawLine(x1,y1,x2,y2,paint)
        paint.color = if(major) SpaceCompassAngularGridAppearance.majorArgb else SpaceCompassAngularGridAppearance.lineArgb
        paint.strokeWidth = thickness; canvas.drawLine(x1,y1,x2,y2,paint)
    }
    for (longitude in -180..180 step 15) {
        val x = (longitude+180)/360f*width
        line(x,0f,x,height.toFloat(),longitude==0)
    }
    for (latitude in -90..90 step 15) {
        val y = (90-latitude)/180f*height
        line(0f,y,width.toFloat(),y,latitude==0)
    }
    val text = spaceCompassOrbitTextPaint(25*scale).apply { color=Color.WHITE }
    fun number(degrees: Int) = (if(degrees>0) "+" else "") +
        formatSpaceCompassNumber(degrees.toDouble(),0,snapshot.formatting.numeric,
            grouping=false,systemLocale=snapshot.formatting.deviceLocale) + "°"
    fun label(value: String, x: Float, baseline: Float) {
        text.setShadowLayer(3*scale,0f,scale,Color.BLACK)
        canvas.drawText(value,x,baseline,text);text.clearShadowLayer()
    }
    for (longitude in -180..180 step 15) {
        val value=number(longitude);text.textAlign=Paint.Align.CENTER
        val half=text.measureText(value)/2
        val x=((longitude+180)/360f*width).coerceIn(half+12*scale,width-half-12*scale)
        label(value,x,32*scale);label(value,x,height-100*scale)
    }
    for (latitude in -90..90 step 15) {
        val baseline=((90-latitude)/180f*height-text.fontMetrics.ascent/2).coerceIn(70*scale,height-140*scale)
        text.textAlign=Paint.Align.LEFT;label(number(latitude),12*scale,baseline)
        text.textAlign=Paint.Align.RIGHT;label(number(latitude),width-12*scale,baseline)
    }
    return listOf(
        SpaceCompassSunSceneFrame(0.0,0.0,width.toDouble(),(80*scale).toDouble()),
        SpaceCompassSunSceneFrame(0.0,(height-160*scale).toDouble(),width.toDouble(),(160*scale).toDouble()),
        SpaceCompassSunSceneFrame(0.0,0.0,(125*scale).toDouble(),height.toDouble()),
        SpaceCompassSunSceneFrame((width-125*scale).toDouble(),0.0,(125*scale).toDouble(),height.toDouble()))
}
