package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.Properties
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassComparisonBulkTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val sun = SpaceCompassCelestialBody.SUN
    private val now = Instant.parse("2026-06-21T11:25:00Z").toEpochMilli()
    private val position = SpaceCompassSunPosition(180.0, 68.0)
    private fun comparison(day: Int = 21, source: SpaceCompassComparisonSource = SpaceCompassComparisonSource.HEIGHT,
        body: SpaceCompassCelestialBody = sun, kind: String = "MONTHLY") = SpaceCompassTemporalComparison.create(
        body, now + (day - 21) * 86_400_000L, zone, source, "$kind:2026-06-$day")
    private fun snapshot(c: SpaceCompassTemporalComparison) = SpaceCompassPanoramaSnapshot(now,45.0,9.0,0.0,
        SpaceCompassSunSkyPhase.AFTERNOON,null,emptyList(),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),
        comparisonPointNames=mapOf(c.id to "Culmine"))

    @Test fun titleAddsAllWhenEmptyAndClearsEvenAPartialSelectionWithoutTouchingOtherScopes() {
        val state = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState())
        val choices = listOf(comparison(),comparison(22))
        val other = listOf(comparison(23), comparison(source=SpaceCompassComparisonSource.EVENT,kind="FULL_MOON"),
            comparison(body=SpaceCompassCelestialBody.MOON))
        state.restore(other,other.first().id)
        state.toggleAll(choices)
        assertEquals((other+choices).map { it.id }.toSet(),state.entries.map { it.id }.toSet())
        assertEquals(choices.last().id,state.activeId)
        state.toggleAll(choices)
        assertEquals(other,state.entries)
        state.restore(other+choices.first(),other.first().id)
        state.toggleAll(choices)
        assertEquals(other,state.entries)
        assertEquals(other.first().id,state.activeId)
    }

    @Test fun clearingThePagePrunesOnlyItsComparisonPinsAndTurnsOffUnusedAntipodes() {
        val points = SpaceCompassSunDailyPathUiState()
        val parents = mutableListOf<SpaceCompassCelestialBody>()
        val state = SpaceCompassTemporalComparisonState(points) { parents.add(it) }
        val c = comparison()
        val point = SpaceCompassSunPathPoint(now,position)
        val path = SpaceCompassSunDailyPath(Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),zone,
            listOf(point),listOf(point),comparison=c)
        state.restore(listOf(c),c.id)
        points.selectCurrent(sun,point,path); points.toggleAntipodes()
        state.toggleAll(listOf(c))
        assertTrue(state.entries.isEmpty()); assertTrue(points.selectedPoints.isEmpty())
        assertFalse(points.antipodesVisible); assertNull(state.activeId)
        assertEquals(listOf(sun), parents)
    }

    @Test fun bulkNeverPartiallyAddsBeyondTheExistingLimit() {
        val state = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState())
        val existing = (0 until 511).map { SpaceCompassTemporalComparison.create(sun,now,zone,
            SpaceCompassComparisonSource.EVENT,"event-$it") }
        state.restore(existing,null)
        val candidates = listOf(comparison(),comparison(22))
        assertFalse(state.canToggleAll(candidates))
        state.toggleAll(candidates); assertEquals(existing,state.entries)
        state.toggleAll(emptyList()); assertEquals(existing,state.entries)
    }

    @Test fun choicesMatchEnabledRowsAndKeepAnnualVersusCycleNames() {
        val monthly = SpaceCompassObservationMoment(SpaceCompassObservationMomentKind.MONTHLY,SpaceCompassSunPathPoint(now,position))
        val maximum = SpaceCompassObservationMoment(SpaceCompassObservationMomentKind.MAXIMUM,SpaceCompassSunPathPoint(now,position))
        val loading = SpaceCompassObservationMomentsUiState(listOf(monthly,maximum),false,true)
        assertTrue(spaceCompassHeightComparisonChoices(sun,loading,zone).isEmpty())
        val choices = spaceCompassHeightComparisonChoices(sun,loading.copy(complete=true),zone)
        assertEquals(2,choices.size)
        assertEquals(R.string.observation_height_maximum,spaceCompassComparisonPointNameResource(choices.first()))
        assertEquals(R.string.sun_path_culmination,spaceCompassComparisonPointNameResource(choices.last()))
        val events = SpaceCompassKeyEventsUiState(listOf(SpaceCompassKeyEvent(SpaceCompassKeyEventKind.FULL_MOON,now,5.0)),true,true)
        val event = spaceCompassEventComparisonChoices(sun,events,zone).single()
        assertEquals(R.string.key_event_full_moon,spaceCompassComparisonPointNameResource(event))
    }

    @Test fun optionalAnnualNameMetadataSurvivesArchivesWithoutChangingIdentityOrLegacyFields() {
        val c = SpaceCompassTemporalComparison.create(sun,now,zone,SpaceCompassComparisonSource.HEIGHT,
            "MAXIMUM:2026-06-21",annualExtreme=true)
        val p = Properties().apply { putSpaceCompassComparisons(listOf(c),emptyList(),c.id,false) }
        assertEquals(c,p.readSpaceCompassComparisons()!!.single())
        assertEquals(6,p.getProperty("comparison.0").split('|').size)
        p.remove("comparisonAnnual.0")
        val legacy = p.readSpaceCompassComparisons()!!.single()
        assertEquals(c.id,legacy.id); assertFalse(legacy.annualExtreme)
        assertEquals(R.string.sun_path_culmination,spaceCompassComparisonPointNameResource(legacy))
    }

    @Test fun coincidentCaptionsRequireTheSameInstantAndDirectionAndPreserveSelectedOnlyVisibility() {
        val c = comparison(); val snap = snapshot(c)
        val point = SpaceCompassSunPathPoint(now+1000,position,SpaceCompassSunPathEvent.CULMINATION)
        val item = SpaceCompassPanoramaObject(sun,"Sun",null,c)
        assertTrue(snap.hasComparisonAnchorLabel(item,point,position,emptyList()))
        assertFalse(snap.hasComparisonAnchorLabel(item,point.copy(timeMs=now+180_000),position,emptyList()))
        assertFalse(snap.hasComparisonAnchorLabel(item,point.copy(position=SpaceCompassSunPosition(190.0,68.0)),position,emptyList()))
        assertFalse(snap.copy(labelMode=SpaceCompassPanoramaLabelMode.SELECTED).hasComparisonAnchorLabel(item,point,position,emptyList()))
        val current = SpaceCompassPanoramaSelectedPoint(sun,point.copy(timeMs=now),null,true,comparison=c)
        assertTrue(snap.copy(labelMode=SpaceCompassPanoramaLabelMode.SELECTED).hasComparisonAnchorLabel(item,point,position,listOf(current)))
        assertEquals("Culmine",snap.forCurrentInstance(c).currentPointName)
    }

    @Test fun selectedAnchorAndRefinedCulminationHaveOneCaptionButRetainBothSelectionRecords() {
        val c = comparison(); val snap = snapshot(c)
        val current = SpaceCompassPanoramaSelectedPoint(sun,SpaceCompassSunPathPoint(now,position),null,true,comparison=c)
        val refined = current.copy(point=SpaceCompassSunPathPoint(now+1000,position,SpaceCompassSunPathEvent.CULMINATION),isCurrent=false)
        val pins = listOf(refined,current)
        assertEquals(listOf(current),spaceCompassUniqueComparisonCaptionPins(pins))
        assertEquals(2,pins.size)
        assertTrue(formatSpaceCompassPanoramaSelection(snap,refined).startsWith("Culmine"))
        assertFalse(formatSpaceCompassPanoramaSelection(snap,refined).contains("Reference"))
    }
}
