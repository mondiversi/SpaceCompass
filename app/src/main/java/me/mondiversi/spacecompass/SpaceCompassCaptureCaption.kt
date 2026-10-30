package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import kotlin.math.ceil

/** A dedicated opaque information band, shared by previews and both export profiles. */
internal class SpaceCompassCaptureCaption(val height: Int, private val inset: Float,
    private val textTop: Float, private val layout: StaticLayout, val bounds: RectF,
    private val logo: Drawable? = null, private val logoBounds: RectF? = null) {
    fun draw(canvas: Canvas): RectF {
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), height.toFloat(), Paint().apply { color = Color.BLACK })
        logoBounds?.let { box ->
            logo?.let { drawable ->
                canvas.save(); canvas.translate(box.left, box.top)
                drawable.setBounds(0, 0, ceil(box.width()).toInt(), ceil(box.height()).toInt())
                drawable.draw(canvas); canvas.restore()
            }
        }
        canvas.save(); canvas.translate(inset, textTop); layout.draw(canvas); canvas.restore()
        return RectF(bounds)
    }
}

/** Photographs keep a legible font size; extra camera metadata wraps instead of shrinking. */
internal fun prepareSpaceCompassCameraCaptureCaption(value: String, width: Int, height: Int,
    context: Context? = null): SpaceCompassCaptureCaption {
    require(width > 0 && height > 0)
    return prepareSpaceCompassCaptureCaption(value, width, 26 * (minOf(width, height) / 1080f).coerceAtLeast(.45f),
        wrap = true, context = context)
}

internal fun prepareSpaceCompassCaptureCaption(value: String, width: Int,
    textSize: Float = 16 * (width / 1080f), wrap: Boolean = false, context: Context? = null): SpaceCompassCaptureCaption {
    require(width > 0 && textSize.isFinite() && textSize > 0)
    val scale = width / 1080f
    val inset = 16 * scale
    val logo = context?.let { androidx.core.content.ContextCompat.getDrawable(it, R.drawable.ic_space_compass_button)?.mutate() }
    val logoSide = if (logo != null) maxOf(20 * scale, textSize * 1.25f) else 0f
    val logoGap = if (logo != null) 8 * scale else 0f
    val available = (width - inset * 2 - logoSide - logoGap).toInt().coerceAtLeast(1)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(scale, 0f, scale * .5f, Color.BLACK)
    }
    val desired = Layout.getDesiredWidth(value, paint)
    if (!wrap && desired > available - 1) paint.textSize *= (available - 1).coerceAtLeast(1) / desired
    val textWidth = if (logo != null) ceil(Layout.getDesiredWidth(value, paint)).toInt().coerceIn(1, available) else available
    val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, textWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .setIncludePad(false).setMaxLines(if (wrap) Int.MAX_VALUE else 1).build()
    val ink = Rect()
    val inkBounds = RectF()
    for (line in 0 until layout.lineCount) {
        paint.getTextBounds(value, layout.getLineStart(line), layout.getLineVisibleEnd(line), ink)
        if (ink.isEmpty) { ink.top = paint.fontMetrics.ascent.toInt(); ink.bottom = ceil(paint.fontMetrics.descent).toInt() }
        val baseline = layout.getLineBaseline(line)
        inkBounds.union(RectF(layout.getLineLeft(line), (baseline + ink.top).toFloat(),
            layout.getLineRight(line), (baseline + ink.bottom).toFloat()))
    }
    val padding = 8 * scale
    val height = ceil(maxOf(inkBounds.height(), logoSide) + padding * 2).toInt().coerceAtLeast(1)
    val textTop = (height - inkBounds.height()) / 2f - inkBounds.top
    val groupWidth = textWidth + logoSide + logoGap
    val groupLeft = if (logo != null) (width - groupWidth) / 2f else inset
    val textLeft = groupLeft + logoSide + logoGap
    val textBounds = RectF(inkBounds).apply { offset(textLeft, textTop) }
    val logoBounds = if (logo != null) RectF(groupLeft, (height - logoSide) / 2,
        groupLeft + logoSide, (height + logoSide) / 2) else null
    val bounds = RectF(textBounds).apply { logoBounds?.let { union(it) } }
    return SpaceCompassCaptureCaption(height, textLeft, textTop, layout, bounds, logo, logoBounds)
}

/** Separate top azimuth gutter; preserve the original 44 px extent of a zenith marker. */
internal const val SPACE_COMPASS_PANORAMA_TOP_AXIS_HEIGHT = 80f
internal fun spaceCompassPanoramaSceneTop(caption: SpaceCompassCaptureCaption, width: Int): Float =
    caption.height + SPACE_COMPASS_PANORAMA_TOP_AXIS_HEIGHT * (width / 4096f)
