package me.mondiversi.spacecompass

/** Center within the real list extent; the first and last rows never need artificial blank space. */
internal fun spaceCompassDailyPathCenterScroll(pointCenterPx: Int, viewportHeightPx: Int, maximumScrollPx: Int): Int =
    (pointCenterPx - viewportHeightPx.coerceAtLeast(0) / 2).coerceIn(0, maximumScrollPx.coerceAtLeast(0))
