package me.mondiversi.spacecompass

/** Weather retries are relevant only to the rendered weather and follow location/orbit errors. */
internal fun spaceCompassWeatherFailureVisible(reading: SpaceCompassSunWeatherReading,
    showWeather: Boolean, cameraEnabled: Boolean, hasHigherPriorityStatus: Boolean): Boolean =
    showWeather && !cameraEnabled && !hasHigherPriorityStatus && reading.failed && !reading.loading && reading.snapshot == null
