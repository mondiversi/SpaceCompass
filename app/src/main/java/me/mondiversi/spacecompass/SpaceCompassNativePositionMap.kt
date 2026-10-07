package me.mondiversi.spacecompass

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.content.res.ColorStateList
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

/** A native window avoids legacy WebView/Compose canvas incompatibilities. */
internal class SpaceCompassNativePositionMap(
    context: Context, private var url: String?,
    private val title: String, private val backLabel: String,
    private val unavailable: String, private val retry: String,
    private val density: Float, private val fontScale: Float, private val rtl: Boolean,
    private val backgroundArgb: Int, private val foregroundArgb: Int, private val accentArgb: Int, private val scrollbarArgb: Int,
    private val dark: Boolean, private val onClose: () -> Unit,
    private var details: SpaceCompassPositionDetailsData? = null,
    private val onPick: ((Double, Double) -> Unit)? = null, private val pickLabel: String = "", private val zoomIn: String = "", private val zoomOut: String = "",
    private val onAccentArgb: Int = android.graphics.Color.WHITE
) : Dialog(context, android.R.style.Theme_Material_Light_NoActionBar) {
    private val pickToken = java.util.UUID.randomUUID().toString()
    private var picked: Pair<Double, Double>? = null
    private var pickButton: Button? = null
    private fun loadMap(view: WebView, target: String) {
        view.loadDataWithBaseURL("https://www.openstreetmap.org/",
            spaceCompassPositionMapHtml(context, target, dark, onPick != null, pickToken, zoomIn, zoomOut), "text/html", "UTF-8", null)
    }
    private var map: WebView? = null
    private var table: SpaceCompassNativePositionTable? = null
    fun updateDetails(data: SpaceCompassPositionDetailsData, firstFixUrl: String?) {
        details = data
        table?.update(data)
        // A newly acquired fix may enable the map; subsequent GPS jitter preserves pan/zoom.
        if (url == null && firstFixUrl != null) { url = firstFixUrl; map?.let { loadMap(it, firstFixUrl) } }
    }
    private fun dp(value: Float) = (value * density).roundToInt()
    private fun textSize(view: TextView, value: Float) =
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, value * density * fontScale)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            setBackgroundColor(backgroundArgb)
        }
        val direction = if (rtl) androidx.compose.ui.unit.LayoutDirection.Rtl
            else androidx.compose.ui.unit.LayoutDirection.Ltr
        val padding = SpaceCompassTitleBarContentPadding
        val backSlot = SpaceCompassTitleBackButtonSize.value
        val backTarget = maxOf(48f, backSlot)
        val extraTargetInset = (dp(backTarget) - dp(backSlot)) / 2
        val toolbar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(backSlot + padding.calculateTopPadding().value + padding.calculateBottomPadding().value)
            // Expand the touch area around the shared 40 dp slot, without moving its center.
            setPaddingRelative(dp(padding.calculateStartPadding(direction).value) - extraTargetInset, 0,
                dp(padding.calculateEndPadding(direction).value), 0)
        }
        toolbar.addView(ImageButton(context).apply {
            contentDescription = backLabel
            setImageDrawable(MapBackDrawable(foregroundArgb, density, rtl))
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            setOnClickListener { onClose() }
        }, LinearLayout.LayoutParams(dp(backTarget), dp(backTarget)))
        toolbar.addView(SpaceCompassNativeScrollingTitle(context, title, foregroundArgb,
            density, fontScale, rtl), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(backSlot) - dp(backTarget) + extraTargetInset
        })
        // Keep the normal 48 dp toolbar, allowing large accessibility fonts their full height.
        root.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val content = FrameLayout(context).apply {
            tag = "position-map-frame"
            clipToOutline = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat(); setColor(backgroundArgb)
            }
        }
        if (details == null) {
            root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        } else {
            val readout = SpaceCompassNativePositionTable(context, density, fontScale, foregroundArgb, accentArgb, dark, scrollbarArgb)
            table = readout
            val split = object : LinearLayout(context) {
                private val splitChildren = arrayOf(content, readout)
                private val verticalParams = arrayOf(
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10f) })
                private val horizontalParams = arrayOf(
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { leftMargin = dp(12f) })
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    // Match the celestial detail viewport, insets and gap in both orientations.
                    val availableWidth = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
                    val availableHeight = View.MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
                    val horizontal = availableWidth > availableHeight && availableWidth >= dp(540f)
                    val desired = if (horizontal) HORIZONTAL else VERTICAL
                    if (orientation != desired) orientation = desired
                    val parameters = if (horizontal) horizontalParams else verticalParams
                    for (index in splitChildren.indices) {
                        val child = splitChildren[index]
                        if (child.layoutParams !== parameters[index]) child.layoutParams = parameters[index]
                    }
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                }
            }.apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                setPadding(dp(10f), 0, dp(10f), dp(6f))
            }
            readout.layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            split.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            split.addView(readout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            root.addView(split, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            readout.update(details!!)
        }
        if (onPick != null) {
            val button = Button(context).apply {
                text = pickLabel; isAllCaps = false; minimumHeight = dp(48f); minHeight = dp(48f); isEnabled = false
                textSize(this, 14f)
                typeface = if (android.os.Build.VERSION.SDK_INT >= 28)
                    android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, 600, false)
                else android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                val disabled = intArrayOf(-android.R.attr.state_enabled)
                val pressed = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
                val default = intArrayOf()
                val colors = ColorStateList(arrayOf(disabled, pressed, default), intArrayOf(
                    if (dark) 0xffa1a5ae.toInt() else 0xff808790.toInt(), onAccentArgb, accentArgb))
                setTextColor(colors)
                fun surface(enabled: Boolean, pressed: Boolean = false) = GradientDrawable().apply {
                    cornerRadius = dp(28f).toFloat()
                    setColor(if (pressed) accentArgb else if (enabled) android.graphics.Color.TRANSPARENT else
                        if (dark) 0xff33343c.toInt() else 0xffecedef.toInt())
                    setStroke(dp(1f), ColorUtils.setAlphaComponent(if (enabled) accentArgb else foregroundArgb,
                        ((if (enabled) { if (dark) .48f else .38f } else { if (dark) .20f else .12f }) * 255).roundToInt()))
                }
                val states = StateListDrawable().apply {
                    addState(disabled, surface(false)); addState(pressed, surface(true, pressed = true)); addState(default, surface(true))
                }
                val rippleColors = ColorStateList(arrayOf(pressed, default), intArrayOf(
                    ColorUtils.setAlphaComponent(onAccentArgb, 40), ColorUtils.setAlphaComponent(accentArgb, 32)))
                background = RippleDrawable(rippleColors, states, null)
                setPadding(dp(24f), dp(8f), dp(24f), dp(8f))
                val check = ContextCompat.getDrawable(context, R.drawable.ic_check)?.mutate()?.apply {
                    setTintList(colors); setBounds(0, 0, dp(20f), dp(20f))
                }
                // The leading drawable is fixed; Android centers text in the remaining content area.
                contentDescription = pickLabel
                setCompoundDrawablesRelative(check, null, null, null)
                compoundDrawablePadding = dp(8f)
                gravity = Gravity.CENTER
                setOnClickListener { picked?.let { (a, b) -> onPick.invoke(a, b) } }
            }
            pickButton = button
            root.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(8f); marginEnd = dp(8f); topMargin = dp(4f); bottomMargin = dp(4f)
            })
        }
        val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
        }
        val failure = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            setBackgroundColor(backgroundArgb)
            visibility = View.GONE
        }
        failure.addView(TextView(context).apply {
            text = unavailable
            setTextColor(foregroundArgb)
            textSize(this, 13f)
            gravity = Gravity.CENTER
        })
        failure.addView(Button(context).apply {
            text = retry
            isAllCaps = false
            setTextColor(accentArgb)
            textSize(this, 14f)
            minHeight = dp(48f)
            setOnClickListener { failure.visibility = View.GONE; map?.let { view -> url?.let { loadMap(view, it) } } }
        })
        val webView = WebView(context).apply {
            setBackgroundColor(backgroundArgb)
            // The bundled page owns both palettes. Prevent a second platform color inversion.
            if (android.os.Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
            if (android.os.Build.VERSION.SDK_INT >= 33) settings.isAlgorithmicDarkeningAllowed = false
            settings.userAgentString = settings.userAgentString + " SpaceCompass/${BuildConfig.VERSION_NAME}"
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    progress.visibility = View.VISIBLE
                    failure.visibility = View.GONE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    progress.visibility = View.GONE
                }
                private fun fail() { failure.visibility = View.VISIBLE; progress.visibility = View.GONE }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) fail()
                }
                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                    if (request?.isForMainFrame == true) fail()
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    if (uri.scheme == "spacecompass" && uri.path == "/$pickToken") {
                        if (uri.host == "pick" && onPick != null) {
                            val a = uri.getQueryParameter("latitude")?.toDoubleOrNull()
                            val b = uri.getQueryParameter("longitude")?.toDoubleOrNull()
                            if (a != null && b != null && a.isFinite() && b.isFinite() && a in -90.0..90.0 && b in -180.0..180.0) {
                                picked = a to b; pickButton?.isEnabled = true
                            }
                        } else if (uri.host == "tiles-error") fail()
                        return true
                    }
                    if (uri.scheme == "https" && uri.host == "www.openstreetmap.org" && uri.path == "/export/embed.html") return false
                    if (request.hasGesture() && uri.scheme == "https") runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                    return true
                }
            }
        }
        map = webView
        content.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        content.addView(progress, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3f), Gravity.TOP))
        content.addView(failure, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        setOnCancelListener { onClose() }
        window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(backgroundArgb))
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (android.os.Build.VERSION.SDK_INT < 35) {
                window.statusBarColor = backgroundArgb
                window.navigationBarColor = backgroundArgb
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                insets
            }
            WindowCompat.getInsetsController(window, root).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        }
        // Attach/measure the native hierarchy before requesting the initial viewport.
        webView.post { url?.let { loadMap(webView, it) } ?: run { failure.visibility = View.VISIBLE; progress.visibility = View.GONE } }
    }

    override fun show() {
        super.show()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun releaseMap() {
        dismiss()
        map?.let { view ->
            view.stopLoading()
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        map = null
    }
}

private class MapBackDrawable(color: Int, private val density: Float, private val rtl: Boolean) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE
        strokeWidth = SpaceCompassTitleBackIconStrokeWidth.value * density
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    override fun getIntrinsicWidth() = (SpaceCompassTitleBackIconSize.value * density).roundToInt()
    override fun getIntrinsicHeight() = (SpaceCompassTitleBackIconSize.value * density).roundToInt()
    override fun draw(canvas: Canvas) {
        val size = SpaceCompassTitleBackIconSize.value * density
        val offset = SpaceCompassTitleBackIconOpticalOffset.value * density
        // Use the Compose toolbar's optical placement as well as its glyph size and stroke.
        fun x(fraction: Float) = bounds.exactCenterX() +
            ((fraction - .5f) * size + offset) * if (rtl) -1f else 1f
        fun y(fraction: Float) = bounds.exactCenterY() + (fraction - .5f) * size
        canvas.drawPath(Path().apply {
            moveTo(x(.68f), y(.20f)); lineTo(x(.34f), y(.50f)); lineTo(x(.68f), y(.80f))
        }, paint)
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
