package me.mondiversi.spacecompass

/** Session-only coordinates for the decorative globe; never starts or persists a GPS request. */
internal data class SpaceCompassScreensaverLocation(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }

    val javascript: String get() = "[$latitude,$longitude]"
}

/** Keep the last usable effective position while live work is suspended or awaiting another fix. */
internal fun spaceCompassScreensaverLocation(observer: SpaceCompassScreensaverSolarObserver?,
    previous: SpaceCompassScreensaverLocation?): SpaceCompassScreensaverLocation? {
    if (observer == null || !observer.latitude.isFinite() || observer.latitude !in -90.0..90.0 ||
        !observer.longitude.isFinite() || observer.longitude !in -180.0..180.0) return previous
    return SpaceCompassScreensaverLocation(observer.latitude, observer.longitude)
}
