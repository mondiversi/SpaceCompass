package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SpaceCompassForwardPathTest {
    private val remote=SpaceCompassCelestialRemoteData()
    private fun path(time: String,zone: String="Europe/Rome",body: SpaceCompassCelestialBody=SpaceCompassCelestialBody.SUN) =
        requireNotNull(calculateSpaceCompassCelestialPath(body,Instant.parse(time).atZone(ZoneId.of(zone)).toLocalDate(),
            ZoneId.of(zone),Instant.parse(time).toEpochMilli(),45.0,9.0,120.0,remote,forward24Hours=true))

    @Test fun startsExactlyNowAndCrossesMidnightWithOnlyFutureMarkers() {
        val now=Instant.parse("2026-11-02T22:27:35Z").toEpochMilli()
        val path=path("2026-11-02T22:27:35Z")
        assertTrue(path.forward24Hours)
        assertEquals(now,path.samples.first().timeMs)
        assertEquals(now+86_400_000L,path.samples.last().timeMs)
        assertTrue(path.markers.all { it.timeMs > now && it.timeMs <= now+86_400_000L })
        assertEquals(24,path.markers.count { it.event==SpaceCompassSunPathEvent.HOUR })
        assertTrue(path.markers.any { Instant.ofEpochMilli(it.timeMs).atZone(path.zone).toLocalDate()>path.date })
        val entries=spaceCompassDailyPathListEntries(path,path.samples.first())
        assertTrue(entries.first().isCurrent)
        assertTrue(entries.drop(1).all { it.point.timeMs>now })
    }

    @Test fun exactly24ElapsedHoursAcrossBothDstChangesAndFractionalZones() {
        for (time in listOf("2026-03-28T22:17:00Z","2026-10-24T22:17:00Z")) {
            val p=path(time)
            assertEquals(86_400_000L,p.samples.last().timeMs-p.samples.first().timeMs)
            assertEquals(24,p.markers.count { it.event==SpaceCompassSunPathEvent.HOUR })
        }
        val india=path("2026-11-02T22:17:00Z","Asia/Kolkata")
        assertTrue(india.markers.filter { it.event==SpaceCompassSunPathEvent.HOUR }.all {
            Instant.ofEpochMilli(it.timeMs).atZone(india.zone).minute==0
        })
    }

    @Test fun normalDayRemainsTheDefault() {
        val now=Instant.parse("2026-11-02T22:27:35Z").toEpochMilli()
        val zone=ZoneId.of("Europe/Rome");val date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val p=requireNotNull(calculateSpaceCompassCelestialPath(SpaceCompassCelestialBody.SUN,date,zone,now,45.0,9.0,120.0,remote))
        assertFalse(p.forward24Hours)
        assertEquals(date.atStartOfDay(zone).toInstant().toEpochMilli(),p.samples.first().timeMs)
    }

    @Test fun moonEndpointsUseTheCorrectFutureEphemerisAndEventsAreChronological() {
        val p=path("2026-06-21T15:33:12Z",body=SpaceCompassCelestialBody.MOON)
        for (point in listOf(p.samples.first(),p.samples.last())) {
            val expected=requireNotNull(calculateSpaceCompassCelestialObservation(p.body,point.timeMs,45.0,9.0,120.0,remote)).position
            assertEquals(expected.azimuthDegrees,point.position.azimuthDegrees,1e-9)
            assertEquals(expected.elevationDegrees,point.position.elevationDegrees,1e-9)
        }
        assertEquals(p.markers.sortedBy { it.timeMs },p.markers)
        assertTrue(p.markers.all { it.moonPhase!=null })
    }

    @Test fun bufferAnchorIsStableAndUnavailableFutureDataIsNotInvented() {
        val now=Instant.parse("2026-11-02T22:27:35Z").toEpochMilli()
        assertEquals(spaceCompassForwardPathAnchor(now),spaceCompassForwardPathAnchor(now+60_000))
        assertNotEquals(spaceCompassForwardPathAnchor(now),spaceCompassForwardPathAnchor(now+3_600_000))
        assertNull(calculateSpaceCompassCelestialPath(SpaceCompassCelestialBody.ISS,
            Instant.ofEpochMilli(now).atZone(ZoneId.of("UTC")).toLocalDate(),ZoneId.of("UTC"),now,45.0,9.0,120.0,remote,true))
    }

    @Test fun futureSelectionSurvivesMidnightButPastPinsLeaveTheWindow() {
        val first=path("2026-11-02T22:27:35Z")
        val selected=first.markers.first { it.event==SpaceCompassSunPathEvent.HOUR && it.timeMs >= first.samples.first().timeMs+3*3_600_000L &&
            Instant.ofEpochMilli(it.timeMs).atZone(first.zone).toLocalDate()>first.date }
        val state=SpaceCompassSunDailyPathUiState();state.select(first,selected)
        val later=path("2026-11-02T23:10:00Z")
        assertTrue(state.isSelected(later,selected))
        assertTrue(spaceCompassPathAcceptsSelection(later,state.selectedPoints.single()))
        val past=path("2026-11-03T03:10:00Z")
        assertFalse(spaceCompassPathAcceptsSelection(past,state.selectedPoints.single()))
        assertEquals(1,state.selectedPoints.size)
        assertTrue(spaceCompassSelectedDailyPathEntries(past,past.samples.first(),state.selectedPoints).all {
            it.point.timeMs>=past.samples.first().timeMs
        })
    }

    @Test fun exportsContainTheSameForwardWindowAndDoNotRestorePastHours() {
        val p=path("2026-11-02T22:27:35Z")
        val now=p.samples.first()
        val snapshot=spaceCompassSingleOrbitExportSnapshot(p,now,emptyList(),now.timeMs)
        assertTrue(snapshot.forward24Hours)
        val table=spaceCompassSingleOrbitExportTable(snapshot,p,now)
        assertTrue(table.rows.all { row -> row.cells.values.all { it.point.timeMs>=now.timeMs && it.point.timeMs<=now.timeMs+86_400_000L } })
        assertTrue(table.rows.none { it.cells.isEmpty() })
        assertEquals(24,table.rows.count { it.kind==SpaceCompassOrbitExportRowKind.POINT })
    }
}
