package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.widget.ScrollView
import kotlin.math.roundToInt

/** Match the celestial detail overlay rather than the platform's fading scrollbar. */
internal open class SpaceCompassNativeScrollView(context: Context, private val density: Float,
    scrollbarArgb: Int) : ScrollView(context) {
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = scrollbarArgb }
    private var draggingScrollbar = false

    init {
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setWillNotDraw(false)
    }

    private fun maximumScroll() = ((getChildAt(0)?.bottom ?: 0) + paddingBottom - height).coerceAtLeast(0)

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        val maximum = maximumScroll()
        if (maximum <= 2f * density || width <= 0 || height <= 0) return
        val track = height.toFloat()
        val thumbHeight = spaceCompassScrollbarThumbHeight(track, track / (track + maximum), 28f * density)
        val thumbWidth = 3f * density
        val left = scrollX + width - thumbWidth - 2f * density
        val top = scrollY + (track - thumbHeight) * (scrollY.toFloat() / maximum).coerceIn(0f, 1f)
        // ScrollView's canvas uses content coordinates; compensate to keep the overlay in the viewport.
        canvas.drawRoundRect(left, top, left + thumbWidth, top + thumbHeight, thumbWidth, thumbWidth, thumb)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            draggingScrollbar = maximumScroll() > 2f * density && event.x >= width - 16f * density
        }
        if (draggingScrollbar) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!draggingScrollbar) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Let ScrollView stop an existing fling before the thumb takes ownership.
                if (event.actionMasked == MotionEvent.ACTION_DOWN) super.onTouchEvent(event)
                val target = ((event.y / height.coerceAtLeast(1)).coerceIn(0f, 1f) * maximumScroll()).roundToInt()
                scrollTo(0, target)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                try { super.onTouchEvent(cancel) } finally { cancel.recycle() }
                draggingScrollbar = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
}
