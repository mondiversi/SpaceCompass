package me.mondiversi.spacecompass

import android.content.res.Resources
import java.util.TimeZone

internal data class SpaceCompassPanoramaPresentation(val snapshot: SpaceCompassPanoramaSnapshot,
    val caption: SpaceCompassPanoramaCaptionData, val cameraWarning: String)

/** Build both presentations at capture time. Export never queries current GPS, weather or the clock. */
internal fun spaceCompassPanoramaPresentation(snapshot: SpaceCompassPanoramaSnapshot, resources: Resources,
    formatting: SpaceCompassPanoramaFormatting, zone: TimeZone, place: SpaceCompassPlaceParts?,
    altitude: Double?, accuracy: Double?, simulated: Boolean, simulatedAltitude: Boolean,
    cameraPointing: SpaceCompassCameraPhotoPointing? = null,
    cameraFieldOfView: SpaceCompassCameraPhotoFieldOfView? = null): SpaceCompassPanoramaPresentation {
    val timestamp = formatSpaceCompassPanoramaExportTimestamp(snapshot.timeMs, zone, formatting).let {
        if (simulated) it.replaceFirst("Space Compass", "Space Compass · ${resources.getString(R.string.observer_title)}") else it
    }
    val weather = resources.getString(when (snapshot.weather?.kind) {
        null -> R.string.sun_weather_unavailable
        SpaceCompassSunWeatherKind.CLEAR -> R.string.sun_weather_clear
        SpaceCompassSunWeatherKind.MAINLY_CLEAR -> R.string.celestial_weather_mainly_clear
        SpaceCompassSunWeatherKind.PARTLY_CLOUDY -> R.string.sun_weather_partial
        SpaceCompassSunWeatherKind.CLOUDY -> R.string.sun_weather_cloudy
        SpaceCompassSunWeatherKind.FOG -> R.string.sun_weather_fog
        SpaceCompassSunWeatherKind.DRIZZLE -> R.string.sun_weather_drizzle
        SpaceCompassSunWeatherKind.RAIN -> R.string.sun_weather_rain
        SpaceCompassSunWeatherKind.SNOW -> R.string.sun_weather_snow
        SpaceCompassSunWeatherKind.STORM -> R.string.sun_weather_storm
    })
    val rows = spaceCompassPanoramaExportRows(snapshot.latitude.takeIf { snapshot.observerPositionKnown },
        snapshot.longitude.takeIf { snapshot.observerPositionKnown }, altitude, accuracy,
        resources.getString(if (simulatedAltitude) R.string.observer_altitude_value else R.string.sun_finder_altitude,
            SPACE_COMPASS_SUN_DATA_MARKER), weather, formatting)
    val caption = SpaceCompassPanoramaCaptionData(timestamp,
        place?.let(::formatSpaceCompassEstimatedPlace) ?: resources.getString(R.string.environment_place_unavailable),
        formatSpaceCompassPanoramaArea(place), rows, formatSpaceCompassPanoramaExportCloudCover(snapshot.weather?.cloudCover, formatting),
        cameraPointing?.let { formatSpaceCompassCameraPhotoPointing(it,
            resources.getString(R.string.sun_finder_heading, SPACE_COMPASS_SUN_DATA_MARKER),
            resources.getString(R.string.sun_finder_tilt, SPACE_COMPASS_SUN_DATA_MARKER), formatting) },
        cameraFieldOfView?.let { formatSpaceCompassCameraPhotoFieldOfView(it,
            resources.getString(R.string.camera_photo_field_of_view), formatting) })
    fun angle(value: Double) = formatSpaceCompassNumber(value, 0, formatting.numeric,
        grouping = false, systemLocale = formatting.deviceLocale) + "°"
    val objects = snapshot.objects.map { it.copy(name = resources.getString(it.body.nameResource)) }
    val names = mapOf(SpaceCompassSkyReference.EQUATOR to R.string.sky_reference_equator,
        SpaceCompassSkyReference.CANCER to R.string.sky_reference_cancer,
        SpaceCompassSkyReference.CAPRICORN to R.string.sky_reference_capricorn,
        SpaceCompassSkyReference.ARCTIC to R.string.sky_reference_arctic,
        SpaceCompassSkyReference.ANTARCTIC to R.string.sky_reference_antarctic).mapValues { resources.getString(it.value) }
    return SpaceCompassPanoramaPresentation(snapshot.copy(objects = objects, formatting = formatting,
        cardinalNames = listOf(R.string.panorama_north, R.string.panorama_northeast, R.string.panorama_east,
            R.string.panorama_southeast, R.string.panorama_south, R.string.panorama_southwest,
            R.string.panorama_west, R.string.panorama_northwest).map(resources::getString),
        elevationNames = listOf(90.0, 60.0, 30.0, 0.0, -30.0, -60.0, -90.0).map { "${if (it > 0) "+" else ""}${angle(it)}" },
        azimuthNames = (0..12).map { angle(it * 30.0) },
        markerTimes = objects.flatMap { it.path?.markers.orEmpty() }.associate { point ->
            point.timeMs to formatSpaceCompassPanoramaExportTime(point.timeMs, zone, formatting) },
        referenceNames = names, poleNames = resources.getString(R.string.sky_reference_north_pole) to
            resources.getString(R.string.sky_reference_south_pole),
        observerPointNames = resources.getString(R.string.celestial_earth_center) to
            resources.getString(R.string.sky_reference_zenith)), caption, resources.getString(R.string.pc_compass_approximate))
}
