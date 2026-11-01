package me.mondiversi.spacecompass

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Conservative glyph ribbons along the shaped baseline, including diacritics and its outline. */
internal fun spaceCompassCurveTextClearance(baseline: List<SpaceCompassSunScenePoint>,
    textSize: Double): List<SpaceCompassSunSceneFrame> = buildList {
    if (!textSize.isFinite() || textSize <= 0) return@buildList
    baseline.zipWithNext().forEach { (a, b) ->
        val length = hypot(b.x-a.x, b.y-a.y)
        if (length < 1e-5) return@forEach
        val nx = (b.y-a.y)/length; val ny = -(b.x-a.x)/length
        val count = ceil(length / (textSize * .5)).toInt().coerceAtLeast(1)
        for (index in 0 until count) {
            val start = index.toDouble()/count; val end = (index+1).toDouble()/count
            val x1 = a.x+(b.x-a.x)*start; val y1 = a.y+(b.y-a.y)*start
            val x2 = a.x+(b.x-a.x)*end; val y2 = a.y+(b.y-a.y)*end
            val up = textSize * 1.35; val down = -textSize * .5
            val left = min(min(x1+nx*up, x2+nx*up), min(x1+nx*down, x2+nx*down))
            val top = min(min(y1+ny*up, y2+ny*up), min(y1+ny*down, y2+ny*down))
            val right = max(max(x1+nx*up, x2+nx*up), max(x1+nx*down, x2+nx*down))
            val bottom = max(max(y1+ny*up, y2+ny*up), max(y1+ny*down, y2+ny*down))
            add(SpaceCompassSunSceneFrame(left, top, right-left, bottom-top))
        }
    }
}
