package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SpaceCompassObservationMomentRowsTest {
    private fun moment(kind: SpaceCompassObservationMomentKind, time: Long, height: Double) =
        SpaceCompassObservationMoment(kind,SpaceCompassSunPathPoint(time,SpaceCompassSunPosition(180.0,height),
            SpaceCompassSunPathEvent.CULMINATION))
    private val maximum = SpaceCompassObservationMomentKind.MAXIMUM
    private val minimum = SpaceCompassObservationMomentKind.MINIMUM
    private val rising = SpaceCompassObservationMomentKind.MID_RISING
    private val setting = SpaceCompassObservationMomentKind.MID_SETTING

    @Test fun absoluteExtremesAppearFirstOnceAndOtherExtremaHaveChronologicalNumbers() {
        val input = listOf(moment(maximum,30,55.0),moment(minimum,40,20.0),moment(maximum,20,60.0),
            moment(minimum,50,10.0),moment(maximum,10,50.0))
        val rows = spaceCompassObservationMomentRows(input)
        assertEquals(5,rows.size)
        assertEquals(listOf(20L,50L),rows.filter { it.annualExtreme }.map { it.point!!.timeMs })
        assertTrue(rows.take(2).all { it.number==null })
        val otherMaxima = rows.filter { it.kind==maximum && !it.annualExtreme }
        assertEquals(listOf(10L,30L),otherMaxima.map { it.point!!.timeMs })
        assertEquals(listOf(1,2),otherMaxima.map { it.number })
        assertNull(rows.single { it.kind==minimum && !it.annualExtreme }.number)
        assertEquals(5,rows.distinctBy { it.kind to it.point!!.timeMs }.size)
    }

    @Test fun midpointDirectionsHaveIndependentChronologicalCountersWithoutLosingInterleaving() {
        val input = listOf(moment(rising,50,40.0),moment(setting,40,40.0),moment(rising,10,40.0),moment(setting,20,40.0))
        val rows = spaceCompassObservationMomentRows(input)
        assertEquals(listOf(10L,20L,40L,50L),rows.map { it.point!!.timeMs })
        assertEquals(listOf(1,1,2,2),rows.map { it.number })
        assertTrue(rows.none { it.annualExtreme })
    }

    @Test fun duplicateEventsDoNotDuplicateAnnualSummariesOrCycleCounters() {
        val a = moment(maximum,10,60.0); val b = moment(minimum,20,20.0)
        val rows = spaceCompassObservationMomentRows(listOf(a,b,a,b,moment(maximum,30,50.0)))
        assertEquals(3,rows.size); assertEquals(2,rows.count { it.annualExtreme })
        assertNull(rows.last().number)
    }

    @Test fun equalHeightExtremesOnDifferentDatesRemainSeparatePhysicalEvents() {
        val rows = spaceCompassObservationMomentRows(listOf(moment(maximum,30,80.0),moment(maximum,10,80.0),
            moment(minimum,40,20.0),moment(minimum,20,20.0)))
        assertEquals(4,rows.size)
        assertEquals(listOf(10L,20L),rows.take(2).map { it.point!!.timeMs })
        assertEquals(listOf(30L,40L),rows.drop(2).map { it.point!!.timeMs })
        assertTrue(rows.drop(2).all { it.number==null })
    }

    @Test fun anEmptyAnnualResultHasNoInventedNumberedCycleEvents() {
        assertTrue(spaceCompassObservationMomentRows(emptyList()).isEmpty())
    }

    @Test fun actualLunarYearRetainsEveryEventWithOnlyTwoAbsoluteSummaries() {
        val values = calculateSpaceCompassObservationMoments(SpaceCompassCelestialBody.MOON,
            Instant.parse("2026-10-31T12:00:00Z").toEpochMilli(),ZoneId.of("Europe/Rome"),45.0,9.0,100.0,
            SpaceCompassCelestialRemoteData())!!
        val rows = spaceCompassObservationMomentRows(values)
        assertEquals(values.size,rows.size); assertEquals(2,rows.count { it.annualExtreme })
        assertEquals(values.filter { it.kind==maximum }.maxOf { it.point.position.elevationDegrees },
            rows[0].point!!.position.elevationDegrees,0.0)
        assertEquals(values.filter { it.kind==minimum }.minOf { it.point.position.elevationDegrees },
            rows[1].point!!.position.elevationDegrees,0.0)
        for (kind in SpaceCompassObservationMomentKind.entries) {
            val cycle = rows.filter { it.kind==kind && !it.annualExtreme }
            if (cycle.size > 1) assertEquals((1..cycle.size).toList(),cycle.map { it.number })
            else assertTrue(cycle.all { it.number==null })
            assertTrue(cycle.zipWithNext().all { (a,b)-> a.point!!.timeMs < b.point!!.timeMs })
        }
    }

    @Test fun uniqueMidpointsDoNotShowNumbersWhileRepeatedDirectionsAreNumberedIndependently() {
        val rows = spaceCompassObservationMomentRows(listOf(moment(rising,10,40.0),moment(setting,20,40.0),
            moment(rising,30,40.0),moment(rising,50,40.0)))
        assertEquals(listOf(1,2,3),rows.filter { it.kind==rising }.map { it.number })
        assertNull(rows.single { it.kind==setting }.number)
        val unique = spaceCompassObservationMomentRows(listOf(moment(rising,10,40.0),moment(setting,20,40.0)))
        assertTrue(unique.all { it.number==null })
    }

    @Test fun annualSummaryDoesNotForceANumberOnASingleRemainingCycleEvent() {
        val rows = spaceCompassObservationMomentRows(listOf(moment(maximum,10,70.0),moment(maximum,20,60.0),
            moment(minimum,30,20.0),moment(minimum,40,30.0),moment(minimum,50,25.0)))
        assertNull(rows.single { it.kind==maximum && !it.annualExtreme }.number)
        assertEquals(listOf(1,2),rows.filter { it.kind==minimum && !it.annualExtreme }.map { it.number })
        assertTrue(rows.take(2).all { it.annualExtreme && it.number==null })
    }

    @Test fun actualSolarYearAtMidLatitudeHasNoNumberedLabels() {
        val values = calculateSpaceCompassObservationMoments(SpaceCompassCelestialBody.SUN,
            Instant.parse("2026-10-31T12:00:00Z").toEpochMilli(),ZoneId.of("Europe/Rome"),45.0,9.0,100.0,
            SpaceCompassCelestialRemoteData())!!
        val rows = spaceCompassObservationMomentRows(values)
        assertEquals(4,rows.size)
        assertEquals(2,rows.count { it.annualExtreme })
        assertEquals(listOf(rising,setting),rows.drop(2).map { it.kind })
        assertTrue(rows.all { it.number==null })
    }

}
