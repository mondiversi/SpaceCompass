package me.mondiversi.spacecompass

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Base64
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.webkit.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import kotlin.math.*

internal const val SPACE_COMPASS_GLOBE_MODE_KEY = "sky_globe_mode"
internal const val SPACE_COMPASS_GLOBE_ZOOM_KEY = "globe_zoom"

internal fun SpaceCompassGlobeVector.json() = JSONArray(listOf(x,y,z))
internal fun SpaceCompassGlobePose.json() = JSONArray(listOf(longitude,latitude,zoom,roll))
internal data class SpaceCompassGlobeDocument(val json: String, val targets: List<Pair<SpaceCompassGlobeTrack,SpaceCompassGlobeDot>>)

internal fun globeColor(value: Int) = "#%06x".format(value and 0xffffff)

@Composable
internal fun SpaceCompassGlobeView(snapshot: SpaceCompassPanoramaSnapshot, pose: SpaceCompassGlobePose,
    onPose: (SpaceCompassGlobePose) -> Unit, state: SpaceCompassSunDailyPathUiState,
    exclusions: List<SpaceCompassSunSceneFrame>, viewport: SpaceCompassSunSceneFrame, noteBottomInset: androidx.compose.ui.unit.Dp, labelMode: SpaceCompassPanoramaLabelMode, axisLocked: Boolean,
    resumed: Boolean,
    onTap: () -> Unit, onDoubleTap: () -> Unit, modifier: Modifier = Modifier, onCaptureFrame: (SpaceCompassGlobeFraming)->Unit = {}, showDirectionNote: Boolean = true, pointingFrame: SpaceCompassSunSceneFrame? = null,
    flat: Boolean = false, sky: Boolean = false, reticle: SpaceCompassGlobeVector? = null, capturePose: SpaceCompassGlobePose = pose, captureViewport: SpaceCompassSunSceneFrame = viewport) {
    val context=LocalContext.current;val resources=LocalResources.current;val dark=isSystemInDarkTheme()
    val currentPose by rememberUpdatedState(pose)
    val changePose by rememberUpdatedState(onPose)
    val tap by rememberUpdatedState(onTap);val doubleTap by rememberUpdatedState(onDoubleTap)
    val currentZone by rememberUpdatedState(snapshot.zoneId)
    val paths=snapshot.objects.map { it.path }
    val bodyKeys=snapshot.objects.map { it.body to it.comparison?.id }
    val curveCache=remember(sky,snapshot.latitude,snapshot.longitude,snapshot.altitude,snapshot.remote) {
        java.util.concurrent.ConcurrentHashMap<SpaceCompassSunDailyPath,List<Pair<SpaceCompassGlobeVector,Boolean>>>()
    }
    val model by produceState<SpaceCompassGlobeModel?>(null,paths,bodyKeys,snapshot.timeMs/1000,
        snapshot.latitude,snapshot.longitude,snapshot.altitude,snapshot.selectedPoints,snapshot.showAntipodes,snapshot.remote,sky) {
        val job=kotlin.coroutines.coroutineContext
        value=withContext(Dispatchers.Default) { buildSpaceCompassGlobeModel(snapshot,curveCache,sky=sky) { job.ensureActive() } }
    }
    val fitBounds=remember(model,pose.latitude,pose.roll) { spaceCompassGlobeFitBounds(model,pose) }
    val captureBounds=remember(model,capturePose.latitude,capturePose.roll) { spaceCompassGlobeFitBounds(model,capturePose) }
    val iconKeys=snapshot.objects.map { spaceCompassGlobeSpriteKey(it,snapshot.timeMs) }
    val icons by produceState<Map<String,String>>(emptyMap(),iconKeys) {
        value=withContext(Dispatchers.Default) { spaceCompassGlobeSprites(context.applicationContext,snapshot.objects,snapshot.timeMs) }
        withContext(Dispatchers.IO) { ensureSpaceCompassCelestialThumbnails(context.applicationContext,snapshot.objects.map { it.body }.toSet()) }
        value=withContext(Dispatchers.Default) { spaceCompassGlobeSprites(context.applicationContext,snapshot.objects,snapshot.timeMs) }
    }
    val fluidity=SpaceCompassSkyFluidity.fromStored(LocalSpaceCompassPresentationSettings.current[SPACE_COMPASS_SKY_FLUIDITY_KEY])
    val starMap by produceState<String?>(null,sky,snapshot.timeMs/60_000,snapshot.latitude,snapshot.longitude,
        snapshot.altitude,snapshot.weather,snapshot.center) {
        value=if(!sky) null else withContext(Dispatchers.Default) {
            val bitmap=android.graphics.Bitmap.createBitmap(1024,512,android.graphics.Bitmap.Config.ARGB_8888)
            try {
                drawSpaceCompassPanoramaStars(android.graphics.Canvas(bitmap),snapshot,1024,512,context.applicationContext)
                java.io.ByteArrayOutputStream().use { output -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output)
                    "data:image/png;base64,"+Base64.encodeToString(output.toByteArray(),Base64.NO_WRAP) }
            } finally { bitmap.recycle() }
        }
    }
    val selection=androidx.compose.material3.MaterialTheme.colorScheme.primary.toArgb()
    val sun by produceState<SpaceCompassScreensaverSunlight?>(null,snapshot.timeMs/60_000) {
        value=withContext(Dispatchers.Default) { spaceCompassScreensaverSunlight(snapshot.timeMs) }
    }
    var native by remember { mutableStateOf<SpaceCompassGlobeWebView?>(null) }
    var noteFrame by remember { mutableStateOf<SpaceCompassSunSceneFrame?>(null) }
    val localDensity=androidx.compose.ui.platform.LocalDensity.current
    val composeDensity=localDensity.density
    // CSS pixels use the native WebView density, not the app's adaptive Compose scaling.
    val density=context.resources.displayMetrics.density
    val windowHeight=androidx.compose.ui.platform.LocalView.current.height.takeIf { it>0 }?.toFloat()
        ?: (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*composeDensity)
    val previewHeight=(windowHeight-WindowInsets.safeDrawing.getTop(localDensity)-
        WindowInsets.safeDrawing.getBottom(localDensity)-48*composeDensity).coerceAtLeast(density)
    val geographicGuideNames=mapOf(
        SpaceCompassSkyReference.EQUATOR to R.string.globe_reference_equator,
        SpaceCompassSkyReference.CANCER to R.string.globe_reference_cancer,
        SpaceCompassSkyReference.CAPRICORN to R.string.globe_reference_capricorn,
        SpaceCompassSkyReference.ARCTIC to R.string.globe_reference_arctic,
        SpaceCompassSkyReference.ANTARCTIC to R.string.globe_reference_antarctic
    ).mapValues { resources.getString(it.value) }
    val geographicPoleNames=listOf(resources.getString(R.string.globe_reference_south_pole),
        resources.getString(R.string.globe_reference_north_pole))
    // Geometry and label formatting never depend on layout or interaction pose.
    // Keep this work off the presentation thread, including the once-per-second clock update.
    val document by produceState(SpaceCompassGlobeDocument("{\"tracks\":[],\"background\":false}",emptyList()),
        model,sun,dark,labelMode,icons,starMap,snapshot.timeMs/1000,sky,snapshot.phase,snapshot.weather,snapshot.solarLighting,
        snapshot.latitude,snapshot.longitude,snapshot.observerPositionKnown,
        snapshot.objects.map { it.name },geographicGuideNames,geographicPoleNames,snapshot.activeBody,
        snapshot.activeComparisonId,snapshot.showSkyReferences,snapshot.showGrid,snapshot.showGridNumbers,selection,snapshot.formatting) {
        value=withContext(Dispatchers.Default) {
            val targets=mutableListOf<Pair<SpaceCompassGlobeTrack,SpaceCompassGlobeDot>>()
            val scene=JSONObject().apply {
                put("extent",model?.extent ?: 1.55);put("dark",dark)
                put("sky",sky);put("skyCenter",snapshot.center.azimuth)
                put("starMap",starMap ?: "")
                put("skyGuides",JSONArray().apply {
                    if(sky && snapshot.showSkyReferences && snapshot.observerPositionKnown)
                        spaceCompassSkyReferenceCircles(snapshot.latitude,snapshot.timeMs,180).forEachIndexed { index,circle ->
                            put(JSONObject().apply { put("latitude",index);put("name",snapshot.referenceNames[circle.reference].orEmpty());put("value","")
                                put("vertices",JSONArray().apply { circle.directions.forEach { v -> put(JSONArray(listOf(v.east,v.up,v.north))) } }) })
                        }
                })
                put("skyPoles",JSONArray().apply {
                    if(sky && snapshot.showSkyReferences && snapshot.observerPositionKnown) {
                        val north=spaceCompassNorthCelestialPole(snapshot.latitude)
                        val vectors=listOf(SpaceCompassGlobeVector(north.east,north.up,north.north),SpaceCompassGlobeVector(-north.east,-north.up,-north.north),
                            SpaceCompassGlobeVector(0.0,1.0,0.0),SpaceCompassGlobeVector(0.0,-1.0,0.0))
                        val names=listOf(snapshot.poleNames.first,snapshot.poleNames.second,snapshot.observerPointNames.second,snapshot.observerPointNames.first)
                        vectors.forEachIndexed { index,v -> put(JSONObject().apply { put("v",v.json());put("latitude",index+100);put("name",names[index]);put("value","") }) }
                    }
                })
                val skyColors=spaceCompassSunSkyColors(snapshot.phase,snapshot.weather,snapshot.solarLighting)
                val ground=spaceCompassSunGroundPalette(snapshot.phase,snapshot.solarLighting)
                put("skyColors",JSONArray(listOf(globeColor(skyColors.first.toArgb()),globeColor(skyColors.second.toArgb()),
                    globeColor(ground.farArgb),globeColor(ground.nearArgb))))
                put("grid",snapshot.showGrid);put("gridNumbers",snapshot.showGridNumbers);put("guides",snapshot.showSkyReferences && !sky);put("selection",globeColor(selection))
                put("coordinateLabels",JSONArray().apply {
                    for(latitude in listOf(true,false)) for(degrees in if(latitude) -90..90 step 15 else -180 until 180 step 15) {
                        val text=if(sky) formatSpaceCompassNumber((if(latitude) degrees else (degrees+360)%360).toDouble(),0,
                            snapshot.formatting.numeric,grouping=false,systemLocale=snapshot.formatting.deviceLocale)+"°"
                            else spaceCompassGeographicGridLabel(degrees,latitude,snapshot.formatting,snapshot.geographicDirections)
                        put(JSONObject().put("latitude",latitude).put("degrees",degrees).put("text",text))
                    }
                })
                put("guideLabels",JSONArray().apply {
                    listOf(-66.56 to SpaceCompassSkyReference.ANTARCTIC,-23.44 to SpaceCompassSkyReference.CAPRICORN,
                        0.0 to SpaceCompassSkyReference.EQUATOR,23.44 to SpaceCompassSkyReference.CANCER,66.56 to SpaceCompassSkyReference.ARCTIC)
                        .forEach { (degrees,reference) -> put(JSONObject().apply {
                            put("latitude",degrees);put("name",geographicGuideNames[reference].orEmpty())
                            put("value",(if(degrees>0) "+" else "")+formatSpaceCompassNumber(degrees,1,
                                snapshot.formatting.numeric,grouping=false,systemLocale=snapshot.formatting.deviceLocale)+"°")
                        }) }
                })
                put("poleLabels",JSONArray().apply {
                    listOf(-90,90).forEachIndexed { index,degrees -> put(JSONObject().apply {
                        put("latitude",degrees);put("name",geographicPoleNames[index])
                        put("value",(if(degrees>0) "+" else "")+formatSpaceCompassNumber(degrees.toDouble(),0,
                            snapshot.formatting.numeric,grouping=false,systemLocale=snapshot.formatting.deviceLocale)+"°")
                    }) }
                })
                put("labels",labelMode!=SpaceCompassPanoramaLabelMode.NONE);put("background",false)
                put("sun",sun?.let { JSONArray(listOf(it.x,it.y,it.z)) } ?: JSONArray(listOf(0,0,1)))
                // Coarse/hidden captures suppress this marker; the live view uses the effective Location.
                put("location",if(!sky && snapshot.observerPositionKnown) JSONArray(listOf(snapshot.latitude,snapshot.longitude)) else JSONObject.NULL)
                put("tracks",JSONArray().apply { model?.tracks?.forEach { track ->
                    ensureActive()
                    put(JSONObject().apply {
                        put("key",track.item.body.name+":"+track.item.comparison?.id)
                        put("color",globeColor(track.item.tint.toArgb()));put("comparison",track.item.comparison!=null)
                        put("belowColor",globeColor(spaceCompassCelestialPathLineTint(track.item.tint,true).toArgb()))
                        put("belowLabelColor",globeColor(spaceCompassCelestialPathVisibilityTint(track.item.tint,true).toArgb()))
                        put("icon",icons[spaceCompassGlobeSpriteKey(track.item,snapshot.timeMs)] ?: "")
                        put("curve",JSONArray().apply { track.curve.forEach { (v,below) -> put(JSONArray().put(v.json()).put(below)) } })
                        val active=if(snapshot.activeComparisonId!=null) snapshot.activeComparisonId==track.item.comparison?.id
                            else snapshot.activeBody==track.item.body&&track.item.comparison==null
                        put("dots",JSONArray().apply { track.dots.forEach { dot ->
                            val id=targets.size;targets+=track to dot
                            val event=dot.point.events.filter { it!=SpaceCompassSunPathEvent.HOUR }.mapNotNull { snapshot.pointEventNames[it] }.joinToString(" · ")
                            val time=formatSpaceCompassGlobeMainPointTime(dot.point.timeMs,track.item.referenceTime(snapshot.timeMs),snapshot.displayTimeZone,snapshot.formatting)
                            put(JSONObject().apply {
                                put("v",dot.vector.json());put("current",dot.current);put("selected",dot.selected)
                                put("antipode",dot.antipode);put("id",id);put("below",dot.point.position.elevationDegrees<0)
                                put("event",dot.point.event.name)
                                if(dot.point.event==SpaceCompassSunPathEvent.SUNRISE||dot.point.event==SpaceCompassSunPathEvent.SUNSET)
                                    put("glyph",JSONArray().apply { spaceCompassHorizonEventGlyph(dot.point.event,0f,0f,1f).forEach { (x,y) -> put(JSONArray(listOf(x,y))) } })
                                if(!spaceCompassGlobeMainShowsLabel(labelMode,active,dot.current,dot.antipode,dot.point.events,dot.selected)) put("label","")
                                else {
                                    val pointName=if(dot.antipode) formatSpaceCompassPanoramaSelection(snapshot,SpaceCompassPanoramaSelectedPoint(
                                        track.item.body,dot.point,track.item.path,dot.current,true,track.item.comparison)).lineSequence().first()
                                        else if(dot.current) track.item.comparison?.let(snapshot::comparisonPointName) ?: snapshot.currentPointName
                                        else event
                                    val detail=formatSpaceCompassGlobeMainLabelDetails(track.item.name,pointName,time,dot.point.position,snapshot.formatting)
                                    put("label",detail.minimal);put("labelCompact",detail.compact);put("labelFull",detail.full)
                                }
                            })
                        } })
                    })
                } })
            }
            ensureActive()
            SpaceCompassGlobeDocument(scene.toString(),targets.toList())
        }
    }
    val latestTargets by rememberUpdatedState(document.targets)
    val globeLabel=stringResource(if(sky) R.string.view_mode_sky_panorama else if(flat) R.string.view_mode_globe_panorama else R.string.view_mode_globe_frame)
    BoxWithConstraints(modifier.testTag(if(sky) "sky-panorama-view" else "earth-orbit-view").semantics {
        contentDescription=globeLabel
        customActions=latestTargets.filter { (_,dot) -> dot.current || dot.selected }.filter { !it.second.antipode }
            .map { (track,dot) -> CustomAccessibilityAction(track.item.name+" · "+
                formatSpaceCompassGlobeMainPointTime(dot.point.timeMs,track.item.referenceTime(snapshot.timeMs),snapshot.displayTimeZone,snapshot.formatting)) {
                if(dot.current) state.toggleCurrent(track.item.body,dot.point,track.item.path)
                else track.item.path?.let { state.togglePoint(it,dot.point) };true
            } }
    }) {
        val frameExclusions=remember(exclusions,noteFrame) { exclusions+listOfNotNull(noteFrame) }
        val frameJson=remember(constraints.maxWidth,constraints.maxHeight,density,viewport,frameExclusions,fitBounds,fluidity,flat,reticle) {
            JSONObject().apply {
                put("flat",flat);put("reticle",reticle?.json() ?: JSONObject.NULL)
                put("canvasSize",JSONArray(listOf(constraints.maxWidth/density,constraints.maxHeight/density)))
                put("viewport",JSONArray(listOf(viewport.left/density,viewport.top/density,viewport.width/density,viewport.height/density)))
                put("fitBounds",JSONArray(listOf(fitBounds.horizontal,fitBounds.vertical)))
                put("frameInterval",fluidity.minimumUpdateIntervalNs/1_000_000.0)
                put("exclusions",JSONArray().apply { frameExclusions.forEach { e ->
                    put(JSONArray(listOf(e.left/density,e.top/density,e.width/density,e.height/density))) } })
            }.toString()
        }
        val frozenFrame=SpaceCompassGlobeFraming(constraints.maxWidth.toDouble(),constraints.maxHeight.toDouble(),
            (captureViewport.top+captureViewport.height/2)/constraints.maxHeight,
            spaceCompassGlobeFitRadius(captureViewport,captureBounds,capturePose.zoom,24.0*density)/constraints.maxWidth,
            guideTextSizeByHeight=12.0*density/previewHeight)
        SideEffect { onCaptureFrame(frozenFrame) }
        AndroidView(factory={ SpaceCompassGlobeWebView(it,flat,sky).also { web -> native=web
            web.onPose={ value -> changePose(value) }
            web.pose={ currentPose }
            web.onTap={ id ->
                val target=latestTargets.getOrNull(id)
                if(target==null) tap() else {
                    val (track,dot)=target
                    if(dot.current) state.toggleCurrent(track.item.body,if(dot.antipode) dot.point.antipodalDirection() else dot.point,track.item.path,
                        track.item.path?.zone ?: java.time.ZoneId.of(currentZone))
                    else track.item.path?.let { path ->
                        val original=if(dot.antipode) dot.point.antipodalDirection() else dot.point
                        state.togglePoint(path,original)
                    }
                }
            }
            web.onDoubleTap={ doubleTap() }
        } },modifier=Modifier.fillMaxSize(),update={ it.interactionFrame=if(flat) viewport else pointingFrame; it.setScene(document.json,frameJson,resumed,pose,axisLocked,frameExclusions.map { e ->
            SpaceCompassSunSceneFrame(e.left/density,e.top/density,e.width/density,e.height/density) },flatMap=flat) },onRelease={ it.dispose() })
        if (showDirectionNote) Layout(content = {
            SpaceCompassGlobeDirectionNote(Modifier.onGloballyPositioned {
                val rect=it.boundsInParent()
                noteFrame=SpaceCompassSunSceneFrame(rect.left.toDouble(),rect.top.toDouble(),rect.width.toDouble(),rect.height.toDouble())
            })
        },modifier=Modifier.fillMaxSize()) { children, limits ->
            val captionWidth=(pointingFrame?.width ?: limits.maxWidth.toDouble()).roundToInt()
                .coerceIn(1,limits.maxWidth.coerceAtLeast(1))
            val caption=children.single().measure(limits.copy(minWidth=0,minHeight=0,maxWidth=captionWidth))
            val top=(limits.maxHeight-noteBottomInset.roundToPx()-caption.height)
                .coerceIn(0,(limits.maxHeight-caption.height).coerceAtLeast(0))
            layout(limits.maxWidth,limits.maxHeight) {
                caption.place(((pointingFrame?.left ?: 0.0)+(captionWidth-caption.width)/2).roundToInt(),top)
            }
        }
        LaunchedEffect(showDirectionNote) { if (!showDirectionNote) noteFrame=null }
    }
    DisposableEffect(Unit) { onDispose { native?.dispose();native=null } }
}

/** Shared single-line caption; its placement never changes the Earth's optical frame. */
@Composable
internal fun SpaceCompassGlobeDirectionNote(modifier: Modifier = Modifier) {
    val note=stringResource(R.string.globe_direction_note)
    val measurer=rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val density=LocalDensity.current
        val available=with(density) { (maxWidth-24.dp).toPx() }.coerceAtLeast(1f)
        val font=remember(note,measurer,available) {
            val width=measurer.measure(note,TextStyle(fontSize=10.sp,letterSpacing=0.sp),softWrap=false,
                maxLines=1,constraints=Constraints()).size.width.coerceAtLeast(1)
            (10f*(available/width).coerceAtMost(1f)).sp
        }
        Text(note,Modifier.padding(horizontal=12.dp,vertical=2.dp),color=Color.White.copy(alpha=.72f),
            fontSize=font,lineHeight=font*1.3f,letterSpacing=0.sp,maxLines=1,softWrap=false,textAlign=TextAlign.Center)
    }
}

/** Trusted bundled HTML only. JS returns a numeric hit ID; it cannot call Android or browse anywhere. */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
internal class SpaceCompassGlobeWebView(context: Context,initialFlat: Boolean=false,initialSky: Boolean=false): WebView(context) {
    var pose: () -> SpaceCompassGlobePose = { SpaceCompassGlobePose() }
    var onPose: (SpaceCompassGlobePose) -> Unit = {}
    var onTap: (Int) -> Unit = {}
    var onDoubleTap: () -> Unit = {}
    private var flatMap=initialFlat
    private var ready=false;private var disposed=false
    private val updates=SpaceCompassGlobeUpdates()
    private var sendScheduled=false
    private val sendFrame=Runnable { sendScheduled=false;send() }
    private var scaling=false
    private var gestureExclusions=emptyList<SpaceCompassSunSceneFrame>()
    private fun gestureAllowed(event: MotionEvent)=spaceCompassOverlayGestureAllowed(
        SpaceCompassSunScenePoint((event.x/resources.displayMetrics.density).toDouble(),
            (event.y/resources.displayMetrics.density).toDouble()),gestureExclusions,SPACE_COMPASS_OVERLAY_GESTURE_MARGIN_DP)
    var interactionFrame: SpaceCompassSunSceneFrame? = null
    private var axisLocked=true
    private var framePose=SpaceCompassGlobePose();private var sentPose="";private var activeView=true
    private val scale=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaling=true
            val next=pose().copy(zoom=pose().zoom*detector.scaleFactor)
            onPose(if(flatMap) spaceCompassGlobeMapPose(next,interactionFrame?.width,interactionFrame?.height) else next.normalized())
            return true
        }
    })
    private val gestures=GestureDetector(context,object: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent)=true
        override fun onScroll(e1: MotionEvent?,e2: MotionEvent,dx: Float,dy: Float): Boolean {
            if(!scaling&&e2.pointerCount==1) {
                val w=interactionFrame?.width ?: width.toDouble();val h=interactionFrame?.height ?: height.toDouble()
                onPose(if(flatMap) spaceCompassGlobeMapDrag(pose(),dx.toDouble(),dy.toDouble(),w,h)
                    else spaceCompassGlobeDrag(pose(),dx.toDouble(),dy.toDouble(),w,h,axisLocked))
            }
            return true
        }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if(ready&&gestureAllowed(e)) {
                send()
                evaluateJavascript("window.pickGlobePoint(${e.x/resources.displayMetrics.density},${e.y/resources.displayMetrics.density})") {
                    if(!disposed) onTap(it.toIntOrNull() ?: -1)
                }
            }
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean { if(gestureAllowed(e)) onDoubleTap();return true }
    }).apply { setIsLongpressEnabled(false) }
    init {
        setBackgroundColor(AndroidColor.TRANSPARENT);overScrollMode=View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled=false;isHorizontalScrollBarEnabled=false
        importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        settings.javaScriptEnabled=true;settings.blockNetworkLoads=true
        settings.allowFileAccess=false;settings.allowContentAccess=false;settings.domStorageEnabled=false
        settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.textZoom=100
        if(android.os.Build.VERSION.SDK_INT>=29) isForceDarkAllowed=false
        if(android.os.Build.VERSION.SDK_INT>=33) settings.isAlgorithmicDarkeningAllowed=false
        webViewClient=object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?,request: WebResourceRequest?)=true
            override fun shouldInterceptRequest(view: WebView?,request: WebResourceRequest?): WebResourceResponse? =
                if(request?.url?.scheme=="data") null else WebResourceResponse("text/plain","UTF-8",ByteArrayInputStream(ByteArray(0)))
            override fun onPageFinished(view: WebView?,url: String?) { ready=true;updates.invalidate();scheduleSend() }
        }
        fun image(name: String)="data:image/webp;base64,"+Base64.encodeToString(context.assets.open("screensaver/$name.webp").use { it.readBytes() },Base64.NO_WRAP)
        val adapter="window.spaceCompassInitialFlat=$initialFlat;window.spaceCompassInitialSky=$initialSky;\n"+context.assets.open("globe/globe-navigation.js").bufferedReader().use { it.readText() }
        val html=context.assets.open("screensaver/mondiversi-space.html").bufferedReader().use { it.readText() }
            .replace("__EARTH_DAY__",image("earth-day")).replace("__EARTH_NIGHT__",image("earth-night"))
            .replace("__NATIVE_NIGHT__","true").replace("__NATIVE_LIGHT_HOUR__","12")
            .replace("__NATIVE_LOCATION__","null").replace("__NATIVE_EARTH_ORIENTATION__","[0,0]")
            .replace("__NATIVE_SUN_DIRECTION__","[0,0,1]").replace("__ELAPSED_SECONDS__","0")
            .replace("</body>","<script>$adapter</script></body>")
            // Software fallback must redraw immediately while rotating this persistent view.
            .replace("(globeNavigation.active?", "(true?")
        val embedded=html.replace("width*.5*dpr,height*.5*dpr","(window.spaceCompassGlobeCenter?window.spaceCompassGlobeCenter(width,height):[width*.5,height*.5])[0]*dpr,(height-(window.spaceCompassGlobeCenter?window.spaceCompassGlobeCenter(width,height):[width*.5,height*.5])[1])*dpr")
            .replace("x=width*.5+point.x*diameter*.5,y=height*.5-point.y*diameter*.5",
                "x=(window.spaceCompassGlobeCenter?window.spaceCompassGlobeCenter(width,height):[width*.5,height*.5])[0]+point.x*diameter*.5,y=(window.spaceCompassGlobeCenter?window.spaceCompassGlobeCenter(width,height):[width*.5,height*.5])[1]-point.y*diameter*.5")
        loadDataWithBaseURL("https://spacecompass.invalid/globe/",embedded,"text/html","UTF-8",null)
    }
    private fun scheduleSend() {
        if(ready&&!disposed&&activeView&&!sendScheduled) { sendScheduled=true;postOnAnimation(sendFrame) }
    }
    private fun send() {
        if(!ready||disposed||!activeView)return
        updates.drain()?.let { evaluateJavascript("window.updateGlobe($it)",null) }
    }
    fun setScene(content: String,frame: String,active: Boolean,pose: SpaceCompassGlobePose,
        locked: Boolean,exclusions: List<SpaceCompassSunSceneFrame>,flatMap: Boolean = false) {
        this.flatMap=flatMap
        axisLocked=locked;gestureExclusions=exclusions
        updates.update(content,frame,pose.json().toString())
        if(active!=activeView) {
            activeView=active
            if(active)onResume() else { removeCallbacks(sendFrame);sendScheduled=false;onPause() }
        }
        scheduleSend()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN) scaling=false
        scale.onTouchEvent(event);gestures.onTouchEvent(event);return true
    }
    fun dispose() { if(!disposed) { disposed=true;removeCallbacks(sendFrame);sendScheduled=false;stopLoading();onPause();destroy() } }
}
