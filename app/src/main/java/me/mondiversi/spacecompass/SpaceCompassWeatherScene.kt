package me.mondiversi.spacecompass

import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

internal data class SpaceCompassWeatherCloud(val x: Float, val y: Float, val radius: Float,
    val lobeSpacing: Float)
internal data class SpaceCompassWeatherParticle(val x: Float, val y: Float, val size: Float)
internal data class SpaceCompassWeatherScene(val width: Float, val height: Float, val unit: Float,
    val cover: Float, val kind: SpaceCompassSunWeatherKind?, val cloudAlpha: Float, val veilAlpha: Float,
    val fogAlpha: Float, val clouds: List<SpaceCompassWeatherCloud>,
    val precipitation: List<SpaceCompassWeatherParticle>)

/** Decorative geometry, not measured cloud locations. Density is per unit sky area in both views. */
internal fun spaceCompassWeatherScene(width: Float, height: Float,
    weather: SpaceCompassSunWeatherSnapshot?): SpaceCompassWeatherScene {
    require(width.isFinite() && height.isFinite() && width > 0 && height > 0)
    val unit = min(width, height)
    val cover = spaceCompassSunDisplayCloudCover(weather)
    val overcast = ((cover - .5f) / .5f).coerceIn(0f, 1f)
    val veil = .84f * overcast * overcast * (3 - 2 * overcast)
    val kind = weather?.kind
    val cloudAlpha = if (cover > 0) .12f + cover * .30f else 0f
    val clouds = buildList {
        if (cover >= .05f) {
            val cell = unit * .28f
            val columns = ceil(width / cell).toInt() + 2
            val rows = ceil(height / cell).toInt() + 2
            for (row in 0 until rows) for (column in 0 until columns) {
                // Stable ranks keep existing clouds in place as coverage grows.
                val index = row * 101 + column
                val rank = ((index + 1) * .618034f) % 1f
                if (rank <= cover) add(SpaceCompassWeatherCloud(
                    (column - .5f + ((index + 1) * .414214f) % 1f * .25f) * cell,
                    (row - .5f + ((index + 1) * .732051f) % 1f * .25f) * cell,
                    unit * (.024f + cover * .055f), unit * .045f))
            }
        }
    }
    val density = when (kind) {
        SpaceCompassSunWeatherKind.DRIZZLE -> 17f
        SpaceCompassSunWeatherKind.RAIN -> 42f
        SpaceCompassSunWeatherKind.STORM -> 64f
        SpaceCompassSunWeatherKind.SNOW -> 36f
        else -> 0f
    }
    val particles = List((density * width / unit * height / unit).roundToInt()) { index ->
        SpaceCompassWeatherParticle(width * ((index * .618034f) % 1f),
            height * ((index * .414214f) % 1f), unit * (.001f + index % 3 * .001f))
    }
    return SpaceCompassWeatherScene(width, height, unit, cover, kind, cloudAlpha, veil,
        if (kind == SpaceCompassSunWeatherKind.FOG) .48f else 0f, clouds, particles)
}
