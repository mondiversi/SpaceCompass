package me.mondiversi.spacecompass

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.UUID

private class SpaceCompassObserverMapAnchor { var placement: SpaceCompassObserverMapPlacement? = null }

/** Keep the Leaflet map in the editor, with native drawing and the scroll slot's exact visible bounds. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SpaceCompassObserverPositionMap(latitude: Double?, longitude: Double?,
    selectedPoint: Pair<Double, Double>?, enabled: Boolean, onPick: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val root = LocalView.current.rootView as ViewGroup
    val density = LocalDensity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val saver = LocalSpaceCompassScreensaverActive.current
    val background = spaceCompassSettingsCardColor()
    val accent = MaterialTheme.colorScheme.primary
    val dark = background.luminance() < .4f
    val zoomIn = stringResource(R.string.celestial_view_zoom_in)
    val zoomOut = stringResource(R.string.celestial_view_zoom_out)
    val choose by rememberUpdatedState(onPick)
    val valid = if (spaceCompassPositionMapUrl(latitude, longitude) != null) latitude!! to longitude!! else null
    val requested = selectedPoint ?: valid
    var settled by remember { mutableStateOf(requested) }
    LaunchedEffect(requested) { delay(SPACE_COMPASS_OBSERVER_METADATA_DELAY_MS); settled = requested }
    val shown by rememberUpdatedState(settled)
    val anchor = remember { SpaceCompassObserverMapAnchor() }
    var revision by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var layer by remember { mutableStateOf<SpaceCompassObserverMapLayer?>(null) }
    var lastApplied by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    fun updatePosition(view: WebView, point: Pair<Double, Double>?) {
        val script = if (point == null)
            "if(window.map&&window.marker){map.removeLayer(marker);map.setView([0,0],2,{animate:false});}"
        else "if(window.map&&window.marker){marker.setLatLng([${point.first},${point.second}]).addTo(map);" +
            "map.setView([${point.first},${point.second}],Math.max(5,map.getZoom()),{animate:false});}"
        view.evaluateJavascript("if(window.map){map.invalidateSize();}$script", null)
        lastApplied = point
    }
    DisposableEffect(context, root, dark, zoomIn, zoomOut, revision, density.density) {
        val token = UUID.randomUUID().toString()
        failed = false; ready = false
        val native = SpaceCompassObserverMapLayer(context, root, background.toArgb(), accent.toArgb(),
            0f, with(density) { 3.dp.roundToPx() })
        layer = native
        native.web.apply {
            setBackgroundColor(background.toArgb())
            if (android.os.Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
            if (android.os.Build.VERSION.SDK_INT >= 33) settings.isAlgorithmicDarkeningAllowed = false
            settings.userAgentString += " SpaceCompass/${BuildConfig.VERSION_NAME}"
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    native.loading(true); failed = false; ready = false
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    native.loading(false); ready = true
                    view?.let { updatePosition(it, shown) }
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) { failed = true; native.loading(false) }
                }
                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                    if (request?.isForMainFrame == true) { failed = true; native.loading(false) }
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    if (uri.scheme == "spacecompass" && uri.path == "/$token") {
                        if (uri.host == "pick") {
                            val a = uri.getQueryParameter("latitude")?.toDoubleOrNull()
                            val b = uri.getQueryParameter("longitude")?.toDoubleOrNull()
                            if (a != null && b != null && a.isFinite() && b.isFinite() &&
                                a in -90.0..90.0 && b in -180.0..180.0) choose(a, b)
                        } else if (uri.host == "tiles-error") { failed = true; native.loading(false) }
                        return true
                    }
                    if (request.hasGesture() && uri.scheme == "https") runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                    return true
                }
            }
            addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                if (ready) (view as WebView).evaluateJavascript("if(window.map){map.invalidateSize();}", null)
            }
            post {
                val point = shown
                val url = spaceCompassPositionMapUrl(point?.first ?: 0.0, point?.second ?: 0.0)!!
                loadDataWithBaseURL("https://www.openstreetmap.org/",
                    spaceCompassPositionMapHtml(context, url, dark, true, token, zoomIn, zoomOut,
                        positionKnown = point != null), "text/html", "UTF-8", null)
            }
        }
        native.place(anchor.placement, !saver)
        onDispose { if (layer === native) layer = null; native.release() }
    }
    DisposableEffect(layer, lifecycle, saver) {
        val view = layer?.web
        val listener = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !saver) view?.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) view?.onPause()
        }
        lifecycle.addObserver(listener)
        if (saver) view?.onPause()
        onDispose { lifecycle.removeObserver(listener) }
    }
    SideEffect {
        layer?.let { native ->
            native.place(anchor.placement, !failed && !saver)
            native.web.isEnabled = enabled
            if (ready && lastApplied != shown) updatePosition(native.web, shown)
        }
    }
    val height = (LocalConfiguration.current.screenHeightDp * .35f).dp.coerceIn(180.dp, 240.dp)
    Box(Modifier.fillMaxWidth().height(height).clipToBounds().background(background)
        .testTag("observer-position-map").onGloballyPositioned { coordinates ->
            val origin = coordinates.localToWindow(Offset.Zero)
            val placement = SpaceCompassObserverMapPlacement(origin.x, origin.y,
                coordinates.size.width, coordinates.size.height, coordinates.boundsInWindow())
            anchor.placement = placement
            layer?.place(placement, !failed && !saver)
        }) {
        if (failed) Column(Modifier.fillMaxSize().background(background).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            SpaceCompassSettingsDescription(stringResource(R.string.position_map_unavailable))
            SpaceCompassObserverAction(stringResource(R.string.sun_finder_retry), { revision++ },
                icon = R.drawable.ic_refresh)
        }
    }
}
