package me.mondiversi.spacecompass

/** The visible pointing area starts below the toolbar; drawing still extends beneath it. */
internal fun spaceCompassSceneOpticalFrame(frame: SpaceCompassSunSceneFrame, topInset: Double): SpaceCompassSunSceneFrame {
    if (!topInset.isFinite() || topInset <= 0 || !frame.height.isFinite() || frame.height <= 1) return frame
    val inset = topInset.coerceAtMost(frame.height - 1)
    return frame.copy(top = frame.top + inset, height = frame.height - inset)
}

/** Express the inset optical frame in the full drawing canvas, including real camera focal lengths. */
internal fun spaceCompassSceneDrawingPerspective(width: Double, height: Double, topInset: Double,
    perspective: SpaceCompassPerspective?): SpaceCompassPerspective? {
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0) return perspective
    val optical = spaceCompassSceneOpticalFrame(SpaceCompassSunSceneFrame(0.0, 0.0, width, height), topInset)
    if (optical.top == 0.0) return perspective
    return spaceCompassSceneFramePerspective(width, height, optical, perspective)
}

/** Extend the same rays onto a larger canvas without changing the visible optical axis or zoom. */
internal fun spaceCompassSceneFramePerspective(width: Double, height: Double, optical: SpaceCompassSunSceneFrame,
    perspective: SpaceCompassPerspective?): SpaceCompassPerspective? {
    if (!width.isFinite() || !height.isFinite() || width <= 0 || height <= 0 ||
        !optical.left.isFinite() || !optical.top.isFinite() || !optical.width.isFinite() || !optical.height.isFinite() ||
        optical.width <= 0 || optical.height <= 0) return perspective
    val focal = spaceCompassSunProjectionFocalLength(optical.height)
    val source = perspective ?: SpaceCompassPerspective(focal / optical.width, focal / optical.height)
    return SpaceCompassPerspective(source.horizontal * optical.width / width, source.vertical * optical.height / height,
        (optical.left + source.principalX * optical.width) / width,
        (optical.top + source.principalY * optical.height) / height)
}
