package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Color
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import java.io.ByteArrayInputStream

/** Trusted, bundled animation only: no navigation, network, audio or JavaScript bridge. */
internal class SpaceCompassScreensaverView(context: Context, dark: Boolean, lightHour: Double, elapsedSeconds: Double,
    onDismiss: () -> Unit) : FrameLayout(context) {
    private var night = dark
    private var solarHour = lightHour
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
            }
        }
    }

    init {
        setBackgroundColor(Color.rgb(2, 5, 12))
        isClickable = true
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        setOnClickListener { onDismiss() }
        addView(web, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        fun image(name: String) = "data:image/webp;base64," + Base64.encodeToString(
            context.assets.open("screensaver/$name.webp").use { it.readBytes() }, Base64.NO_WRAP)
        val html = context.assets.open("screensaver/mondiversi-space.html").bufferedReader().use { it.readText() }
            .replace("__EARTH_DAY__", image("earth-day"))
            .replace("__EARTH_NIGHT__", image("earth-night"))
            .replace("__NATIVE_NIGHT__", dark.toString())
            .replace("__NATIVE_LIGHT_HOUR__", lightHour.toString())
            .replace("__ELAPSED_SECONDS__", elapsedSeconds.toString())
        web.loadDataWithBaseURL("https://spacecompass.invalid/screensaver/", html, "text/html", "UTF-8", null)
    }

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

    fun release() {
        web.stopLoading()
        web.onPause()
        removeView(web)
        web.destroy()
    }
}
