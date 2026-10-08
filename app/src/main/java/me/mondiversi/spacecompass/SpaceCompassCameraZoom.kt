package me.mondiversi.spacecompass

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

internal const val SPACE_COMPASS_CAMERA_ZOOM_KEY = "camera_zoom_ratio"
internal const val SPACE_COMPASS_CAMERA_ZOOM_DEFAULT = 1f

/** Ratios use the ordinary rear lens as 1x, including separately exposed ultrawide lenses. */
internal data class SpaceCompassCameraZoomOption(
    val id: String, val base: Float, val minimum: Float, val maximum: Float
) {
    init { require(base.isFinite() && base > 0 && minimum.isFinite() && maximum.isFinite() &&
        minimum > 0 && minimum <= 1 && maximum >= 1) }
    val lower get() = base * minimum
    val upper get() = base * maximum
}

internal data class SpaceCompassCameraZoomRange(val minimum: Float, val maximum: Float) {
    init { require(minimum.isFinite() && maximum.isFinite() && minimum > 0f && maximum >= minimum) }

    private val regularRatios by lazy {
        val values = mutableListOf<Double>()
        var fraction = 10.0.pow(floor(log10(minOf(minimum.toDouble(), .1))))
        while (fraction < 1.0) {
            for (i in 1..9) values += i * fraction
            fraction *= 10.0
        }
        values += listOf(1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 3.5)
        values += (4..9).map(Int::toDouble)
        var decade = 10.0
        do {
            for (multiple in listOf(1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 6.0, 7.0, 8.0, 9.0))
                values += multiple * decade
            decade *= 10.0
        } while (decade <= maximum.toDouble())
        values.map(Double::toFloat).filter { it.isFinite() && it > 0f }.distinct().sorted()
    }

    private fun endpointLabel(value: Float): Float {
        val nearest = regularRatios.minBy { abs(it.toDouble() - value) }
        // Nominal 0.5x/8x names hide tiny optical/driver rounding, without changing physical limits.
        // Preserve a separate ordinary 1x setting when an endpoint merely approaches it.
        return if ((nearest != 1f || value == 1f) && abs(nearest.toDouble() - value) / value <= .025) nearest else value
    }
    private val endpointLabels by lazy {
        val lower = endpointLabel(minimum)
        val upper = endpointLabel(maximum)
        if (minimum != maximum && lower == upper) mapOf(minimum to minimum, maximum to maximum)
        else mapOf(minimum to lower, maximum to upper)
    }
    private val levels by lazy {
        (listOf(minimum, maximum) + regularRatios.filter {
            it in minimum..maximum && it !in endpointLabels.values
        }).distinct().sorted()
    }

    /** No arbitrary gesture/current ratio is ever inserted into the menu. */
    fun presets(): List<Float> = levels

    private fun bounded(current: Float) = current.takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(minimum, maximum) ?: 1f.coerceIn(minimum, maximum)

    /** Optional small detent hysteresis stops finger jitter repeatedly switching the ordinary lens. */
    fun snap(current: Float, previous: Float? = null): Float {
        val ratio = bounded(current)
        val nearest = levels.minBy { abs(it.toDouble() - ratio) }
        val last = previous?.takeIf { it in levels } ?: return nearest
        if (last == nearest) return nearest
        val midpoint = (last.toDouble() + nearest) / 2
        val margin = abs(last.toDouble() - nearest) * .08
        return if (nearest > last && ratio < midpoint + margin || nearest < last && ratio > midpoint - margin)
            last else nearest
    }

    /** Display one ladder value while retaining unrounded capture-result zoom for optical geometry. */
    fun displayRatio(current: Float): Float = snap(current).let { endpointLabels[it] ?: it }

    /** Accumulate continuous finger movement, separately from the emitted stepped camera request. */
    fun pinch(current: Float, factor: Float): Float {
        val ratio = bounded(current)
        if (!factor.isFinite() || factor <= 0f) return ratio
        return (ratio.toDouble() * factor.toDouble()).coerceIn(minimum.toDouble(), maximum.toDouble()).toFloat()
    }

    fun step(current: Float, inward: Boolean): Float {
        val ratio = bounded(current)
        return if (inward) levels.firstOrNull { it > ratio } ?: maximum
        else levels.lastOrNull { it < ratio } ?: minimum
    }
}

internal fun spaceCompassCameraZoomDigits(ratio: Float): Int = (0..4).firstOrNull { digits ->
    val scale = 10.0.pow(digits)
    abs(ratio.toDouble() - round(ratio.toDouble() * scale) / scale) < 1e-6
} ?: 4

internal fun spaceCompassCameraZoomOption(options: List<SpaceCompassCameraZoomOption>, zoom: Float): SpaceCompassCameraZoomOption {
    require(options.isNotEmpty())
    val ratio = zoom.takeIf { it.isFinite() && it > 0 } ?: 1f
    return options.filter { ratio >= it.lower - .0001f && ratio <= it.upper + .0001f }
        .maxByOrNull { if (it.base <= ratio + .0001f) it.base else -it.base }
        ?: options.minBy { abs(ratio - ratio.coerceIn(it.lower, it.upper)) }
}

/** Centered legacy sensor crop, bounded even at the hardware limit and for offset arrays. */
internal fun spaceCompassCameraZoomCrop(left: Int, top: Int, width: Int, height: Int, zoom: Float): IntArray {
    require(width > 0 && height > 0 && zoom.isFinite() && zoom >= 1)
    val w = (width / zoom).roundToInt().coerceIn(1, width)
    val h = (height / zoom).roundToInt().coerceIn(1, height)
    val x = left + (width - w) / 2
    val y = top + (height - h) / 2
    return intArrayOf(x, y, x + w, y + h)
}

/** Camera2 ratio metadata uses a post-zoom virtual array; transform calibration into that array. */
internal fun SpaceCompassCameraLens.withZoomRatio(zoom: Float, arrayLeft: Double, arrayTop: Double,
    arrayWidth: Double, arrayHeight: Double): SpaceCompassCameraLens {
    require(zoom.isFinite() && zoom > 0)
    val cx = arrayLeft + arrayWidth / 2
    val cy = arrayTop + arrayHeight / 2
    return copy(focalX = focalX * zoom, focalY = focalY * zoom,
        centerX = cx + (centerX - cx) * zoom, centerY = cy + (centerY - cy) * zoom)
}
