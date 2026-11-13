package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.*

internal data class SpaceCompassGlobeCoordinateLabel(val value: String,val anchor: SpaceCompassSunScenePoint,
    val bounds: SpaceCompassSunSceneFrame,val pole: Boolean = false)
internal data class SpaceCompassGlobeCoordinateDrawing(val labels: List<SpaceCompassGlobeCoordinateLabel> = emptyList(),
    val textSize: Float = 0f) {
    val bounds: List<SpaceCompassSunSceneFrame> get()=labels.map { it.bounds }
}

/** Draw quiet grid lines and reserve perimeter/equator labels; paint their text after all trajectories. */
internal fun drawSpaceCompassGlobeCoordinateGrid(canvas: Canvas,width: Int,height: Int,
    radius: Double,centerY: Double,pose: SpaceCompassGlobePose,snapshot: SpaceCompassPanoramaSnapshot,
    reserved: List<SpaceCompassSunSceneFrame> = emptyList()): SpaceCompassGlobeCoordinateDrawing {
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    val edge=SpaceCompassAngularGridAppearance.lineWidth(width,height)
    val textSize=spaceCompassGlobeGuideTextSize(width,height,snapshot.globe?.framing)*.75f
    val text=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.textSize=textSize }
    val occupied=reserved.toMutableList()
    val labels=mutableListOf<SpaceCompassGlobeCoordinateLabel>()
    fun point(v: SpaceCompassGlobeVector)=SpaceCompassSunScenePoint(width/2+v.x*radius,centerY-v.y*radius)
    spaceCompassGeographicGrid.forEach { curve ->
        curve.vertices.map { spaceCompassGlobeView(it,pose) }.zipWithNext().forEach { (a,b) ->
            if(a.z>=-1e-9 && b.z>=-1e-9) {
                val p=point(a);val q=point(b)
                paint.color=SpaceCompassAngularGridAppearance.shadowArgb;paint.strokeWidth=edge*2
                canvas.drawLine(p.x.toFloat(),p.y.toFloat(),q.x.toFloat(),q.y.toFloat(),paint)
                paint.color=SpaceCompassAngularGridAppearance.lineArgb;paint.strokeWidth=edge
                canvas.drawLine(p.x.toFloat(),p.y.toFloat(),q.x.toFloat(),q.y.toFloat(),paint)
            }
        }
    }
    if (!snapshot.showGridNumbers) return SpaceCompassGlobeCoordinateDrawing()
    val padding=textSize*.45;val gap=textSize*.55
    val lineHeight=(text.fontMetrics.descent-text.fontMetrics.ascent).toDouble()
    val anchors=spaceCompassGlobeCoordinateAnchors(pose)
    val ordered=anchors.filter { it.latitude&&abs(it.degrees)==90 }+
        anchors.filter { it.latitude&&abs(it.degrees)!=90 }.sortedByDescending { it.rim.y }+
        anchors.filterNot { it.latitude }.sortedByDescending { it.point.z }
    for(anchor in ordered) {
        val value=spaceCompassGeographicGridLabel(anchor.degrees,anchor.latitude,snapshot.formatting,snapshot.geographicDirections)
        val textWidth=text.measureText(value).toDouble();val p=point(anchor.point);val rim=point(anchor.rim)
        if(textWidth+padding*2>=width || lineHeight+padding*2>=height) continue
        if(rim.y< -lineHeight || rim.y>height+lineHeight) continue
        if(!anchor.latitude && (p.x<padding || p.x>width-padding || p.y<padding || p.y>height-padding)) continue
        val pole=anchor.latitude&&abs(anchor.degrees)==90
        val options=if(pole) {
            if(p.x<padding||p.x>width-padding||p.y<padding||p.y>height-padding) continue
            // Keep 90 degrees beside its actual projected pole, including pole-on views.
            val sides=if(p.y<centerY) listOf(1,-1) else listOf(-1,1)
            sides.map { side -> SpaceCompassSunSceneFrame(
                (p.x-textWidth/2).coerceIn(padding,width-padding-textWidth),
                (if(side<0) p.y-gap-lineHeight else p.y+gap).coerceIn(padding,height-padding-lineHeight),textWidth,lineHeight) }+
                listOf(-1,1).map { side -> SpaceCompassSunSceneFrame(
                    (if(side<0) p.x-gap-textWidth else p.x+gap).coerceIn(padding,width-padding-textWidth),
                    (p.y-lineHeight/2).coerceIn(padding,height-padding-lineHeight),textWidth,lineHeight) }
        } else if(anchor.latitude) {
            // Do not slide latitude numbers up/down onto a neighbouring parallel.
            // Extra horizontal inset is allowed; every number keeps its exact curve anchor.
            val cy=rim.y.coerceIn(padding+lineHeight/2,height-padding-lineHeight/2)
            listOf(1.0,2.5,4.0).map { inset ->
                val left=if(anchor.side<0) rim.x+gap*inset else rim.x-gap*inset-textWidth
                SpaceCompassSunSceneFrame(left.coerceIn(padding,width-padding-textWidth),cy-lineHeight/2,textWidth,lineHeight)
            }
        } else listOf(-1,1).map { side ->
            // Longitudes stay immediately above/below their equatorial crossing, never near a pole.
            SpaceCompassSunSceneFrame((p.x-textWidth/2).coerceIn(padding,width-padding-textWidth),
                (if(side<0) p.y-gap-lineHeight else p.y+gap).coerceIn(padding,height-padding-lineHeight),textWidth,lineHeight)
        }
        val bounds=options.firstOrNull { candidate -> occupied.none { o ->
            candidate.left<o.left+o.width+padding && candidate.left+candidate.width>o.left-padding &&
                candidate.top<o.top+o.height+padding && candidate.top+candidate.height>o.top-padding
        } } ?: continue
        labels+=SpaceCompassGlobeCoordinateLabel(value,p,bounds,pole);occupied+=bounds
    }
    return SpaceCompassGlobeCoordinateDrawing(labels,textSize)
}

/** Coordinate text stays readable over the paths; subtle leaders preserve its exact geographic anchor. */
internal fun drawSpaceCompassGlobeCoordinateLabels(canvas: Canvas,drawing: SpaceCompassGlobeCoordinateDrawing) {
    val text=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=drawing.textSize;textAlign=Paint.Align.CENTER }
    val line=Paint(Paint.ANTI_ALIAS_FLAG)
    // Paint all leaders before text so a later connector cannot overwrite another number.
    drawing.labels.forEach { label ->
        val b=label.bounds;val p=label.anchor
        val x=p.x.coerceIn(b.left,b.left+b.width);val y=p.y.coerceIn(b.top,b.top+b.height)
        if(hypot(x-p.x,y-p.y)>drawing.textSize*.1) {
            line.color=0xaa000000.toInt();line.strokeWidth=drawing.textSize*.14f
            canvas.drawLine(p.x.toFloat(),p.y.toFloat(),x.toFloat(),y.toFloat(),line)
            line.color=0xbbd8e7f2.toInt();line.strokeWidth=drawing.textSize*.06f
            canvas.drawLine(p.x.toFloat(),p.y.toFloat(),x.toFloat(),y.toFloat(),line)
        }
        val radius=drawing.textSize*if(label.pole) .14f else .09f
        line.color=Color.BLACK;canvas.drawCircle(p.x.toFloat(),p.y.toFloat(),radius+drawing.textSize*.05f,line)
        line.color=0xffd8e7f2.toInt();canvas.drawCircle(p.x.toFloat(),p.y.toFloat(),radius,line)
    }
    drawing.labels.forEach { label ->
        val b=label.bounds
        val baseline=(b.top+b.height/2-(text.fontMetrics.ascent+text.fontMetrics.descent)/2).toFloat()
        text.style=Paint.Style.STROKE;text.strokeWidth=drawing.textSize*.16f;text.color=Color.BLACK
        canvas.drawText(label.value,(b.left+b.width/2).toFloat(),baseline,text)
        text.style=Paint.Style.FILL;text.color=0xcce2edf5.toInt()
        canvas.drawText(label.value,(b.left+b.width/2).toFloat(),baseline,text)
    }
}
