package me.mondiversi.spacecompass

/** Rear-camera axis relative to the local horizon, sampled at the JPEG exposure. */
internal data class SpaceCompassCameraPhotoPointing(val headingDegrees: Double?, val tiltDegrees: Double?)

internal fun spaceCompassCameraPhotoPointing(attitude: SpaceCompassCameraAttitude?): SpaceCompassCameraPhotoPointing {
    val orientation = attitude?.orientation
    val forward = orientation?.forward?.takeIf {
        listOf(it.east, it.north, it.up).all(Double::isFinite) && it.dot(it) > 1e-12
    } ?: return SpaceCompassCameraPhotoPointing(null, null)
    val pointing = orientation.copy(forward = forward.normalized())
    return SpaceCompassCameraPhotoPointing(
        spaceCompassSunPointingHeading(pointing.takeIf { attitude.usable }),
        pointing.tiltDegrees.takeIf(Double::isFinite))
}

/** Reuse existing translated labels and frozen numeric formats; never invent a missing bearing. */
internal fun formatSpaceCompassCameraPhotoPointing(pointing: SpaceCompassCameraPhotoPointing,
    headingTemplate: String, tiltTemplate: String, formatting: SpaceCompassPanoramaFormatting): String {
    fun angle(value: Double?, signed: Boolean): String = value?.takeIf(Double::isFinite)?.let {
        val degrees = formatSpaceCompassPanoramaElevation(it, formatting)
        (if (signed && it >= .05) "+" else "") + degrees + "°"
    } ?: "—"
    return headingTemplate.replace(SPACE_COMPASS_SUN_DATA_MARKER, angle(pointing.headingDegrees, false)) +
        " · " + tiltTemplate.replace(SPACE_COMPASS_SUN_DATA_MARKER, angle(pointing.tiltDegrees, true))
}


internal data class SpaceCompassCameraPhotoFieldOfView(val horizontalDegrees: Double, val verticalDegrees: Double)

/** Angular span between the JPEG edges, preserving the real optical centre and crop. */
internal fun spaceCompassCameraPhotoFieldOfView(perspective: SpaceCompassPerspective): SpaceCompassCameraPhotoFieldOfView {
    fun span(focal: Double, principal: Double) = Math.toDegrees(
        kotlin.math.atan2(1.0 - principal, focal) - kotlin.math.atan2(-principal, focal))
    return SpaceCompassCameraPhotoFieldOfView(span(perspective.horizontal, perspective.principalX),
        span(perspective.vertical, perspective.principalY))
}

/** Horizontal then vertical in the upright JPEG; independent of later preview pan/zoom. */
internal fun formatSpaceCompassCameraPhotoFieldOfView(field: SpaceCompassCameraPhotoFieldOfView,
    template: String, formatting: SpaceCompassPanoramaFormatting): String = template
    .replace("%1\$s", formatSpaceCompassPanoramaElevation(field.horizontalDegrees, formatting) + "°")
    .replace("%2\$s", formatSpaceCompassPanoramaElevation(field.verticalDegrees, formatting) + "°")
