package me.mondiversi.planetcompass

import io.github.cosinekitty.astronomy.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.*

class PlanetCompassJovianMoonsTest {
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private val moons = listOf(PlanetCompassCelestialBody.IO,PlanetCompassCelestialBody.EUROPA)
    @Test fun localStatesHaveRealJovicentricRadiusAndOrbitalSpeed() {
        for (body in moons) {
            val state=planetCompassJovianMoonState(body,planetCompassAstronomyTime(now))
            val radius=sqrt(state.x*state.x+state.y*state.y+state.z*state.z)*PLANET_COMPASS_AU_KM
            assertTrue(radius in if(body==PlanetCompassCelestialBody.IO) 410_000.0..435_000.0 else 650_000.0..690_000.0)
            assertTrue(calculatePlanetCompassCelestialSpeed(body,now)!! in
                if(body==PlanetCompassCelestialBody.IO) 16.0..18.5 else 12.5..14.5)
        }
    }
    @Test fun apparentVectorBackdatesMoonByJupiterLightTravelTime() {
        val time=planetCompassAstronomyTime(now)
        val jupiter=geoVector(Body.Jupiter,time,Aberration.Corrected)
        for (body in moons) {
            val actual=planetCompassJovianMoonGeoVector(body,time,Aberration.Corrected)
            val delayed=planetCompassJovianMoonState(body,time.addDays(-jupiter.length()/C_AUDAY))
            assertEquals(jupiter.x+delayed.x,actual.x,1e-12)
            assertEquals(jupiter.y+delayed.y,actual.y,1e-12)
            assertEquals(jupiter.z+delayed.z,actual.z,1e-12)
            val simultaneous=planetCompassJovianMoonState(body,time)
            assertTrue(sqrt((simultaneous.x-delayed.x).pow(2)+(simultaneous.y-delayed.y).pow(2)+
                (simultaneous.z-delayed.z).pow(2))*PLANET_COMPASS_AU_KM > 10_000)
        }
    }
    @Test fun moonsStayNearJupiterOnSkyButDependOnObserverLocation() {
        val jupiter=calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.JUPITER,now,45.0,9.0)!!
        for(body in moons) {
            val observation=calculatePlanetCompassCelestialObservation(body,now,45.0,9.0)!!
            val other=calculatePlanetCompassCelestialObservation(body,now,-33.0,151.0)!!
            assertTrue(abs(observation.position.elevationDegrees-jupiter.position.elevationDegrees)<0.15)
            assertTrue(abs(observation.distanceKm-jupiter.distanceKm)<800_000)
            assertNotEquals(observation.position,other.position)
        }
    }
    @Test fun bothBodiesHaveCompleteApparentDayCurvesAboveAndBelowTheHorizon() {
        val date=Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate()
        for(body in moons) {
            val path=calculatePlanetCompassCelestialPath(body,date,ZoneOffset.UTC,now,45.0,9.0,0.0,PlanetCompassCelestialRemoteData())!!
            assertEquals(24,path.markers.count { it.event==PlanetCompassSunPathEvent.HOUR })
            assertTrue(path.samples.any { it.position.elevationDegrees<0 })
            assertTrue(path.samples.any { it.position.elevationDegrees>0 })
            assertEquals(body,path.body)
        }
    }
    @Test fun orientationUsesUnitPoleAndFiniteIauPrimeMeridian() {
        for(body in moons) for(day in listOf(0,1,1000)) {
            val axis=planetCompassJovianMoonAxis(body,planetCompassAstronomyTime(now+day*86_400_000L))
            assertEquals(1.0,axis.north.length(),1e-12)
            assertTrue(axis.ra in 17.0..19.0); assertTrue(axis.dec in 63.0..66.0)
            assertTrue(axis.spin.isFinite())
        }
    }
    @Test fun referenceFactsDistinguishJupiterFromSunAndUseSiderealPeriods() {
        val io=planetCompassCelestialFacts(PlanetCompassCelestialBody.IO)
        val europa=planetCompassCelestialFacts(PlanetCompassCelestialBody.EUROPA)
        assertEquals(PlanetCompassCelestialBody.JUPITER,io.parent)
        assertEquals(PlanetCompassCelestialBody.JUPITER,europa.parent)
        assertEquals(1.769,io.revolutionDays!!,0.001)
        assertEquals(3.551,europa.revolutionDays!!,0.001)
        assertEquals(1.796,io.gravity!!,0.005)
        assertEquals(1.315,europa.gravity!!,0.005)
        assertEquals(3527.6,io.density!!,0.0)
        assertEquals(3013.0,europa.density!!,0.0)
    }
}
