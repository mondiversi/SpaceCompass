package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
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
    return prepareSpaceCompassCaptureCaption(value, width, spaceCompassCaptureBandStyle(width, height), context)
}

internal fun prepareSpaceCompassCaptureCaption(value: String, width: Int,
    style: SpaceCompassCaptureBandStyle = spaceCompassCaptureBandStyle(width), context: Context? = null): SpaceCompassCaptureCaption {
    require(width > 0)
    val scale = style.scale
    val textSize = style.textSize
    val inset = style.horizontalInset
    val logo = context?.let { androidx.core.content.ContextCompat.getDrawable(it, R.drawable.ic_space_compass_button)?.mutate() }
    val logoSide = if (logo != null) maxOf(20 * scale, textSize * 1.25f) else 0f
    val logoGap = if (logo != null) 8 * scale else 0f
    val available = (width - inset * 2 - logoSide - logoGap).toInt().coerceAtLeast(1)
    val paint = style.paint()
    val textWidth = if (logo != null) ceil(Layout.getDesiredWidth(value, paint)).toInt().coerceIn(1, available) else available
    val layout = StaticLayout.Builder.obtain(value, 0, value.length, paint, textWidth)
        .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .setIncludePad(false).build()
    val inkBounds = spaceCompassCaptureTextInk(value, layout)
    val padding = style.verticalInset
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
