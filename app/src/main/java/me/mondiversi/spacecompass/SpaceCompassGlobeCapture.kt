package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.*
import kotlin.math.*
import androidx.compose.ui.graphics.toArgb

internal data class SpaceCompassGlobeCapture(val pose: SpaceCompassGlobePose,
    val directionNote: String, val dark: Boolean = true, val labelMode: SpaceCompassPanoramaLabelMode = SpaceCompassPanoramaLabelMode.KEY_SELECTED)

/** Frozen geometry, independent of the WebView and later gestures. Reuses the standard caption/legend/export policy. */
internal fun renderSpaceCompassGlobe(snapshot: SpaceCompassPanoramaSnapshot,width: Int,context: Context): Bitmap {
    val capture=requireNotNull(snapshot.globe)
    val model=buildSpaceCompassGlobeModel(snapshot)
    val h=(width*.8).toInt();val style=spaceCompassCaptureBandStyle(width,h)
    val caption=prepareSpaceCompassCaptureCaption(snapshot.caption,width,style,context)
    val heading=spaceCompassPanoramaSceneTop(caption,width).toInt()
    val legend=prepareSpaceCompassPanoramaLegend(snapshot.objects,width,style)
    val bitmap=Bitmap.createBitmap(width,h+heading+legend.height,Bitmap.Config.ARGB_8888)
    try {
        val canvas=Canvas(bitmap);canvas.drawColor(Color.BLACK);caption.draw(canvas)
        canvas.save();canvas.translate(0f,heading.toFloat());canvas.clipRect(0,0,width,h)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        drawSpaceCompassGlobeUniverse(canvas,width,h,capture.dark)
        val r=spaceCompassGlobeFitRadius(SpaceCompassSunSceneFrame(0.0,0.0,width.toDouble(),h.toDouble()),model.extent,capture.pose.zoom,width/80.0)
        val earth=renderSpaceCompassGlobeEarth(context,(r*2).roundToInt().coerceIn(64,2048),capture.pose,snapshot.timeMs)
        try { canvas.drawBitmap(earth,null,RectF((width/2-r).toFloat(),(h/2-r).toFloat(),(width/2+r).toFloat(),(h/2+r).toFloat()),paint) }
        finally { earth.recycle() }
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize=width/100f;typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD);textAlign=Paint.Align.CENTER }
        fun projected(v: SpaceCompassGlobeVector)=spaceCompassGlobeView(v,capture.pose)
        fun hidden(v: SpaceCompassGlobeVector)=v.z<0&&v.x*v.x+v.y*v.y<1
        fun xy(v: SpaceCompassGlobeVector)=PointF((width/2+v.x*r).toFloat(),(h/2-v.y*r).toFloat())
        fun segment(a: SpaceCompassGlobeVector,b: SpaceCompassGlobeVector,tint: Int,dashed: Boolean) {
            val av=projected(a);val bv=projected(b);val p=xy(av);val q=xy(bv)
            paint.color=tint;paint.alpha=if(hidden(av)&&hidden(bv)) 42 else 255;paint.strokeWidth=width/650f
            paint.pathEffect=if(dashed||hidden(av)&&hidden(bv)) DashPathEffect(floatArrayOf(width/180f,width/200f),0f) else null
            canvas.drawLine(p.x,p.y,q.x,q.y,paint);paint.alpha=255;paint.pathEffect=null
        }
        if(snapshot.showSkyReferences) for(latitude in listOf(-66.56,-23.44,0.0,23.44,66.56)) {
            val phi=Math.toRadians(latitude)
            fun v(i: Int)=Math.toRadians(i*3.0).let { SpaceCompassGlobeVector(cos(phi)*sin(it),sin(phi),cos(phi)*cos(it)) }
            for(i in 1..120) segment(v(i-1),v(i),0xff869bab.toInt(),false)
        }
        model.tracks.forEach { track -> track.curve.zipWithNext().forEach { (a,b) ->
            segment(a.first,b.first,track.item.tint.toArgb(),b.second||track.item.comparison!=null)
        } }
        val unit=width/1920f
        val obstacles=mutableListOf<SpaceCompassSunSceneFrame>()
        // Reuse panorama placement, reserving the projected curves and every visible marker first.
        fun reserve(x: Float,y: Float,w: Float,h: Float,padding: Float) {
            obstacles+=SpaceCompassSunSceneFrame((x-padding).toDouble(),(y-padding).toDouble(),
                (w+padding*2).toDouble(),(h+padding*2).toDouble())
        }
        model.tracks.forEach { track ->
            track.curve.zipWithNext().forEach { (a,b) ->
                val av=projected(a.first);val bv=projected(b.first)
                if(!hidden(av)||!hidden(bv)) {
                    val p=xy(av);val q=xy(bv)
                    reserve(min(p.x,q.x),min(p.y,q.y),abs(p.x-q.x),abs(p.y-q.y),6*unit)
                }
            }
            track.dots.forEach { dot -> val v=projected(dot.vector)
                if(!hidden(v)) { val p=xy(v);val radius=if(dot.current) width/120f else 7*unit
                    reserve(p.x-radius,p.y-radius,radius*2,radius*2,6*unit) }
            }
        }
        data class PendingLabel(val value: String,val point: PointF,val tint: Int,val radius: Float,
            val selected: Boolean,val current: Boolean,val behind: Boolean)
        val pending=mutableListOf<PendingLabel>()
        fun label(value: String,p: PointF,tint: Int,radius: Float,selected: Boolean,current: Boolean,behind: Boolean) {
            pending+=PendingLabel(value,p,tint,radius,selected,current,behind)
        }
        model.tracks.forEach { track ->
            val active=if(snapshot.activeComparisonId!=null) snapshot.activeComparisonId==track.item.comparison?.id
                    else snapshot.activeBody==track.item.body&&track.item.comparison==null
            track.dots.sortedWith(compareBy<SpaceCompassGlobeDot> { it.current }.thenBy { it.selected }).forEach { dot ->
                val v=projected(dot.vector);val behind=hidden(v)
                val p=xy(v);val tint=if(dot.selected) snapshot.selectionTint else track.item.tint.toArgb()
                val radius=if(dot.current) width/120f else width/260f
                if(!behind) {
                paint.color=tint;paint.style=Paint.Style.FILL;paint.strokeWidth=width/850f
                if(dot.current&&!dot.antipode) {
                    val marker=renderSpaceCompassPanoramaMarker(context,track.item.body,track.item.referenceTime(snapshot.timeMs))
                    try {
                        canvas.save();canvas.clipPath(Path().apply { addCircle(p.x,p.y,radius,Path.Direction.CW) })
                        canvas.drawBitmap(marker,null,RectF(p.x-radius,p.y-radius,p.x+radius,p.y+radius),paint);canvas.restore()
                    } finally { marker.recycle() }
                    paint.style=Paint.Style.STROKE;paint.strokeWidth=if(dot.selected) width/600f else width/1500f
                    canvas.drawCircle(p.x,p.y,radius,paint)
                } else if(!dot.antipode&&dot.point.event!=SpaceCompassSunPathEvent.HOUR) {
                    drawSpaceCompassPanoramaEventMarker(canvas,dot.point.event,
                        SpaceCompassSunScenePoint(p.x.toDouble(),p.y.toDouble()),width/4096f,tint)
                } else {
                    paint.style=if(dot.antipode) Paint.Style.STROKE else Paint.Style.FILL
                    canvas.drawCircle(p.x,p.y,radius,paint)
                }
                if(dot.selected&&!dot.current){paint.style=Paint.Style.STROKE;paint.strokeWidth=width/600f;canvas.drawCircle(p.x,p.y,width/110f,paint)}
                paint.style=Paint.Style.FILL
                }
                if(spaceCompassGlobeCaptureShowsLabel(snapshot.labelMode,active,dot.current,dot.antipode,dot.point.events,dot.selected)) {
                    val namedEvents=dot.point.events.filter { it!=SpaceCompassSunPathEvent.HOUR }.toSet()
                    val point=if(!dot.current&&namedEvents.isNotEmpty()) dot.point.copy(event=namedEvents.first(),coincidentEvents=namedEvents) else dot.point
                    val selection=SpaceCompassPanoramaSelectedPoint(track.item.body,point,track.item.path,dot.current,dot.antipode,track.item.comparison)
                    val borderTint=if(dot.selected&&!behind) snapshot.selectionTint else
                        spaceCompassCelestialPathVisibilityTint(track.item.tint,behind||dot.point.position.elevationDegrees<0).toArgb()
                    label(formatSpaceCompassPanoramaSelection(snapshot,selection),p,borderTint,radius,dot.selected,dot.current,behind)
                }
            }
        }
        data class PlacedLabel(val label: PendingLabel,val lines: List<String>,val bounds: RectF)
        val placed=pending.sortedWith(compareByDescending<PendingLabel> { it.selected }.thenByDescending { it.current }).mapNotNull { label ->
            val lines=label.value.lines();val w=minOf(width*.44f,lines.maxOf { text.measureText(it) }+width/80f)
            val height=lines.size*text.textSize*1.35f+width/100f
            val bounds=spaceCompassPanoramaLabelBounds(SpaceCompassSunScenePoint(label.point.x.toDouble(),label.point.y.toDouble()),
                w.toDouble(),height.toDouble(),width.toDouble(),h.toDouble(),unit.toDouble(),obstacles,
                label.radius/unit+12.0) ?: return@mapNotNull null
            obstacles+=bounds
            PlacedLabel(label,lines,RectF(bounds.left.toFloat(),bounds.top.toFloat(),
                (bounds.left+bounds.width).toFloat(),(bounds.top+bounds.height).toFloat()))
        }
        // Paint connectors first so no later leader can cross another label's text.
        placed.forEach { (label,_,bounds) ->
            val x=label.point.x.coerceIn(bounds.left,bounds.right);val y=label.point.y.coerceIn(bounds.top,bounds.bottom)
            val dx=x-label.point.x;val dy=y-label.point.y;val length=hypot(dx,dy).coerceAtLeast(1f)
            paint.color=label.tint;paint.style=Paint.Style.STROKE;paint.strokeWidth=.8f*unit;paint.pathEffect=if(label.behind) DashPathEffect(floatArrayOf(3*unit,3*unit),0f) else null
            canvas.drawLine(label.point.x+dx/length*(label.radius+3*unit),label.point.y+dy/length*(label.radius+3*unit),x,y,paint)
        }
        placed.forEach { (label,lines,bounds) ->
            paint.pathEffect=null
            paint.color=0xd9101418.toInt();paint.style=Paint.Style.FILL;canvas.drawRoundRect(bounds,4*unit,4*unit,paint)
            paint.color=label.tint;paint.style=Paint.Style.STROKE;paint.strokeWidth=width/1100f;canvas.drawRoundRect(bounds,4*unit,4*unit,paint)
            text.color=Color.WHITE
            lines.forEachIndexed { i,line -> canvas.drawText(line,bounds.centerX(),bounds.top+width/160f+text.textSize*(1+i*1.35f),text) }
        }
        paint.style=Paint.Style.FILL
        // Location marker is itself precise position metadata: omit it in Area and Hidden exports.
        if(!snapshot.positionHidden&&snapshot.globeLocationVisible&&snapshot.observerPositionKnown) {
            val lat=Math.toRadians(snapshot.latitude);val lon=Math.toRadians(snapshot.longitude)
            val p=spaceCompassGlobeView(SpaceCompassGlobeVector(cos(lat)*sin(lon),sin(lat),cos(lat)*cos(lon)),capture.pose)
            val anchor=xy(p)
            val pinWidth=(r*2*.06).toFloat().coerceIn(16*unit,22*unit)
            drawSpaceCompassGlobeLocationMarker(canvas,anchor.x,anchor.y,pinWidth,p.z)
        }
        text.color=0xffb5c3d0.toInt();text.typeface=Typeface.DEFAULT;text.textSize=width/120f
        val noteWidth=text.measureText(capture.directionNote)
        if(noteWidth>width*.95f)text.textSize*=width*.95f/noteWidth
        canvas.drawText(capture.directionNote,width/2f,h-width/75f,text)
        canvas.restore();legend.draw(canvas,(heading+h).toFloat(),Color.BLACK)
        return bitmap
    } catch(error: Throwable) { bitmap.recycle();throw error }
}

/** Offline software capture of the shared Blue/Black Marble maps. No new texture is bundled. */
internal fun renderSpaceCompassGlobeEarth(context: Context,size: Int,pose: SpaceCompassGlobePose,timeMs: Long): Bitmap {
    fun map(name: String): Pair<IntArray,Pair<Int,Int>> {
        val bitmap=context.assets.open("screensaver/$name.webp").use { BitmapFactory.decodeStream(it) }
        try { val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
            return pixels to (bitmap.width to bitmap.height)
        } finally { bitmap.recycle() }
    }
    val (day,dim)=map("earth-day");val (night,_)=map("earth-night");val (mw,mh)=dim
    val sunlight=spaceCompassScreensaverSunlight(timeMs)
    val sun=spaceCompassGlobeView(SpaceCompassGlobeVector(sunlight.x,sunlight.y,sunlight.z),pose)
    val out=IntArray(size*size);val cp=cos(pose.latitude);val sp=sin(pose.latitude);val c=cos(pose.longitude);val s=sin(pose.longitude);val cr=cos(pose.roll);val sr=sin(pose.roll)
    for(y in 0 until size)for(x in 0 until size) {
        val nx=(x+.5)*2/size-1;val ny=1-(y+.5)*2/size;val d=nx*nx+ny*ny
        if(d>1)continue
        val nz=sqrt(1-d);val ax=nx*cr-ny*sr;val by=nx*sr+ny*cr
        val ay=by*cp+nz*sp;val az=-by*sp+nz*cp
        val vx=ax*c+az*s;val vz=-ax*s+az*c
        val u=((atan2(vx,vz)/(2*PI)+.5)%1+1)%1;val v=.5-asin(ay.coerceIn(-1.0,1.0))/PI
        val idx=(v*mh).toInt().coerceIn(0,mh-1)*mw+(u*mw).toInt().coerceIn(0,mw-1)
        val color=day[idx];val city=night[idx]
        val lit=nx*sun.x+ny*sun.y+nz*sun.z;val diffuse=max(0.0,lit)
        val t=((lit+.13)/.26).coerceIn(0.0,1.0);val dark=1-t*t*(3-2*t)
        val red=Color.red(color);val green=Color.green(color);val blue=Color.blue(color)
        val mask=(((blue-max(red,green)).toDouble()/max(blue,1)-.12)/.33).coerceIn(0.0,1.0)
        val gain=1+.6*mask*mask*(3-2*mask)
        fun channel(value: Int,lights: Int)=(min(255.0,value*gain)*(.075+diffuse*.98)+(lights/255.0).pow(1.5)*dark*1.7*255).toInt().coerceIn(0,255)
        out[y*size+x]=Color.rgb(channel(red,Color.red(city)),channel(green,Color.green(city)),channel(blue,Color.blue(city)))
    }
    return Bitmap.createBitmap(out,size,size,Bitmap.Config.ARGB_8888)
}

/** Full image backdrop is drawn before Earth, paths and labels; caption and legend remain black. */
internal fun drawSpaceCompassGlobeUniverse(canvas: Canvas,width: Int,height: Int,dark: Boolean) {
    val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader=RadialGradient(width*.7f,height*.7f,width.toFloat(),
        SpaceCompassGlobeUniverse.palette(dark),SpaceCompassGlobeUniverse.stops,Shader.TileMode.CLAMP)
    canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
    paint.shader=null
    paint.color=SpaceCompassGlobeUniverse.starColor
    // Export-resolution independent star sizes, equivalent to a 400 dp wide main view.
    val scale=width/400f
    SpaceCompassGlobeUniverse.stars.forEach { star ->
        canvas.drawCircle(width*star.x,height*star.y,star.radius*scale,paint)
    }
}
