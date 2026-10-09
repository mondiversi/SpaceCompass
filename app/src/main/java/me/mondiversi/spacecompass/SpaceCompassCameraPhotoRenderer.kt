package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import kotlin.math.hypot

/** Reproject onto the whole JPEG; controls, notices and main-page columns never cover the photo. */
internal fun renderSpaceCompassCameraPhoto(context: Context, photo: SpaceCompassCameraPhotoSnapshot,
    snapshot: SpaceCompassPanoramaSnapshot): Bitmap {
    val raw = requireNotNull(BitmapFactory.decodeFile(photo.source.absolutePath))
    val geometry = spaceCompassCameraPhotoGeometry(photo.lens, raw.width, raw.height, photo.displayRotation)
    var image: Bitmap? = null
    try {
        val rotated = if (geometry.rotation==0) raw else Bitmap.createBitmap(raw,0,0,raw.width,raw.height,
            Matrix().apply { postRotate(geometry.rotation.toFloat()) },true)
        image=rotated.copy(Bitmap.Config.ARGB_8888,true)
        if (rotated !== raw) rotated.recycle()
        val result=requireNotNull(image)
        val canvas=Canvas(result)
        val width=result.width; val height=result.height
        val scale=(minOf(width,height)/1080f).coerceAtLeast(.45f)
        val perspective=geometry.perspective
        val attitude=photo.attitude
        val orientation=attitude?.orientation
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
        val text=spaceCompassOrbitTextPaint(25*scale)
        val occupied=mutableListOf(RectF(0f, 0f, width.toFloat(), SPACE_COMPASS_PANORAMA_HEADER_HEIGHT * scale))
        if (orientation!=null) {
            val horizon=projectSpaceCompassSunGround(orientation,SpaceCompassSunSceneFrame(0.0,0.0,width.toDouble(),height.toDouble()),
                width.toDouble(),height.toDouble(),perspective)?.horizon
            if (horizon?.size==2) {
                paint.color=0x99000000.toInt(); paint.strokeWidth=3*scale
                canvas.drawLine(horizon[0].x.toFloat(),horizon[0].y.toFloat(),horizon[1].x.toFloat(),horizon[1].y.toFloat(),paint)
                paint.color=0xddffffff.toInt(); paint.strokeWidth=scale
                canvas.drawLine(horizon[0].x.toFloat(),horizon[0].y.toFloat(),horizon[1].x.toFloat(),horizon[1].y.toFloat(),paint)
            }
        }
        val pointing=orientation.takeIf { attitude?.usable==true }
        if (pointing != null) drawSpaceCompassCameraAngularGrid(canvas, width, height,
            pointing, perspective, snapshot, scale, occupied)
        fun projection(position: SpaceCompassSunPosition) = pointing?.let {
            projectSpaceCompassSun(position,it,width.toDouble(),height.toDouble(),perspective).takeIf { p->p.visible }
        }
        val observations = snapshot.objects.associateWith { item -> runCatching {
            calculateSpaceCompassCelestialObservation(item.body,snapshot.timeMs,snapshot.latitude,snapshot.longitude,
                snapshot.altitude,snapshot.remote)
        }.getOrNull()?.position }
        observations.forEach { (item,position) -> position?.let(::projection)?.let {
            occupied+=RectF(it.x.toFloat()-48*scale,it.y.toFloat()-(if(snapshot.showPointLabels) 70 else 48)*scale,it.x.toFloat()+48*scale,it.y.toFloat()+45*scale)
            if (snapshot.showPointLabels) {
                text.textSize=23*scale
                val half=text.measureText(item.name)/2
                val x=it.x.toFloat().coerceIn(half+5*scale,width-half-5*scale)
                val y=(it.y.toFloat()-45*scale).coerceAtLeast(30*scale)
                val metrics=text.fontMetrics
                occupied+=RectF(x-half-6*scale,y+metrics.ascent-6*scale,x+half+6*scale,y+metrics.descent+6*scale)
            }
        } }
        // Keep guide captions below the photograph's header and away from current object images.
        val referenceDrawing = if (snapshot.showSkyReferences && pointing != null && snapshot.observerPositionKnown)
            drawSpaceCompassSkyReferences(canvas, snapshot.latitude, snapshot.timeMs, width, height, scale, occupied,
                pointing, perspective, SPACE_COMPASS_PANORAMA_HEADER_HEIGHT * scale, showLabels = snapshot.showPointLabels,
                referenceNames = snapshot.referenceNames, poleNames = snapshot.poleNames,
                numericFormat = snapshot.formatting.numeric, systemLocale = snapshot.formatting.deviceLocale,
                observerPointNames = snapshot.observerPointNames, observerAltitude = snapshot.altitude)
        else SpaceCompassSkyReferenceDrawing(emptyList(), emptyList())
        val points=mutableListOf<Triple<SpaceCompassSunPathPoint,SpaceCompassSunScenePoint,Int>>()
        if (pointing!=null) snapshot.objects.forEach { item -> item.path?.let { path ->
            val tint=spaceCompassCelestialPathTint(item.body)
            val segments=projectSpaceCompassSunDailyPath(path,pointing,width.toDouble(),height.toDouble(),perspective)
            for (below in listOf(false,true)) {
                val curve=Path(); var end: SpaceCompassSunScenePoint?=null
                segments.filter { it.belowHorizon==below }.forEach { segment ->
                    if (end==null || hypot(end!!.x-segment.start.x,end!!.y-segment.start.y)>.1)
                        curve.moveTo(segment.start.x.toFloat(),segment.start.y.toFloat())
                    curve.lineTo(segment.end.x.toFloat(),segment.end.y.toFloat()); end=segment.end
                }
                paint.style=Paint.Style.STROKE; paint.pathEffect=if(below) DashPathEffect(floatArrayOf(6*scale,4*scale),0f) else null
                paint.strokeWidth=4*scale; paint.color=0x77000000; canvas.drawPath(curve,paint)
                paint.strokeWidth=2*scale; paint.color=spaceCompassCelestialPathLineTint(tint,below).toArgb(); canvas.drawPath(curve,paint)
            }
            paint.pathEffect=null
            spaceCompassSunPathDirections(path).mapNotNull {
                projectSpaceCompassSunPathArrow(it,pointing,width.toDouble(),height.toDouble(),6.0*scale,perspective)
            }.forEach { arrow ->
                val chevron=Path().apply { moveTo(arrow.left.x.toFloat(),arrow.left.y.toFloat())
                    lineTo(arrow.tip.x.toFloat(),arrow.tip.y.toFloat()); lineTo(arrow.right.x.toFloat(),arrow.right.y.toFloat()) }
                paint.style=Paint.Style.STROKE; paint.strokeWidth=2*scale
                paint.color=spaceCompassCelestialPathVisibilityTint(tint,arrow.belowHorizon).toArgb(); canvas.drawPath(chevron,paint)
                text.textSize=22*scale
                if (snapshot.showPointLabels) drawSpaceCompassOrbitName(canvas,item.name,segments,arrow,text,paint.color,12*scale,occupied)
            }
            path.markers.forEach { marker -> projection(marker.position)?.let {
                points+=Triple(marker,SpaceCompassSunScenePoint(it.x,it.y),spaceCompassCelestialPathVisibilityTint(tint,marker.position.elevationDegrees<0).toArgb())
            } }
        } }
        points.forEach { (marker,point,tint) ->
            drawSpaceCompassPanoramaEventMarker(canvas,marker.event,point,scale*.7f,tint)
            if (marker.events.any { it != SpaceCompassSunPathEvent.HOUR }) {
                val radius = 17*scale
                occupied += RectF(point.x.toFloat()-radius, point.y.toFloat()-radius,
                    point.x.toFloat()+radius, point.y.toFloat()+radius)
            }
        }
        if (snapshot.showPointLabels) {
            val labels=points.map { (marker,point,tint) ->
                val time=snapshot.markerTimes[marker.timeMs] ?: java.time.Instant.ofEpochMilli(marker.timeMs)
                    .atZone(java.time.ZoneOffset.UTC).toLocalTime().let { "%02d:%02d".format(java.util.Locale.ROOT,it.hour,it.minute) }
                SpaceCompassPanoramaTimeLabel(formatSpaceCompassPanoramaPathPointLabel(time,marker,snapshot.formatting,snapshot.pointEventNames),
                    point,tint,marker.events.any { it != SpaceCompassSunPathEvent.HOUR })
            } + observations.mapNotNull { (item,position) -> position?.let(::projection)?.let { p ->
                spaceCompassPanoramaCurrentLabel(snapshot, position, SpaceCompassSunScenePoint(p.x,p.y),
                    spaceCompassCelestialPathTint(item.body).toArgb(), 35f)
            } }
            drawSpaceCompassPanoramaTimeLabels(canvas,labels,width,height,scale,occupied)
        }
        paint.pathEffect=null
        drawSpaceCompassSkyReferenceLabels(canvas, referenceDrawing, scale)
        observations.forEach { (item,position) -> position?.let(::projection)?.let { p ->
            val x=p.x.toFloat(); val y=p.y.toFloat(); val radius=35*scale
            paint.style=Paint.Style.FILL; paint.color=0xcc07101b.toInt(); canvas.drawCircle(x,y,radius,paint)
            paint.style=Paint.Style.STROKE; paint.strokeWidth=2*scale; paint.color=spaceCompassCelestialPathTint(item.body).toArgb()
            canvas.drawCircle(x,y,radius,paint)
            val marker=renderSpaceCompassPanoramaMarker(context,item.body,snapshot.timeMs)
            try { canvas.drawBitmap(marker,null,RectF(x-radius+2*scale,y-radius+2*scale,x+radius-2*scale,y+radius-2*scale),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) } finally { marker.recycle() }
            if (snapshot.showPointLabels) {
                text.textSize=23*scale; text.style=Paint.Style.FILL; text.color=paint.color
                text.setShadowLayer(3*scale,0f,scale,Color.BLACK)
                val half=text.measureText(item.name)/2
                canvas.drawText(item.name,x.coerceIn(half+5*scale,width-half-5*scale),(y-radius-10*scale).coerceAtLeast(30*scale),text)
                text.clearShadowLayer()
            }
        } }
        // The shared translucent header overlays the photograph instead of cropping camera pixels.
        val header=SPACE_COMPASS_PANORAMA_HEADER_HEIGHT*scale
        drawSpaceCompassPanoramaCaption(canvas,snapshot.caption,width,scale)
        if (pointing==null && photo.warning!=null) {
            text.textSize=20*scale; text.color=Color.WHITE; text.textAlign=Paint.Align.CENTER
            canvas.drawText(photo.warning,width/2f,header+28*scale,text)
        }
        image=null
        return result
    } finally { raw.recycle(); image?.recycle() }
}
