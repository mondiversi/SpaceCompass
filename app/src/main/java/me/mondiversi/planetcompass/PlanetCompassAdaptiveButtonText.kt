package me.mondiversi.planetcompass

/** Resolve compact action captions before the first frame, preserving accessibility fonts. */
internal fun fitPlanetCompassButtonFontSize(maximum: Float, minimum: Float, fits: (Float) -> Boolean): Float {
    var candidate = maximum.coerceAtLeast(minimum)
    while (candidate > minimum && !fits(candidate)) {
        candidate = (candidate - 0.5f).coerceAtLeast(minimum)
    }
    return candidate
}
