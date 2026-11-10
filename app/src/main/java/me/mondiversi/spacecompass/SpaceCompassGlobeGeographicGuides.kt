package me.mondiversi.spacecompass

import android.graphics.*
import kotlin.math.*

/** Frozen terrestrial guide names share the sky capture's shaped curve lettering and outline. */
internal fun drawSpaceCompassGlobeGeographicGuides(canvas: Canvas,snapshot: SpaceCompassPanoramaSnapshot,
    width: Int,height: Int,radius: Double,centerY: Double,flat: Boolean,
    reserved: List<SpaceCompassSunSceneFrame>): SpaceCompassSkyReferenceDrawing {
    val capture=requireNotNull(snapshot.globe);val pose=capture.pose;val scale=width/4096f
    val baseTextSize=spaceCompassGlobeGuideTextSize(width,height,capture.framing);val textScale=baseTextSize/24f
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth=1.8f*scale;strokeCap=Paint.Cap.ROUND }
    val text=spaceCompassOrbitTextPaint(baseTextSize)
    val occupied=reserved.toMutableList();val labels=mutableListOf<SpaceCompassSkyReferenceCurveLabel>()
    fun degrees(value: Double)=(if(value>0) "+" else "")+formatSpaceCompassNumber(value,if(value==0.0||abs(value)==90.0) 0 else 1,
        snapshot.formatting.numeric,grouping=false,systemLocale=snapshot.formatting.deviceLocale)+"°"
    val poles=mutableListOf<SpaceCompassSunScenePoint>();val poleLabels=mutableListOf<SpaceCompassSkyReferenceLabel>()
    for(latitude in listOf(90.0,-90.0)) {
        val v=spaceCompassGlobeView(SpaceCompassGlobeVector(0.0,latitude/90,0.0),pose)
        if(!flat && v.z< -1e-6) continue
        val p=if(flat) SpaceCompassSunScenePoint(width/2.0,if(latitude>0) 0.0 else height.toDouble())
            else SpaceCompassSunScenePoint(width/2+v.x*radius,centerY-v.y*radius)
        if(p.x !in 0.0..width.toDouble()||p.y !in 0.0..height.toDouble()) continue
        poles+=p
        val value=(if(latitude>0) snapshot.poleNames.first else snapshot.poleNames.second)+"  "+degrees(latitude)
        text.textSize=baseTextSize
        val w=text.measureText(value)+16*textScale;val h=36*textScale
        val options=listOf(16f,52f,104f,184f).flatMap { gap -> listOf(p.y-gap*textScale-h,p.y+gap*textScale) }
        val left=(p.x-w/2).coerceIn(0.0,(width-w).coerceAtLeast(0f).toDouble())
        val bounds=options.map { SpaceCompassSunSceneFrame(left,it,w.toDouble(),h.toDouble()) }.firstOrNull { b ->
            b.left>=0&&b.left+b.width<=width&&b.top>=0&&b.top+b.height<=height&&occupied.none {
                b.left<it.left+it.width&&b.left+b.width>it.left&&b.top<it.top+it.height&&b.top+b.height>it.top
            }
        } ?: continue
        occupied+=bounds
        poleLabels+=SpaceCompassSkyReferenceLabel(value,RectF(bounds.left.toFloat(),bounds.top.toFloat(),
            (bounds.left+bounds.width).toFloat(),(bounds.top+bounds.height).toFloat()),p,0xffd8e7f2.toInt())
    }
    spaceCompassGlobeGeographicParallels.forEach { (latitude,reference) ->
        val geometry=spaceCompassGlobeGuideSegments(latitude,pose,flat,width.toDouble(),height.toDouble(),radius,centerY)
        for((segments,alpha) in listOf(geometry.front to 210,geometry.back to 42)) {
            paint.color=0xffa8bdd0.toInt();paint.alpha=alpha
            segments.forEach { canvas.drawLine(it.start.x.toFloat(),it.start.y.toFloat(),it.end.x.toFloat(),it.end.y.toFloat(),paint) }
        }
        val candidates=geometry.front.map { SpaceCompassSunScenePoint((it.start.x+it.end.x)/2,(it.start.y+it.end.y)/2) }
            .sortedBy { abs(it.x-width*.45)+abs(it.y-height*.45)*.1 }
        val name=snapshot.referenceNames[reference].orEmpty()
        if(name.isEmpty()) return@forEach
        var chosen: SpaceCompassSkyReferenceCurveLabel?=null
        for(size in listOf(24f,22f,20f)) {
            if(chosen!=null) break
            text.textSize=size*textScale
            for(value in listOf("$name  ${degrees(latitude)}",name)) {
                val layout=spaceCompassChooseCurveTextLayout(geometry.front,candidates,text.measureText(value).toDouble(),
                    text.textSize.toDouble(),(16*textScale).toDouble(),width.toDouble(),height.toDouble(),
                    occupied=occupied,preciseClearance=true) ?: continue
                chosen=SpaceCompassSkyReferenceCurveLabel(value,layout,0xffd8e7f2.toInt(),text.textSize);break
            }
        }
        chosen?.let { labels+=it;occupied+=it.layout.collisionFrames }
    }
    return SpaceCompassSkyReferenceDrawing(poleLabels,poles,labels)
}

internal fun drawSpaceCompassGlobeGeographicCaptions(canvas: Canvas,drawing: SpaceCompassSkyReferenceDrawing,textSize: Float) {
    drawing.curveLabels.forEach { drawSpaceCompassCurveText(canvas,it.value,it.layout.baseline,spaceCompassOrbitTextPaint(it.textSize),it.tint) }
    val paint=spaceCompassOrbitTextPaint(textSize)
    val scale=textSize/24f
    drawing.poles.forEach { p ->
        val path=Path().apply { moveTo((p.x-6*scale).toFloat(),p.y.toFloat());lineTo((p.x+6*scale).toFloat(),p.y.toFloat())
            moveTo(p.x.toFloat(),(p.y-6*scale).toFloat());lineTo(p.x.toFloat(),(p.y+6*scale).toFloat()) }
        paint.style=Paint.Style.STROKE;paint.strokeWidth=5*scale;paint.color=Color.BLACK;canvas.drawPath(path,paint)
        paint.strokeWidth=2*scale;paint.color=0xffd8e7f2.toInt();canvas.drawPath(path,paint)
    }
    drawing.labels.forEach { label ->
        val y=label.anchor.y.coerceIn(label.bounds.top.toDouble(),label.bounds.bottom.toDouble())
        if(kotlin.math.abs(y-label.anchor.y)>textSize*1.2) {
            // A flat map collapses each pole onto a whole edge. Keep a distant caption linked
            // to that edge instead of suggesting that the pole lies at a lower latitude.
            paint.color=label.tint;paint.alpha=180;paint.style=Paint.Style.STROKE;paint.strokeWidth=textSize*.03f
            canvas.drawLine(label.anchor.x.toFloat(),label.anchor.y.toFloat(),label.bounds.left+textSize*.2f,y.toFloat(),paint)
            paint.alpha=255
        }
        val baseline=label.bounds.centerY()-(paint.fontMetrics.ascent+paint.fontMetrics.descent)/2
        paint.style=Paint.Style.STROKE;paint.strokeWidth=paint.textSize*.12f;paint.color=Color.BLACK
        canvas.drawText(label.value,label.bounds.centerX(),baseline,paint)
        paint.style=Paint.Style.FILL;paint.color=label.tint;canvas.drawText(label.value,label.bounds.centerX(),baseline,paint)
    }
}
