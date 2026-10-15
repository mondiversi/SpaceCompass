package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.Gravity
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import java.io.ByteArrayInputStream
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Trusted, bundled animation only: no navigation, network, audio or JavaScript bridge. */
internal class SpaceCompassScreensaverView(context: Context, dark: Boolean, lightHour: Double, elapsedSeconds: Double,
    location: SpaceCompassScreensaverLocation?, season: SpaceCompassScreensaverSeason, onDismiss: () -> Unit) : FrameLayout(context) {
    private var night = dark
    private var solarHour = lightHour
    private var markerLocation = location
    private var globeSeason = season
    private var loaded = false
    private val web = WebView(context).apply {
        setBackgroundColor(Color.rgb(2, 5, 12))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        isFocusable = false
        isFocusableInTouchMode = false
        if (android.os.Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
        if (android.os.Build.VERSION.SDK_INT >= 33) settings.isAlgorithmicDarkeningAllowed = false
        settings.javaScriptEnabled = true
        settings.blockNetworkLoads = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.domStorageEnabled = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.mediaPlaybackRequiresUserGesture = true
        settings.textZoom = 100
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
                if (request?.url?.scheme == "data") null
                else WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            override fun onPageFinished(view: WebView?, url: String?) {
                loaded = true
                applyLighting()
                applyLocation()
                applySeason()
            }
        }
    }

    private val footer = SpaceCompassScreensaverFooter(context)
    private val returnHint = TextView(context).apply {
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        setShadowLayer(2 * resources.displayMetrics.density, 0f, resources.displayMetrics.density, Color.BLACK)
        // The full cover already supplies this label and owns the dismiss action.
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private var safeTop = 0
    private var safeLeft = 0
    private var safeRight = 0
    private var safeBottom = 0

    init {
        setBackgroundColor(Color.rgb(2, 5, 12))
        isClickable = true
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        // The scene has its own solar lighting; native white text must never be auto-inverted.
        if (android.os.Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
        setOnClickListener { onDismiss() }
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(footer)
        addView(returnHint)
        layoutHint()
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            safeTop = cutout.top; safeLeft = cutout.left; safeRight = cutout.right; safeBottom = cutout.bottom
            layoutFooter()
            layoutHint()
            insets
        }
        fun image(name: String) = "data:image/webp;base64," + Base64.encodeToString(
            context.assets.open("screensaver/$name.webp").use { it.readBytes() }, Base64.NO_WRAP)
        val html = context.assets.open("screensaver/mondiversi-space.html").bufferedReader().use { it.readText() }
            .replace("__EARTH_DAY__", image("earth-day"))
            .replace("__EARTH_NIGHT__", image("earth-night"))
            .replace("__NATIVE_NIGHT__", dark.toString())
            .replace("__NATIVE_LIGHT_HOUR__", lightHour.toString())
            .replace("__ELAPSED_SECONDS__", elapsedSeconds.toString())
            .replace("__NATIVE_LOCATION__", markerLocation?.javascript ?: "null")
            .replace("__NATIVE_EARTH_ORIENTATION__", globeSeason.javascript)
        web.loadDataWithBaseURL("https://spacecompass.invalid/screensaver/", html, "text/html", "UTF-8", null)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        footer.resize(width, height)
        val density = resources.displayMetrics.density
        returnHint.textSize = 14f * spaceCompassAutomaticScreenScale((minOf(width, height) / density).toInt())
        layoutFooter()
        layoutHint()
        post { footer.refreshAfterResize() }
    }

    private fun layoutFooter() {
        val margin = (16 * resources.displayMetrics.density).toInt()
        // Symmetric safe margins keep the stacked clock centered even with a side cutout.
        val sideMargin = maxOf(safeLeft, safeRight) + margin
        footer.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            leftMargin = sideMargin
            rightMargin = sideMargin
            bottomMargin = safeBottom + margin
        }
    }

    private fun layoutHint() {
        val margin = (16 * resources.displayMetrics.density).toInt()
        returnHint.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            leftMargin = maxOf(safeLeft, safeRight) + margin
            rightMargin = leftMargin
            topMargin = safeTop + margin
        }
    }

    fun updateHint(label: String) { if (returnHint.text != label) returnHint.text = label }

    fun updateInfo(info: SpaceCompassScreensaverInfo) { footer.update(info) }

    override fun onInterceptTouchEvent(event: MotionEvent?) = true
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }

    fun updateLighting(dark: Boolean, lightHour: Double) {
        if (night != dark || solarHour != lightHour) {
            night = dark; solarHour = lightHour; applyLighting()
        }
    }
    private fun applyLighting() {
        if (loaded) web.evaluateJavascript("window.setNativeLighting($night,$solarHour)", null)
    }

    fun updateLocation(value: SpaceCompassScreensaverLocation?) {
        if (markerLocation != value) { markerLocation = value; applyLocation() }
    }
    private fun applyLocation() {
        if (loaded) web.evaluateJavascript("window.setNativeLocation(${markerLocation?.javascript ?: "null"})", null)
    }

    fun updateSeason(value: SpaceCompassScreensaverSeason) {
        if (globeSeason != value) { globeSeason = value; applySeason() }
    }
    private fun applySeason() {
        if (loaded) web.evaluateJavascript("window.setNativeEarthOrientation(${globeSeason.javascript})", null)
    }

    fun release() {
        web.stopLoading()
        web.onPause()
        removeView(web)
        web.destroy()
    }
}
