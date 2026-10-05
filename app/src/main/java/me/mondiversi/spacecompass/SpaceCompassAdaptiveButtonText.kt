package me.mondiversi.spacecompass

/** Resolve compact action captions before the first frame, preserving accessibility fonts. */
internal fun fitSpaceCompassButtonFontSize(maximum: Float, minimum: Float, fits: (Float) -> Boolean): Float {
    var candidate = maximum.coerceAtLeast(minimum)
    while (candidate > minimum && !fits(candidate)) {
        candidate = (candidate - 0.5f).coerceAtLeast(minimum)
    }
    return candidate
}
