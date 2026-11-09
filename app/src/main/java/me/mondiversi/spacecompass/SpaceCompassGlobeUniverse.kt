package me.mondiversi.spacecompass

/** Shared decorative space field. Stars are normalized to the viewport, not celestial positions. */
internal object SpaceCompassGlobeUniverse {
    data class Star(val x: Float, val y: Float, val radius: Float)
    val stars: List<Star> = java.util.Random(38).let { rng ->
        List(180) { Star(rng.nextFloat(), rng.nextFloat(), .35f + rng.nextFloat()) }
    }
    val stops = floatArrayOf(0f, .45f, 1f)
    const val starColor: Int = 0x59abc4dd
    fun palette(dark: Boolean): IntArray = if (dark)
        intArrayOf(0xff172037.toInt(), 0xff080e1c.toInt(), 0xff02050c.toInt())
    else intArrayOf(0xff30465f.toInt(), 0xff1b2d45.toInt(), 0xff0e1b2d.toInt())
}
