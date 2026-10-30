package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

internal data class SpaceCompassObserverMapPlacement(val left: Float, val top: Float,
    val width: Int, val height: Int, val visible: Rect)

/** A real native sibling avoids sending Chromium's canvas through the Compose renderer. */
internal class SpaceCompassObserverMapLayer(context: Context, private val root: ViewGroup,
    background: Int, accent: Int, radius: Float, progressHeight: Int) {
    val frame = FrameLayout(context).apply {
        visibility = View.GONE
        clipChildren = true
        clipToOutline = true
        this.background = GradientDrawable().apply { setColor(background); cornerRadius = radius }
    }
    val web = WebView(context)
    private val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        isIndeterminate = true
        indeterminateTintList = android.content.res.ColorStateList.valueOf(accent)
    }
    init {
        frame.addView(web, FrameLayout.LayoutParams(1, 1))
        frame.addView(progress, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            progressHeight, Gravity.TOP))
        root.addView(frame, FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.LEFT))
    }
    fun loading(value: Boolean) { progress.visibility = if (value) View.VISIBLE else View.GONE }
    fun place(placement: SpaceCompassObserverMapPlacement?, visible: Boolean) {
        if (!visible || placement == null || placement.visible.width <= 1 || placement.visible.height <= 1) {
            frame.visibility = View.GONE
            return
        }
        val clipped = placement.visible
        val width = clipped.width.roundToInt().coerceAtLeast(1)
        val height = clipped.height.roundToInt().coerceAtLeast(1)
        val location = IntArray(2)
        root.getLocationInWindow(location)
        val frameSize = frame.layoutParams
        if (frameSize.width != width || frameSize.height != height) {
            frame.layoutParams = FrameLayout.LayoutParams(width, height, Gravity.TOP or Gravity.LEFT)
        }
        frame.translationX = clipped.left - location[0]
        frame.translationY = clipped.top - location[1]
        val webSize = web.layoutParams
        if (webSize.width != placement.width || webSize.height != placement.height) {
            web.layoutParams = FrameLayout.LayoutParams(placement.width, placement.height)
        }
        // Crop the native hit region too, so a scrolled map cannot cover the page toolbar.
        web.translationX = placement.left - clipped.left
        web.translationY = placement.top - clipped.top
        frame.visibility = View.VISIBLE
    }
    fun release() {
        root.removeView(frame)
        web.stopLoading()
        web.webViewClient = android.webkit.WebViewClient()
        frame.removeAllViews()
        web.destroy()
    }
}
