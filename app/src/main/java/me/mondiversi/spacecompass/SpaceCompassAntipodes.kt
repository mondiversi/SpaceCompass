package me.mondiversi.spacecompass

/** An opposite viewing direction, never a second measured position of the source object. */
internal fun spaceCompassAntipode(position: SpaceCompassSunPosition): SpaceCompassSunPosition {
    require(position.azimuthDegrees.isFinite() && position.elevationDegrees.isFinite() &&
        position.elevationDegrees in -90.0..90.0)
    val azimuth = ((position.azimuthDegrees % 360.0 + 360.0) % 360.0 + 180.0) % 360.0
    return SpaceCompassSunPosition(azimuth, -position.elevationDegrees)
}

/** Preserve the source instant and event identity separately from the mirrored direction. */
internal fun SpaceCompassSunPathPoint.antipodalDirection() = copy(position = spaceCompassAntipode(position))
