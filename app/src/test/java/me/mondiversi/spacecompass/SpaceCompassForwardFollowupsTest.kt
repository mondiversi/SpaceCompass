package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassForwardFollowupsTest {
    private val now=Instant.parse("2026-10-03T18:35:00Z").toEpochMilli()
    private val end=now+86_400_000L
    private val zone=ZoneId.of("Europe/Rome")
    private fun point(offset: Long,elevation: Double,event: SpaceCompassSunPathEvent=SpaceCompassSunPathEvent.HOUR)=
        SpaceCompassSunPathPoint(now+offset,SpaceCompassSunPosition(180.0,elevation),event)
    private fun path(elevation: Double,markers: List<SpaceCompassSunPathPoint>)=SpaceCompassSunDailyPath(
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),zone,
        listOf(point(0,elevation),point(60_000,elevation),point(86_400_000,-20.0)),markers,
        SpaceCompassCelestialBody.ISS,forward24Hours=true)

    @Test fun firstUpcomingPassUsesRealEventsRatherThanAnEmptySummary() {
        val first=listOf(point(3_600_000,0.0,SpaceCompassSunPathEvent.SUNRISE),
            point(3_900_000,30.0,SpaceCompassSunPathEvent.CULMINATION),
            point(4_200_000,0.0,SpaceCompassSunPathEvent.SUNSET))
        val later=point(8_000_000,0.0,SpaceCompassSunPathEvent.SUNRISE)
        assertEquals(first,spaceCompassForwardSatellitePass(path(-20.0,first+later)))
    }

    @Test fun ongoingPassStartsAtCurrentAndContainsNoPastRise() {
        val peak=point(300_000,50.0,SpaceCompassSunPathEvent.CULMINATION)
        val set=point(600_000,0.0,SpaceCompassSunPathEvent.SUNSET)
        val p=path(20.0,listOf(point(-300_000,0.0,SpaceCompassSunPathEvent.SUNRISE),peak,set))
        assertEquals(listOf(p.samples.first(),peak,set),spaceCompassForwardSatellitePass(p))
        assertTrue(spaceCompassForwardSatellitePass(p).all { it.timeMs>=now })
    }

    @Test fun passCrossingTheWindowEndIsPartialInsteadOfMissing() {
        val rise=point(86_000_000,0.0,SpaceCompassSunPathEvent.SUNRISE)
        val outside=point(87_000_000,45.0,SpaceCompassSunPathEvent.CULMINATION)
        assertEquals(listOf(rise),spaceCompassForwardSatellitePass(path(-20.0,listOf(rise,outside))))
    }

    @Test fun noPassAndContinuousVisibilityAreDistinguished() {
        assertTrue(spaceCompassForwardSatellitePass(path(-20.0,
            listOf(point(3_600_000,-2.0,SpaceCompassSunPathEvent.CULMINATION)))).isEmpty())
        val visible=path(15.0,emptyList())
        assertEquals(listOf(visible.samples.first()),spaceCompassForwardSatellitePass(visible))
    }

    @Test fun actualIssWindowHasPassesWithoutDuplicatingThemInExports() {
        val orbit=SpaceCompassIssOrbit.parse("1 25544U 98067A   26276.04623379  .00005750  00000+0  11349-3 0  9994\n"+
            "2 25544  51.6314 126.3061 0006899 216.4733 143.5786 15.48722558588472")
        val p=requireNotNull(calculateSpaceCompassCelestialPath(SpaceCompassCelestialBody.ISS,
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),zone,now,45.0,9.0,0.0,
            SpaceCompassCelestialRemoteData(iss=orbit),true))
        assertTrue(p.issPass.isNotEmpty())
        assertTrue(p.issPass.all { it.timeMs in now..end })
        assertEquals(spaceCompassForwardSatellitePass(p),p.issPass)
        val snapshot=spaceCompassSingleOrbitExportSnapshot(p,p.samples.first(),emptyList(),now)
        val table=spaceCompassSingleOrbitExportTable(snapshot,p,p.samples.first())
        assertFalse(table.rows.any { it.kind==SpaceCompassOrbitExportRowKind.PASS })
        assertEquals(1,table.rows.count { it.kind==SpaceCompassOrbitExportRowKind.CURRENT })
    }

    @Test fun modeChangesResetTheVisitWhileSameModeAndOtherPagesRetainTheirPosition() {
        val session=SpaceCompassObjectPageScrollSession();val body=SpaceCompassCelestialBody.SUN
        val normal=session.open(body,SpaceCompassObjectPage.DAILY_PATH)
        normal.entry.position.request(1200);normal.entry.headerVisible=false
        val observatory=session.open(body,SpaceCompassObjectPage.OBSERVATORY)
        observatory.entry.position.request(700)
        assertTrue(session.open(body,SpaceCompassObjectPage.DAILY_PATH).returning)
        val forward=session.open(body,SpaceCompassObjectPage.DAILY_PATH,true)
        assertFalse(forward.returning);assertEquals(0,forward.entry.position.offset);assertTrue(forward.entry.headerVisible)
        forward.entry.position.request(900)
        val same=session.open(body,SpaceCompassObjectPage.DAILY_PATH,true)
        assertTrue(same.returning);assertEquals(900,same.entry.position.offset)
        val back=session.open(body,SpaceCompassObjectPage.DAILY_PATH)
        assertFalse(back.returning);assertEquals(0,back.entry.position.offset)
        assertEquals(700,session.open(body,SpaceCompassObjectPage.OBSERVATORY).entry.position.offset)
    }

    @Test fun intervalHeadingUsesBothDatesAndTheSelectedClockAcrossDst() {
        val start=Instant.parse("2026-10-24T11:00:00Z").toEpochMilli()
        val text=formatSpaceCompassPathInterval(start,start+86_400_000L,zone,SpaceCompassTimeFormat.H24,
            SpaceCompassDateFormat.EUROPEAN,SpaceCompassNumericFormat.INTERNATIONAL,Locale.ITALIAN,Locale.ITALIAN)
        assertEquals("24/10/2026 · 13:00 → 25/10/2026 · 12:00",text)
    }
}
