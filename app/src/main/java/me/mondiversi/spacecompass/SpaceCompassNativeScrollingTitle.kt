package me.mondiversi.spacecompass

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.HorizontalScrollView
import android.widget.TextView
import kotlin.math.roundToInt

/** The native map window keeps the same soft edges and bidirectional title motion as other pages. */
internal class SpaceCompassNativeScrollingTitle(context: Context, title: String, color: Int,
    density: Float, fontScale: Float, private val rtl: Boolean) : HorizontalScrollView(context) {
    private val label = TextView(context).apply {
        text = title
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * density * fontScale)
        typeface = Typeface.DEFAULT_BOLD
        setSingleLine(true)
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
    }
    private var distance = 0
    private var animation: ValueAnimator? = null
    private val forward = Runnable { travel(true) }
    private val backward = Runnable { travel(false) }

    init {
        layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        isHorizontalScrollBarEnabled = false
        isHorizontalFadingEdgeEnabled = true
        setFadingEdgeLength((SpaceCompassScrollingFadeDp * density).roundToInt())
        overScrollMode = View.OVER_SCROLL_NEVER
        addView(label, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun stopMotion() {
        removeCallbacks(forward)
        removeCallbacks(backward)
        animation?.cancel()
        animation = null
    }

    private fun restartMotion() {
        stopMotion()
        distance = (label.measuredWidth - width + paddingLeft + paddingRight).coerceAtLeast(0)
        scrollTo(if (rtl) distance else 0, 0)
        if (distance > 0 && isAttachedToWindow && windowVisibility == View.VISIBLE && isShown) {
            postDelayed(forward, SpaceCompassScrollingInitialPauseMs)
        }
    }

    private fun travel(towardEnd: Boolean) {
        if (distance <= 0 || !isAttachedToWindow || windowVisibility != View.VISIBLE || !isShown) return
        val target = if (towardEnd != rtl) distance else 0
        animation = ValueAnimator.ofInt(scrollX, target).apply {
            duration = spaceCompassScrollingTravelDuration(distance).toLong()
            interpolator = LinearInterpolator()
            addUpdateListener { scrollTo(it.animatedValue as Int, 0) }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled && isAttachedToWindow && windowVisibility == View.VISIBLE && isShown) {
                        postDelayed(if (towardEnd) backward else forward,
                            if (towardEnd) SpaceCompassScrollingEndPauseMs else SpaceCompassScrollingStartPauseMs)
                    }
                }
            })
            start()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val nextDistance = (label.measuredWidth - width + paddingLeft + paddingRight).coerceAtLeast(0)
        if (changed || nextDistance != distance) restartMotion()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { restartMotion() }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (isAttachedToWindow) {
            if (visibility == View.VISIBLE) restartMotion() else stopMotion()
        }
    }

    override fun onDetachedFromWindow() {
        stopMotion()
        super.onDetachedFromWindow()
    }
}
