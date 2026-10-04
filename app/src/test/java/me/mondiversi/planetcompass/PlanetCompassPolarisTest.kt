package me.mondiversi.planetcompass

import io.github.cosinekitty.astronomy.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PlanetCompassPolarisTest {
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    @Test fun pointingMatchesIndependentEngineStarPipeline() {
        // Independent Equator/Horizon API instead of the implementation's vector rotation.
        // SIMBAD ICRS/J2000 reference; tolerance covers the proper motion omitted here.
        defineStar(Body.Star1, 2 + 31.0/60 + 49.09456/3600, 89 + 15.0/60 + 50.7923/3600,
            PLANET_COMPASS_POLARIS_DISTANCE_KM / PLANET_COMPASS_LIGHT_YEAR_KM)
        for (latitude in listOf(-33.0,0.0,45.0,65.0)) for (hour in 0..23 step 3) {
            val ms = now + hour * 3_600_000L
            val time = planetCompassAstronomyTime(ms)
            val site = Observer(latitude,12.5,200.0)
            val eq = equator(Body.Star1,time,site,EquatorEpoch.OfDate,Aberration.Corrected)
            val reference = horizon(time,site,eq.ra,eq.dec,Refraction.None)
            val actual = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.POLARIS,ms,latitude,12.5,200.0)!!
            assertEquals(reference.altitude,actual.position.elevationDegrees,0.02)
            val difference = ((reference.azimuth-actual.position.azimuthDegrees+540)%360)-180
            assertEquals(0.0,difference,0.02)
        }
    }
    @Test fun apparentDailyCircleIsNearNorthAndObserverLatitudeNotAnEarthOrbit() {
        val path = calculatePlanetCompassCelestialPath(PlanetCompassCelestialBody.POLARIS,LocalDate.parse("2026-10-04"),
            ZoneId.of("Europe/Rome"),now,45.0,12.5,0.0,PlanetCompassCelestialRemoteData())!!
        assertEquals(24,path.markers.count { it.event == PlanetCompassSunPathEvent.HOUR })
        assertFalse(path.markers.any { it.event == PlanetCompassSunPathEvent.SUNRISE || it.event == PlanetCompassSunPathEvent.SUNSET })
        path.samples.forEach {
            assertTrue(it.position.elevationDegrees in 44.0..46.0)
            assertTrue(it.position.azimuthDegrees < 2 || it.position.azimuthDegrees > 358)
        }
        assertTrue(path.samples.maxOf { it.position.elevationDegrees } - path.samples.minOf { it.position.elevationDegrees } > 0.5)
    }
    @Test fun starIsBelowHorizonInSouthernHemisphereAndUtcIgnoresDst() {
        for (hour in 0..23) assertTrue(calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.POLARIS,
            now+hour*3_600_000L,-33.0,151.0)!!.position.elevationDegrees < -30)
        val original = java.util.TimeZone.getDefault()
        try {
            val reference = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.POLARIS,now,45.0,12.5)
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            assertEquals(reference,calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.POLARIS,now,45.0,12.5))
        } finally { java.util.TimeZone.setDefault(original) }
    }
    @Test fun meanSurfaceGravityAndBulkDensityAreDerivedFromMassAndRadius() {
        val facts = planetCompassCelestialFacts(PlanetCompassCelestialBody.POLARIS)
        val radiusMeters = facts.diameterKm!! * 500
        assertEquals(6.67430e-11 * facts.massKg!! / (radiusMeters * radiusMeters), facts.gravity!!, 1e-12)
        assertEquals(facts.massKg!! / (4.0 / 3 * Math.PI * radiusMeters * radiusMeters * radiusMeters), facts.density!!, 1e-12)
        assertTrue(facts.gravity!! in 0.6..0.7)
        assertTrue(facts.density!! in 0.07..0.08)
    }
    @Test fun referenceFactsDoNotInventRotationOrSolarOrbitalVelocity() {
        assertEquals(446.5,PLANET_COMPASS_POLARIS_DISTANCE_KM/PLANET_COMPASS_LIGHT_YEAR_KM,0.1)
        val facts = planetCompassCelestialFacts(PlanetCompassCelestialBody.POLARIS)
        assertEquals(64_380_078.0,facts.diameterKm!!,0.001)
        assertNull(facts.rotationHours); assertNull(facts.parent); assertNull(facts.revolutionDays)
        assertNull(calculatePlanetCompassCelestialSpeed(PlanetCompassCelestialBody.POLARIS,now))
        assertFalse(PlanetCompassCelestialBody.POLARIS.usesHorizons)
        assertTrue(PlanetCompassCelestialBody.POLARIS.supportsDailyPath)
        assertNull(PlanetCompassCelestialBody.POLARIS.viewerTexture)
    }
}
