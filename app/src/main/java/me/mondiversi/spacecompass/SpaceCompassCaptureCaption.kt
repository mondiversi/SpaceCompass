package me.mondiversi.spacecompass

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import kotlin.math.ceil

/** The same typography per image width in photo, panorama, preview and both export profiles. */
internal class SpaceCompassCaptureCaption(val height: Int, private val inset: Float,
    private val textTop: Float, private val layout: StaticLayout, val bounds: RectF) {
    fun draw(canvas: Canvas): RectF {
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), height.toFloat(), Paint().apply { color = 0x99000000.toInt() })
        canvas.save(); canvas.translate(inset, textTop); layout.draw(canvas); canvas.restore()
        return RectF(bounds)
    }
}

internal fun prepareSpaceCompassCaptureCaption(value: String, width: Int): SpaceCompassCaptureCaption {
    require(width > 0)
    val scale = width / 1080f
    val inset = 16 * scale
    val available = (width - inset * 2).toInt().coerceAtLeast(1)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 16 * scale
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(scale, 0f, scale * .5f, Color.BLACK)
    }
    val desired = Layout.getDesiredWidth(value, paint)
    if (desired > available - 1) paint.textSize *= (available - 1).coerceAtLeast(1) / desired
    val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, available)
        .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .setIncludePad(false).setMaxLines(1).build()
    val ink = Rect()
    paint.getTextBounds(value, 0, value.length, ink)
    if (ink.isEmpty) { ink.top = paint.fontMetrics.ascent.toInt(); ink.bottom = ceil(paint.fontMetrics.descent).toInt() }
    val padding = 8 * scale
    val height = ceil(ink.height() + padding * 2).toInt().coerceAtLeast(1)
    val baseline = layout.getLineBaseline(0)
    // Centre the visible glyphs, rather than the font's unused ascent/descent space.
    val inkTop = (height - ink.height()) / 2f
    val textTop = inkTop - baseline - ink.top
    val inkWidth = Layout.getDesiredWidth(value, paint).coerceAtMost(available.toFloat())
    return SpaceCompassCaptureCaption(height, inset, textTop, layout,
        RectF((width - inkWidth) / 2, inkTop, (width + inkWidth) / 2, inkTop + ink.height()))
}

/** Separate top azimuth gutter; preserve the original 44 px extent of a zenith marker. */
internal const val SPACE_COMPASS_PANORAMA_TOP_AXIS_HEIGHT = 80f
internal fun spaceCompassPanoramaSceneTop(caption: SpaceCompassCaptureCaption, width: Int): Float =
    caption.height + SPACE_COMPASS_PANORAMA_TOP_AXIS_HEIGHT * (width / 4096f)
