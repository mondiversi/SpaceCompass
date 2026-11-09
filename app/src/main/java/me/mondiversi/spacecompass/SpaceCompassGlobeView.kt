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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
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
internal fun globeColor(value: Int) = "#%06x".format(value and 0xffffff)

@Composable
internal fun SpaceCompassGlobeView(snapshot: SpaceCompassPanoramaSnapshot, pose: SpaceCompassGlobePose,
    onPose: (SpaceCompassGlobePose) -> Unit, state: SpaceCompassSunDailyPathUiState,
    exclusions: List<SpaceCompassSunSceneFrame>, viewport: SpaceCompassSunSceneFrame, noteBottomInset: androidx.compose.ui.unit.Dp, labelMode: SpaceCompassPanoramaLabelMode, axisLocked: Boolean,
    resumed: Boolean,
    onTap: () -> Unit, onDoubleTap: () -> Unit, modifier: Modifier = Modifier) {
    val context=LocalContext.current;val resources=LocalResources.current;val dark=isSystemInDarkTheme()
    val currentPose by rememberUpdatedState(pose)
    val changePose by rememberUpdatedState(onPose)
    val tap by rememberUpdatedState(onTap);val doubleTap by rememberUpdatedState(onDoubleTap)
    val currentZone by rememberUpdatedState(snapshot.zoneId)
    val paths=snapshot.objects.map { it.path }
    val bodyKeys=snapshot.objects.map { it.body to it.comparison?.id }
    val curveCache=remember(snapshot.latitude,snapshot.longitude,snapshot.altitude,snapshot.remote) {
        java.util.concurrent.ConcurrentHashMap<SpaceCompassSunDailyPath,List<Pair<SpaceCompassGlobeVector,Boolean>>>()
    }
    val model by produceState<SpaceCompassGlobeModel?>(null,paths,bodyKeys,snapshot.timeMs/1000,
        snapshot.latitude,snapshot.longitude,snapshot.altitude,snapshot.selectedPoints,snapshot.showAntipodes,snapshot.remote) {
        val job=kotlin.coroutines.coroutineContext
        value=withContext(Dispatchers.Default) { buildSpaceCompassGlobeModel(snapshot,curveCache) { job.ensureActive() } }
    }
    val iconKeys=snapshot.objects.map { spaceCompassGlobeSpriteKey(it,snapshot.timeMs) }
    val icons by produceState<Map<String,String>>(emptyMap(),iconKeys) {
        value=withContext(Dispatchers.Default) { spaceCompassGlobeSprites(context.applicationContext,snapshot.objects,snapshot.timeMs) }
        withContext(Dispatchers.IO) { ensureSpaceCompassCelestialThumbnails(context.applicationContext,snapshot.objects.map { it.body }.toSet()) }
        value=withContext(Dispatchers.Default) { spaceCompassGlobeSprites(context.applicationContext,snapshot.objects,snapshot.timeMs) }
    }
    val fluidity=SpaceCompassSkyFluidity.fromStored(LocalSpaceCompassPresentationSettings.current[SPACE_COMPASS_SKY_FLUIDITY_KEY])
    val selection=androidx.compose.material3.MaterialTheme.colorScheme.primary.toArgb()
    val sun by produceState<SpaceCompassScreensaverSunlight?>(null,snapshot.timeMs/60_000) {
        value=withContext(Dispatchers.Default) { spaceCompassScreensaverSunlight(snapshot.timeMs) }
    }
    var native by remember { mutableStateOf<SpaceCompassGlobeWebView?>(null) }
    var noteFrame by remember { mutableStateOf<SpaceCompassSunSceneFrame?>(null) }
    val density=androidx.compose.ui.platform.LocalDensity.current.density
    val (sceneJson,targets)=remember(model,sun,dark,exclusions,noteFrame,viewport,labelMode,axisLocked,icons,fluidity,snapshot.timeMs/1000,
        snapshot.latitude,snapshot.longitude,snapshot.observerPositionKnown,
        snapshot.objects.map { it.name },snapshot.activeBody,snapshot.activeComparisonId,snapshot.showSkyReferences,selection,snapshot.formatting,density) {
    val targets=mutableListOf<Pair<SpaceCompassGlobeTrack,SpaceCompassGlobeDot>>()
    val scene=JSONObject().apply {
        put("pose",pose.json());put("extent",model?.extent ?: 1.55);put("dark",dark)
        put("guides",snapshot.showSkyReferences);put("selection",globeColor(selection))
        put("labels",labelMode!=SpaceCompassPanoramaLabelMode.NONE);put("axisLocked",axisLocked);put("background",false)
        put("frameInterval",fluidity.minimumUpdateIntervalNs/1_000_000.0)
        put("viewport",JSONArray(listOf(viewport.left/density,viewport.top/density,viewport.width/density,viewport.height/density)))
        put("sun",sun?.let { JSONArray(listOf(it.x,it.y,it.z)) } ?: JSONArray(listOf(0,0,1)))
        // Coarse/hidden captures suppress this marker; the live view uses the effective Location.
        put("location",if(snapshot.observerPositionKnown) JSONArray(listOf(snapshot.latitude,snapshot.longitude)) else JSONObject.NULL)
        put("tracks",JSONArray().apply { model?.tracks?.forEach { track ->
            put(JSONObject().apply {
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
                    val time=formatSpaceCompassPanoramaMarkerTime(dot.point.timeMs,track.item.referenceTime(snapshot.timeMs),snapshot.displayTimeZone,snapshot.formatting)
                    put(JSONObject().apply {
                        put("v",dot.vector.json());put("current",dot.current);put("selected",dot.selected)
                        put("antipode",dot.antipode);put("id",id);put("below",dot.point.position.elevationDegrees<0)
                        put("event",dot.point.event.name)
                        if(dot.point.event==SpaceCompassSunPathEvent.SUNRISE||dot.point.event==SpaceCompassSunPathEvent.SUNSET)
                            put("glyph",JSONArray().apply { spaceCompassHorizonEventGlyph(dot.point.event,0f,0f,1f).forEach { (x,y) -> put(JSONArray(listOf(x,y))) } })
                        put("label",if(!spaceCompassGlobeMainShowsLabel(labelMode,active,dot.current,dot.antipode,dot.point.events,dot.selected)) "" else
                            (if(dot.antipode) formatSpaceCompassPanoramaSelection(snapshot,SpaceCompassPanoramaSelectedPoint(
                                track.item.body,dot.point,track.item.path,dot.current,true,track.item.comparison)).lineSequence().first()
                                else if(dot.current) track.item.comparison?.let(snapshot::comparisonPointName) ?: snapshot.currentPointName
                                else event).let { if(it.isBlank()) time else "$it · $time" })
                    })
                } })
            })
        } })
    }
    scene.put("exclusions",JSONArray().apply { (exclusions+listOfNotNull(noteFrame)).forEach { e -> put(JSONArray(listOf(e.left/density,e.top/density,e.width/density,e.height/density))) } })
    scene.toString() to targets.toList()
    }
    val latestTargets by rememberUpdatedState(targets)
    val globeLabel=stringResource(R.string.view_mode_globe)
    BoxWithConstraints(modifier.testTag("earth-orbit-view").semantics {
        contentDescription=globeLabel
        customActions=latestTargets.filter { (_,dot) -> dot.current || dot.selected }.filter { !it.second.antipode }
            .map { (track,dot) -> CustomAccessibilityAction(track.item.name+" · "+
                formatSpaceCompassPanoramaMarkerTime(dot.point.timeMs,snapshot.timeMs,snapshot.displayTimeZone,snapshot.formatting)) {
                if(dot.current) state.toggleCurrent(track.item.body,dot.point,track.item.path)
                else track.item.path?.let { state.togglePoint(it,dot.point) };true
            } }
    }) {
        AndroidView(factory={ SpaceCompassGlobeWebView(it).also { web -> native=web
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
        } },modifier=Modifier.fillMaxSize(),update={ it.setScene(sceneJson,resumed,pose) },onRelease={ it.dispose() })
        val note=stringResource(R.string.globe_direction_note)
        val noteMeasurer=rememberTextMeasurer()
        val noteWidth=(constraints.maxWidth-24*density).roundToInt().coerceAtLeast(1)
        val noteSize=remember(note,noteMeasurer,noteWidth) {
            val measured=noteMeasurer.measure(note,TextStyle(fontSize=10.sp,letterSpacing=0.sp),softWrap=false,maxLines=1,
                constraints=Constraints()).size.width.coerceAtLeast(1)
            (10f*(noteWidth.toFloat()/measured).coerceAtMost(1f)).sp
        }
        Text(note,Modifier.align(Alignment.BottomCenter).padding(start=12.dp,end=12.dp,bottom=noteBottomInset).padding(vertical=2.dp)
            .onGloballyPositioned {
                val rect=it.boundsInParent()
                noteFrame=SpaceCompassSunSceneFrame(rect.left.toDouble(),rect.top.toDouble(),rect.width.toDouble(),rect.height.toDouble())
            },color=Color.White.copy(alpha=.72f),fontSize=noteSize,lineHeight=noteSize*1.3f,letterSpacing=0.sp,maxLines=1,softWrap=false,textAlign=TextAlign.Center)
    }
    DisposableEffect(Unit) { onDispose { native?.dispose();native=null } }
}

/** Trusted bundled HTML only. JS returns a numeric hit ID; it cannot call Android or browse anywhere. */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
internal class SpaceCompassGlobeWebView(context: Context): WebView(context) {
    var pose: () -> SpaceCompassGlobePose = { SpaceCompassGlobePose() }
    var onPose: (SpaceCompassGlobePose) -> Unit = {}
    var onTap: (Int) -> Unit = {}
    var onDoubleTap: () -> Unit = {}
    private var ready=false;private var disposed=false;private var scene="";private var sent=""
    private var scaling=false
    private var gestureExclusions=emptyList<SpaceCompassSunSceneFrame>()
    private fun gestureAllowed(event: MotionEvent)=spaceCompassOverlayGestureAllowed(
        SpaceCompassSunScenePoint((event.x/resources.displayMetrics.density).toDouble(),
            (event.y/resources.displayMetrics.density).toDouble()),gestureExclusions,SPACE_COMPASS_OVERLAY_GESTURE_MARGIN_DP)
    private var axisLocked=true
    private var framePose=SpaceCompassGlobePose();private var sentPose="";private var activeView=true
    private val scale=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaling=true;onPose(pose().copy(zoom=pose().zoom*detector.scaleFactor).normalized());return true
        }
    })
    private val gestures=GestureDetector(context,object: GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent)=true
        override fun onScroll(e1: MotionEvent?,e2: MotionEvent,dx: Float,dy: Float): Boolean {
            if(!scaling&&e2.pointerCount==1) onPose(spaceCompassGlobeDrag(pose(),dx.toDouble(),dy.toDouble(),
                width.toDouble(),height.toDouble(),axisLocked))
            return true
        }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if(ready&&gestureAllowed(e)) evaluateJavascript("window.pickGlobePoint(${e.x/resources.displayMetrics.density},${e.y/resources.displayMetrics.density})") {
                if(!disposed) onTap(it.toIntOrNull() ?: -1)
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
            override fun onPageFinished(view: WebView?,url: String?) { ready=true;send() }
        }
        fun image(name: String)="data:image/webp;base64,"+Base64.encodeToString(context.assets.open("screensaver/$name.webp").use { it.readBytes() },Base64.NO_WRAP)
        val adapter=context.assets.open("globe/globe-navigation.js").bufferedReader().use { it.readText() }
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
    private fun send() {
        if(!ready||disposed)return
        if(scene!=sent) { evaluateJavascript("window.setGlobeScene($scene)",null);sent=scene;sentPose="" }
        val next=framePose.json().toString()
        if(next!=sentPose) { evaluateJavascript("window.setGlobePose($next)",null);sentPose=next }
    }
    fun setScene(value: String,active: Boolean,pose: SpaceCompassGlobePose) { scene=value;framePose=pose
        val document=JSONObject(value)
        axisLocked=document.optBoolean("axisLocked",true)
        gestureExclusions=document.optJSONArray("exclusions")?.let { values ->
            (0 until values.length()).map { index -> values.getJSONArray(index).let {
                SpaceCompassSunSceneFrame(it.getDouble(0),it.getDouble(1),it.getDouble(2),it.getDouble(3))
            } }
        }.orEmpty()
        if(active!=activeView) { activeView=active;if(active)onResume() else onPause() }
        if(active)send() }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN) scaling=false
        scale.onTouchEvent(event);gestures.onTouchEvent(event);return true
    }
    fun dispose() { if(!disposed) { disposed=true;stopLoading();onPause();destroy() } }
}
