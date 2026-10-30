package me.mondiversi.spacecompass

import android.graphics.RectF
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser

internal const val SPACE_COMPASS_FLOATING_GLYPH_FILL = .53f
internal val LocalSpaceCompassFloatingStrokeScale = staticCompositionLocalOf { 1f }

/** Ink bounds are in the original glyph's dp coordinates, including its stroke. */
internal data class SpaceCompassFloatingInk(val side: Float, val bounds: Rect, val edgeWidth: Float = 2.21f)

internal data class SpaceCompassFloatingGlyphTransform(val scale: Float, val translation: Offset)

/** Optical offsets are in final glyph dp, after fitting and independently of stroke compensation. */
internal fun spaceCompassFloatingGlyphTransform(ink: SpaceCompassFloatingInk, circleSide: Float,
    opticalOffset: Offset = Offset.Zero): SpaceCompassFloatingGlyphTransform {
    val bounds = ink.bounds
    val scale = (circleSide * SPACE_COMPASS_FLOATING_GLYPH_FILL - ink.edgeWidth).coerceAtLeast(.01f) /
        (maxOf(bounds.width, bounds.height) - ink.edgeWidth).coerceAtLeast(.01f)
    return SpaceCompassFloatingGlyphTransform(scale,
        Offset((ink.side / 2 - bounds.center.x) * scale + opticalOffset.x,
            (ink.side / 2 - bounds.center.y) * scale + opticalOffset.y))
}

/** Normalize only the visible ink; layout, touch bounds, counters and circle geometry stay intact. */
@Composable
internal fun SpaceCompassFloatingGlyph(ink: SpaceCompassFloatingInk, circleSide: Dp = 48.dp,
    opticalOffset: Offset = Offset.Zero, content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    val transform = spaceCompassFloatingGlyphTransform(ink, circleSide.value, opticalOffset)
    val scale = transform.scale
    CompositionLocalProvider(LocalSpaceCompassFloatingStrokeScale provides (1f / scale)) {
        Box(Modifier.graphicsLayer {
            scaleX = scale; scaleY = scale
            translationX = transform.translation.x * density
            translationY = transform.translation.y * density
        }) { content() }
    }
}


internal fun spaceCompassFloatingInk(side: Float, left: Float, top: Float, right: Float, bottom: Float,
    stroke: Float = 2.21f): SpaceCompassFloatingInk = SpaceCompassFloatingInk(side,
    Rect(left * side - stroke / 2, top * side - stroke / 2,
        right * side + stroke / 2, bottom * side + stroke / 2), stroke)

/** Track the existing chevron rotation without changing the collapse animation or touch target. */
internal fun spaceCompassChevronFloatingInk(angle: Float): SpaceCompassFloatingInk {
    val radians = Math.toRadians(angle.toDouble())
    val c = kotlin.math.cos(radians).toFloat(); val s = kotlin.math.sin(radians).toFloat()
    val points = listOf(Offset(-7.02f, -3.64f), Offset(0f, 3.64f), Offset(7.02f, -3.64f)).map {
        Offset(13 + it.x * c - it.y * s, 13 + it.x * s + it.y * c)
    }
    return SpaceCompassFloatingInk(26f, Rect(points.minOf { it.x } - 1.105f, points.minOf { it.y } - 1.105f,
        points.maxOf { it.x } + 1.105f, points.maxOf { it.y } + 1.105f))
}

/** Apply optical balance after fitting, using the open triangle's centroid as a symmetric tipward guide. */
internal fun spaceCompassChevronOpticalOffset(angle: Float): Offset {
    val scale = (48f * SPACE_COMPASS_FLOATING_GLYPH_FILL - 2.21f) / 14.04f
    val distance = 3.64f * scale / 3f
    val radians = Math.toRadians(angle.toDouble())
    return Offset(-kotlin.math.sin(radians).toFloat() * distance,
        kotlin.math.cos(radians).toFloat() * distance)
}

/** Resolve compiled drawable XML through its asset cookie, preserving qualified resource selection. */
private fun spaceCompassOpenVectorXml(resources: Resources, @DrawableRes resource: Int): XmlResourceParser {
    val value = TypedValue()
    resources.getValue(resource, value, true)
    val path = requireNotNull(value.string).toString()
    require(path.endsWith(".xml")) { "Floating vector artwork must be XML" }
    return resources.assets.openXmlResourceParser(value.assetCookie, path)
}

/** These floating vector resources contain flat paths; use their actual paths rather than the padded viewport. */
@Composable
internal fun spaceCompassFloatingVectorInk(@DrawableRes resource: Int, side: Float = 26f): SpaceCompassFloatingInk {
    val resources = LocalResources.current
    return remember(resources, resource, side) {
        val xml = spaceCompassOpenVectorXml(resources, resource)
        val android = "http://schemas.android.com/apk/res/android"
        var width = 24f; var height = 24f; var maxStroke = 0f
        val all = RectF()
        try {
            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG) when (xml.name) {
                    "vector" -> {
                        width = xml.getAttributeValue(android, "viewportWidth").toFloat()
                        height = xml.getAttributeValue(android, "viewportHeight").toFloat()
                    }
                    "path" -> xml.getAttributeValue(android, "pathData")?.let { data ->
                        val path = requireNotNull(PathParser.createPathFromPathData(data))
                        val bounds = RectF()
                        @Suppress("DEPRECATION")
                        path.computeBounds(bounds, false)
                        val stroke = xml.getAttributeValue(android, "strokeWidth")?.toFloatOrNull() ?: 0f
                        maxStroke = maxOf(maxStroke, stroke)
                        bounds.inset(-stroke / 2, -stroke / 2)
                        all.union(bounds)
                    }
                }
                xml.next()
            }
        } finally { xml.close() }
        require(all.intersect(0f, 0f, width, height))
        SpaceCompassFloatingInk(side, Rect(all.left / width * side, all.top / height * side,
            all.right / width * side, all.bottom / height * side), maxStroke / width * side)
    }
}

/** Floating vectors keep their original stroke width while only their geometry grows. */
@Composable
internal fun SpaceCompassFloatingVectorIcon(@DrawableRes resource: Int, label: String? = null) {
    val resources = LocalResources.current
    val data = remember(resources, resource) {
        val xml = spaceCompassOpenVectorXml(resources, resource)
        val android = "http://schemas.android.com/apk/res/android"
        var width = 24f; var height = 24f
        val paths = mutableListOf<Pair<android.graphics.Path, Float>>()
        try {
            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG) when (xml.name) {
                    "vector" -> {
                        width = xml.getAttributeValue(android, "viewportWidth").toFloat()
                        height = xml.getAttributeValue(android, "viewportHeight").toFloat()
                    }
                    "path" -> xml.getAttributeValue(android, "pathData")?.let { value ->
                        paths += requireNotNull(PathParser.createPathFromPathData(value)) to
                            (xml.getAttributeValue(android, "strokeWidth")?.toFloatOrNull() ?: 0f)
                    }
                }
                xml.next()
            }
        } finally { xml.close() }
        Triple(width, height, paths.toList())
    }
    val strokeScale = LocalSpaceCompassFloatingStrokeScale.current
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND; strokeJoin = android.graphics.Paint.Join.ROUND
    } }
    SpaceCompassIconCanvas(LocalContentColor.current, Modifier.size(26.dp)
        .then(if (label == null) Modifier else Modifier.semantics { contentDescription = label })) { ink ->
        paint.color = ink.toArgb()
        withTransform({ scale(size.width / data.first, size.height / data.second, pivot = Offset.Zero) }) {
            for ((path, stroke) in data.third) {
                paint.strokeWidth = stroke * strokeScale
                drawContext.canvas.nativeCanvas.drawPath(path, paint)
            }
        }
    }
}
