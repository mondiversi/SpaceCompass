package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*

/** Annual events for the inspected body only. Never propagate a short-lived TLE over a year. */
internal fun calculateSpaceCompassKeyEvents(calendar: SpaceCompassObservationCalendar,
    place: SpaceCompassObservationPlace, remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData(),
    checkCancelled: () -> Unit = {}): List<SpaceCompassKeyEvent>? {
    require(place.valid)
    val bounds = spaceCompassKeyEventYearBounds(calendar)
    val body = calendar.body
    if (body == SpaceCompassCelestialBody.SUN) return calculateSpaceCompassSolarKeyEvents(calendar, place, checkCancelled)
    if (body.isEarthSatellite) return null
    if (body.isFictional || body.engine == null && !body.usesHorizons) return emptyList()
    val result = mutableListOf<SpaceCompassKeyEvent>()
    fun add(kind: SpaceCompassKeyEventKind, time: Time, distance: Double? = null, angle: Double? = null) {
        checkCancelled()
        val instant = time.toMillisecondsSince1970()
        if (instant !in bounds) return
        val position = requireNotNull(calculateSpaceCompassCelestialObservation(body, instant,
            place.latitude, place.longitude, place.altitude, remote)).position
        result += SpaceCompassKeyEvent(kind, instant, position.elevationDegrees, distanceKm = distance, angle = angle,
            azimuth = position.azimuthDegrees)
    }
    val start = spaceCompassAstronomyTime(bounds.first)
    if (body == SpaceCompassCelestialBody.MOON) {
        val phases = listOf(SpaceCompassKeyEventKind.NEW_MOON, SpaceCompassKeyEventKind.FIRST_QUARTER,
            SpaceCompassKeyEventKind.FULL_MOON, SpaceCompassKeyEventKind.LAST_QUARTER)
        var phase = searchMoonQuarter(start)
        while (phase.time.toMillisecondsSince1970() <= bounds.last) {
            add(phases[phase.quarter], phase.time)
            phase = searchMoonQuarter(phase.time.addDays(1.0))
        }
        var apsis = searchLunarApsis(start)
        while (apsis.time.toMillisecondsSince1970() <= bounds.last) {
            add(if (apsis.kind == ApsisKind.Pericenter) SpaceCompassKeyEventKind.PERIGEE
                else SpaceCompassKeyEventKind.APOGEE, apsis.time, apsis.distanceKm)
            apsis = searchLunarApsis(apsis.time.addDays(1.0))
        }
    } else if (body.engine != null) {
        val engine = body.engine
        if (engine == Body.Mercury || engine == Body.Venus) {
            var elongation = searchMaxElongation(engine, start)
            while (elongation.time.toMillisecondsSince1970() <= bounds.last) {
                add(if (elongation.visibility == Visibility.Morning) SpaceCompassKeyEventKind.MORNING_ELONGATION
                    else SpaceCompassKeyEventKind.EVENING_ELONGATION, elongation.time, angle = elongation.elongation)
                elongation = searchMaxElongation(engine, elongation.time.addDays(1.0))
            }
        } else {
            // Heliocentric relative longitude 0: Earth and the outer planet on the same side of the Sun.
            var opposition = searchRelativeLongitude(engine, 0.0, start)
            while (opposition.toMillisecondsSince1970() <= bounds.last) {
                add(SpaceCompassKeyEventKind.OPPOSITION, opposition)
                opposition = searchRelativeLongitude(engine, 0.0, opposition.addDays(1.0))
            }
        }
        var apsis = searchPlanetApsis(engine, start)
        while (apsis.time.toMillisecondsSince1970() <= bounds.last) {
            add(if (apsis.kind == ApsisKind.Pericenter) SpaceCompassKeyEventKind.PERIHELION
                else SpaceCompassKeyEventKind.APHELION, apsis.time, apsis.distanceKm)
            apsis = searchPlanetApsis(engine, apsis.time.addDays(1.0))
        }
        val distances = spaceCompassEventCurve(bounds.first, bounds.last, 21_600_000L,
            { geoVector(engine, spaceCompassAstronomyTime(it), Aberration.Corrected).length() * SPACE_COMPASS_AU_KM },
            checkCancelled) ?: return null
        addAnnualDistances(distances, ::add)
    } else {
        val ephemeris = remote.ephemerides[body]?.takeIf { it.body == body } ?: return null
        // An annual extremum needs full-year coverage and a bounded cadence, without extrapolation.
        if (ephemeris.samples.first().timeMs > bounds.first || ephemeris.samples.last().timeMs < bounds.last ||
            ephemeris.samples.zipWithNext().any { (a,b) -> b.timeMs-a.timeMs > 86_400_000L }) return null
        val distances = spaceCompassEventCurve(bounds.first, bounds.last, 21_600_000L,
            { ephemeris.at(it)?.length()?.times(SPACE_COMPASS_AU_KM) }, checkCancelled) ?: return null
        addAnnualDistances(distances, ::add)
    }
    return result.sortedWith(compareBy<SpaceCompassKeyEvent> { it.timeMs }.thenBy { it.kind.ordinal })
}

private fun addAnnualDistances(curve: List<SpaceCompassEventScalar>,
    add: (SpaceCompassKeyEventKind, Time, Double?, Double?) -> Unit) {
    curve.minByOrNull { it.value }?.let {
        add(SpaceCompassKeyEventKind.NEAREST_EARTH, spaceCompassAstronomyTime(it.timeMs), it.value, null) }
    curve.maxByOrNull { it.value }?.let {
        add(SpaceCompassKeyEventKind.FARTHEST_EARTH, spaceCompassAstronomyTime(it.timeMs), it.value, null) }
}
