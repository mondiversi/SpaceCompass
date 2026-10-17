package me.mondiversi.spacecompass

import kotlin.math.cos
import kotlin.math.sin

/** Shared filled semicircle: rise curves above its diameter, set below it.
 * Bounds are optically centred in the marker; Android and Compose use identical geometry. */
internal fun spaceCompassHorizonEventGlyph(event: SpaceCompassSunPathEvent, x: Float, y: Float,
    radius: Float): List<Pair<Float, Float>> {
    require(event == SpaceCompassSunPathEvent.SUNRISE || event == SpaceCompassSunPathEvent.SUNSET)
    val direction = if (event == SpaceCompassSunPathEvent.SUNRISE) -1f else 1f
    return (0..24).map { step ->
        val angle = Math.PI * step / 24
        (x + radius * cos(angle).toFloat()) to
            (y + direction * radius * (sin(angle).toFloat() - .5f))
    }
}
