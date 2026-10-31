package me.mondiversi.spacecompass

/** Keep the requested offset through temporary content/viewport shrinkage. Explicit scrolling
 * replaces it; layout clamping must not erase it before asynchronous annual details return. */
internal class SpaceCompassObjectPageScrollPosition {
    var offset: Int = 0
        private set
    private var lastValue = 0
    private var lastMaximum = Int.MAX_VALUE
    private var expectedRestore: Int? = null

    fun request(value: Int) { offset = value.coerceAtLeast(0); expectedRestore = null }

    fun observe(value: Int, maximum: Int, scrolling: Boolean): Int? {
        val changed = value != lastValue
        val clampedByLayout = maximum != lastMaximum && value == lastValue.coerceAtMost(maximum)
        if (expectedRestore == value) expectedRestore = null
        else if (changed && (scrolling || !clampedByLayout)) {
            offset = value.coerceAtLeast(0)
            expectedRestore = null
        }
        lastValue = value; lastMaximum = maximum
        val target = offset.coerceIn(0, maximum.coerceAtLeast(0))
        return target.takeIf { !scrolling && value != target }?.also { expectedRestore = it }
    }
}
