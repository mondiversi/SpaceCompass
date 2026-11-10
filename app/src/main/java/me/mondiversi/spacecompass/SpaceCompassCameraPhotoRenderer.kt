package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.*
import androidx.compose.ui.graphics.toArgb
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Reproject onto the whole JPEG; controls, notices and main-page columns never cover the photo. */
internal fun renderSpaceCompassCameraPhoto(context: Context, photo: SpaceCompassCameraPhotoSnapshot,
    snapshot: SpaceCompassPanoramaSnapshot): Bitmap {
    val raw=requireNotNull(BitmapFactory.decodeFile(photo.source.absolutePath))
    try {
        val geometry=spaceCompassCameraPhotoGeometry(photo.lens,raw.width,raw.height,photo.displayRotation)
        val rotated=if(geometry.rotation==0) raw else Bitmap.createBitmap(raw,0,0,raw.width,raw.height,
            Matrix().apply { postRotate(geometry.rotation.toFloat()) },true)
        try { return renderSpaceCompassProjectedCapture(context,rotated,geometry.perspective,photo.attitude?.orientation,
            photo.attitude?.usable==true,snapshot,photo.objectOpacity,photo.warning) }
        finally { if(rotated!==raw) rotated.recycle() }
    } finally { raw.recycle() }
}

/** Shared angular overlays: photo pixels and virtual-sky pixels keep their own frozen optical frame. */
internal fun renderSpaceCompassProjectedCapture(context: Context,background: Bitmap,perspective: SpaceCompassPerspective,
    orientation: SpaceCompassSunOrientation?,usable: Boolean,snapshot: SpaceCompassPanoramaSnapshot,
    objectOpacity: Float=1f,warning: String?=null): Bitmap {
    var image: Bitmap?=null
    try {
        val width=background.width;val height=background.height
        val scale=(minOf(width,height)/1080f).coerceAtLeast(.45f)
        val bandStyle=spaceCompassCaptureBandStyle(width,height)
        val caption=prepareSpaceCompassCaptureCaption(snapshot.caption,width,bandStyle,context)
        val observations = snapshot.objects.associateWith { item -> runCatching {
            if (item.comparison?.hasReference == false) null else
                calculateSpaceCompassCelestialObservation(item.body,item.referenceTime(snapshot.timeMs),snapshot.latitude,snapshot.longitude,
                snapshot.altitude,snapshot.remote)
        }.getOrNull()?.position }
        val legend=prepareSpaceCompassPanoramaLegend(snapshot.objects.filter {
            it.path != null || observations[it] != null
        },width,bandStyle)
        image=Bitmap.createBitmap(width,caption.height+height+legend.height,Bitmap.Config.ARGB_8888).apply { density=background.density }
        Canvas(requireNotNull(image)).drawBitmap(background,0f,caption.height.toFloat(),null)
        val result=requireNotNull(image)
        val canvas=Canvas(result)
        // Metadata and legend have their own space; every lens ray stays inside the unchanged photo frame.
        canvas.save(); canvas.translate(0f,caption.height.toFloat()); canvas.clipRect(0,0,width,height)
        val header=0f
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND }
        val text=spaceCompassOrbitTextPaint(25*scale)
        val orbitNamePlacement=SpaceCompassCurveCaptionPlacementState()
        val occupied=mutableListOf<RectF>()
        if (orientation!=null && snapshot.showGrid) {
            val horizon=projectSpaceCompassSunGround(orientation,SpaceCompassSunSceneFrame(0.0,0.0,width.toDouble(),height.toDouble()),
                width.toDouble(),height.toDouble(),perspective)?.horizon
            if (horizon?.size==2) {
                paint.color=SpaceCompassAngularGridAppearance.shadowArgb; paint.strokeWidth=3*SpaceCompassAngularGridAppearance.lineWidth(width,height)
                canvas.drawLine(horizon[0].x.toFloat(),horizon[0].y.toFloat(),horizon[1].x.toFloat(),horizon[1].y.toFloat(),paint)
                paint.color=SpaceCompassAngularGridAppearance.majorArgb; paint.strokeWidth=1.5f*SpaceCompassAngularGridAppearance.lineWidth(width,height)
                canvas.drawLine(horizon[0].x.toFloat(),horizon[0].y.toFloat(),horizon[1].x.toFloat(),horizon[1].y.toFloat(),paint)
            }
        }
        val pointing=orientation.takeIf { usable }
        if (pointing != null && snapshot.showGrid) drawSpaceCompassCameraAngularGrid(canvas, width, height,
            pointing, perspective, snapshot, scale, header, occupied)
        fun projection(position: SpaceCompassSunPosition) = pointing?.let {
            projectSpaceCompassSun(position,it,width.toDouble(),height.toDouble(),perspective).takeIf { p->p.visible }
        }
        if (pointing != null && snapshot.observerPositionKnown) snapshot.objects.filter {
            it.body.constellationPattern != null
        }.forEach { item ->
            drawSpaceCompassSouthernCross(canvas, calculateSpaceCompassConstellation(item.body, item.referenceTime(snapshot.timeMs),
                snapshot.latitude, snapshot.longitude, snapshot.altitude), width, height, scale, pointing, perspective,
                showNames = snapshot.labelMode.showsObject(snapshot.isActive(item)), body = item.body)
        }
        val selections = resolveSpaceCompassPanoramaSelections(snapshot,
            observations.mapNotNull { (item, position) -> position?.takeIf { item.comparison == null }?.let { item.body to it } }.toMap())
        observations.forEach { (item,position) -> position?.let(::projection)?.let {
            occupied+=RectF(it.x.toFloat()-48*scale,it.y.toFloat()-(if(snapshot.labelMode.showsObject(snapshot.isActive(item))) 70 else 48)*scale,it.x.toFloat()+48*scale,it.y.toFloat()+45*scale)
            if (snapshot.labelMode.showsObject(snapshot.isActive(item))) {
                text.textSize=23*scale
                val half=text.measureText(item.name)/2
                val x=it.x.toFloat().coerceIn(half+5*scale,width-half-5*scale)
                val y=(it.y.toFloat()-45*scale).coerceAtLeast(30*scale)
                val metrics=text.fontMetrics
                occupied+=RectF(x-half-6*scale,y+metrics.ascent-6*scale,x+half+6*scale,y+metrics.descent+6*scale)
            }
        } }
        // Keep guide captions inside the lens frame and away from current object images.
        val referenceDrawing = if (snapshot.showSkyReferences && pointing != null && snapshot.observerPositionKnown)
            drawSpaceCompassSkyReferences(canvas, snapshot.latitude, snapshot.timeMs, width, height, scale, occupied,
                pointing, perspective, header, showLabels = true,
                referenceNames = snapshot.referenceNames, poleNames = snapshot.poleNames,
                numericFormat = snapshot.formatting.numeric, systemLocale = snapshot.formatting.deviceLocale,
                observerPointNames = snapshot.observerPointNames, observerAltitude = snapshot.altitude)
        else SpaceCompassSkyReferenceDrawing(emptyList(), emptyList())
        val points=mutableListOf<Triple<SpaceCompassSunPathPoint,SpaceCompassSunScenePoint,Int>>()
        val labels=mutableListOf<SpaceCompassPanoramaTimeLabel>()
        if (pointing!=null) snapshot.objects.forEach { item -> item.path?.let { path ->
            val tint=item.tint
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
                if (snapshot.showsOrbitName(item,selections)) drawSpaceCompassOrbitName(canvas,item.name,segments,arrow,
                    text,paint.color,12*scale,occupied,width.toFloat(),height.toFloat(),orbitNamePlacement,
                    "${item.body}:${item.comparison?.id}:${arrow.center}",projection=perspective)
            }
            path.markers.forEach { marker -> projection(marker.position)?.let {
                val point = SpaceCompassSunScenePoint(it.x,it.y)
                val markerTint = spaceCompassCelestialPathVisibilityTint(tint,marker.position.elevationDegrees<0).toArgb()
                points+=Triple(marker,point,markerTint)
                if (snapshot.labelMode.showsPathPoint(marker, snapshot.isActive(item)) &&
                    !spaceCompassPanoramaPointSelected(selections, item.body, marker, item.comparison?.id) &&
                    !snapshot.hasComparisonAnchorLabel(item, marker, observations[item], selections)) {
                    val time=(if (item.comparison == null) snapshot.markerTimes[marker.timeMs] else null) ?: formatSpaceCompassPanoramaMarkerTime(marker.timeMs,item.referenceTime(snapshot.timeMs),
                        item.displayTimeZone(snapshot),snapshot.formatting)
                    labels += SpaceCompassPanoramaTimeLabel(formatSpaceCompassPanoramaPathPointLabel(time,marker,
                        snapshot.formatting,snapshot.pointEventNames,item.referenceTime(snapshot.timeMs),snapshot.bodyNames),point,markerTint,
                        marker.events.any { it != SpaceCompassSunPathEvent.HOUR })
                }
            } }
        } }
        points.forEach { (marker,point,tint) ->
            if (SpaceCompassSunPathEvent.APPROACH !in marker.events)
                drawSpaceCompassPanoramaEventMarker(canvas,marker.event,point,scale*.7f,tint)
            if (marker.events.any { it != SpaceCompassSunPathEvent.HOUR }) {
                val radius = 17*scale
                occupied += RectF(point.x.toFloat()-radius, point.y.toFloat()-radius,
                    point.x.toFloat()+radius, point.y.toFloat()+radius)
            }
        }
        labels += observations.mapNotNull { (item,position) ->
            if (!snapshot.labelMode.showsCurrentPoint(snapshot.isActive(item)) ||
                position != null && snapshot.comparisonAnchorSelected(item, position, selections)) null
            else position?.let(::projection)?.let { p ->
                spaceCompassPanoramaCurrentLabel(snapshot.forCurrentInstance(item.comparison), position, SpaceCompassSunScenePoint(p.x,p.y),
                    item.tint.toArgb(), 35f)
            }
        }
        if (snapshot.labelMode.showsSelected) labels += spaceCompassPanoramaSelectionLabels(snapshot,selections,scale,35f,
            { position -> projection(position)?.let { SpaceCompassSunScenePoint(it.x,it.y) } },occupied)
        drawSpaceCompassPanoramaTimeLabels(canvas,labels,width,height,scale,occupied)
        paint.pathEffect=null
        drawSpaceCompassSkyReferenceLabels(canvas, referenceDrawing, scale)
        observations.forEach { (item,position) -> position?.let(::projection)?.let { p ->
            val x=p.x.toFloat(); val y=p.y.toFloat(); val radius=35*scale
            if (item.body.constellationPattern == null) {
            // Composite the image, plate and rim once, matching the live thumbnail's alpha layer.
            val layer = if (objectOpacity < 1f) canvas.saveLayerAlpha(
                x-radius-3*scale,y-radius-3*scale,x+radius+3*scale,y+radius+3*scale,
                (objectOpacity*255).roundToInt()) else canvas.save()
            try {
            paint.style=Paint.Style.FILL; paint.color=0xcc07101b.toInt(); canvas.drawCircle(x,y,radius,paint)
            paint.style=Paint.Style.STROKE; paint.strokeWidth=2*scale; paint.color=item.tint.toArgb()
            canvas.drawCircle(x,y,radius,paint)
            val marker=renderSpaceCompassPanoramaMarker(context,item.body,item.referenceTime(snapshot.timeMs))
            try { canvas.drawBitmap(marker,null,RectF(x-radius+2*scale,y-radius+2*scale,x+radius-2*scale,y+radius-2*scale),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) } finally { marker.recycle() }
            } finally { canvas.restoreToCount(layer) }
            }
            if (snapshot.labelMode.showsObject(snapshot.isActive(item))) {
                text.textSize=23*scale; text.style=Paint.Style.FILL; text.color=item.tint.toArgb()
                text.setShadowLayer(3*scale,0f,scale,Color.BLACK)
                val half=text.measureText(item.name)/2
                canvas.drawText(item.name,x.coerceIn(half+5*scale,width-half-5*scale),(y-radius-10*scale).coerceAtLeast(30*scale),text)
                text.clearShadowLayer()
            }
        } }
        drawSpaceCompassCaptureApproachMarkers(canvas, points, scale * .7f)
        drawSpaceCompassCurrentApproaches(canvas,snapshot, observations.mapNotNull { (item,position) ->
            position?.takeIf { item.comparison == null }?.let { item.body to it } }.toMap(),scale) { position -> projection(position)?.let {
                SpaceCompassSunScenePoint(it.x,it.y) } }
        drawSpaceCompassPanoramaSelections(canvas,snapshot,selections,scale,35f,
            { position -> projection(position)?.let { SpaceCompassSunScenePoint(it.x,it.y) } })
        if (pointing==null && warning!=null) {
            text.textSize=20*scale; text.color=Color.WHITE; text.textAlign=Paint.Align.CENTER
            canvas.drawText(warning,width/2f,header+28*scale,text)
        }
        canvas.restore()
        caption.draw(canvas)
        legend.draw(canvas,(caption.height+height).toFloat(),0xff101418.toInt())
        image=null
        return result
    } finally { image?.recycle() }
}
