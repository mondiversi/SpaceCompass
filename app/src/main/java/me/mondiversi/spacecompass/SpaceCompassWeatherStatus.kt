package me.mondiversi.spacecompass

/** Cached usable weather remains readable during refresh; missing data reflects the request state. */
internal fun spaceCompassWeatherValueResource(current: SpaceCompassSunWeatherSnapshot?, loading: Boolean): Int =
    if (current == null) {
        if (loading) R.string.pc_loading else R.string.sun_weather_unavailable
    } else when (current.kind) {
        SpaceCompassSunWeatherKind.CLEAR -> R.string.sun_weather_clear
        SpaceCompassSunWeatherKind.MAINLY_CLEAR -> R.string.celestial_weather_mainly_clear
        SpaceCompassSunWeatherKind.PARTLY_CLOUDY -> R.string.sun_weather_partial
        SpaceCompassSunWeatherKind.CLOUDY -> R.string.sun_weather_cloudy
        SpaceCompassSunWeatherKind.FOG -> R.string.sun_weather_fog
        SpaceCompassSunWeatherKind.DRIZZLE -> R.string.sun_weather_drizzle
        SpaceCompassSunWeatherKind.RAIN -> R.string.sun_weather_rain
        SpaceCompassSunWeatherKind.SNOW -> R.string.sun_weather_snow
        SpaceCompassSunWeatherKind.STORM -> R.string.sun_weather_storm
    }

/** Weather retries are relevant only to the rendered weather and follow location/orbit errors. */
internal fun spaceCompassWeatherFailureVisible(reading: SpaceCompassSunWeatherReading,
    showWeather: Boolean, cameraEnabled: Boolean, hasHigherPriorityStatus: Boolean): Boolean =
    showWeather && !cameraEnabled && !hasHigherPriorityStatus && reading.failed && !reading.loading && reading.snapshot == null
