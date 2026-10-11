package me.mondiversi.spacecompass

import java.util.Locale
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceCompassPanoramaPointLabelTest {
    private val scientific = spaceCompassPanoramaInternationalFormatting
    private val italian = SpaceCompassPanoramaFormatting(locale = Locale.ITALIAN,
        numeric = SpaceCompassNumericFormat.SYSTEM, deviceLocale = Locale.ITALIAN)

    @Test fun currentPointKeepsTheFrozenMomentAndShowsItsOwnElevation() {
        assertEquals("Current · 23:59 T+00:00 · +5.2°",
            formatSpaceCompassPanoramaPointLabel("23:59", 5.24, scientific, "Current",
                formatSpaceCompassPanoramaPointCountdown(1_000L, 1_000L, scientific)))
    }

    @Test fun selectedProfileAndScientificProfileFormatTheRawElevationIndependently() {
        assertEquals("Attuale · 17:42 · +20,8°",
            formatSpaceCompassPanoramaPointLabel("17:42", 20.75, italian, "Attuale"))
        assertEquals("Current · 17:42 · +20.8°",
            formatSpaceCompassPanoramaPointLabel("17:42", 20.75, scientific, "Current"))
    }

    @Test fun nearHorizonValuesDoNotProduceSignedRoundedZeroInEitherProfile() {
        for (value in listOf(-.01, -0.0, 0.0, .01)) {
            assertEquals("12:00 · 0.0°", formatSpaceCompassPanoramaPointLabel("12:00", value, scientific))
            assertEquals("12:00 · 0,0°", formatSpaceCompassPanoramaPointLabel("12:00", value, italian))
        }
    }

    private fun point(event: SpaceCompassSunPathEvent, coincident: Set<SpaceCompassSunPathEvent> = emptySet()) =
        SpaceCompassSunPathPoint(1_000L, SpaceCompassSunPosition(150.0, 20.75), event, coincidentEvents = coincident)

    @Test fun allPrincipalEventsHaveNamesWhileRegularHourlyPointsKeepOnlyTimeAndElevation() {
        val expected = mapOf(SpaceCompassSunPathEvent.HOUR to "12:00 T+00:00 · +20.8°",
            SpaceCompassSunPathEvent.SUNRISE to "Rise · 12:00 T+00:00 · +20.8°",
            SpaceCompassSunPathEvent.CULMINATION to "Culmination · 12:00 T+00:00 · +20.8°",
            SpaceCompassSunPathEvent.SUNSET to "Set · 12:00 T+00:00 · +20.8°",
            SpaceCompassSunPathEvent.MINIMUM to "Minimum · 12:00 T+00:00 · +20.8°")
        for ((event, label) in expected) assertEquals(label,
            formatSpaceCompassPanoramaPathPointLabel("12:00", point(event), scientific, spaceCompassPanoramaDefaultEventNames, 1_000L))
    }

    @Test fun coincidentEventsKeepAllDistinctNamesWithoutAnHourlyName() {
        val merged = point(SpaceCompassSunPathEvent.MINIMUM, setOf(SpaceCompassSunPathEvent.SUNSET,
            SpaceCompassSunPathEvent.HOUR, SpaceCompassSunPathEvent.MINIMUM, SpaceCompassSunPathEvent.SUNRISE))
        assertEquals("Minimum · Rise · Set · 12:00 T+00:00 · +20.8°",
            formatSpaceCompassPanoramaPathPointLabel("12:00", merged, scientific, spaceCompassPanoramaDefaultEventNames, 1_000L))
    }

    @Test fun simultaneousMarkersFromDifferentObjectsKeepTheirOwnEventNames() {
        val first = point(SpaceCompassSunPathEvent.SUNRISE)
        val second = point(SpaceCompassSunPathEvent.MINIMUM)
        assertEquals(first.timeMs, second.timeMs)
        assertEquals("Rise · 12:00 T+00:00 · +20.8°",
            formatSpaceCompassPanoramaPathPointLabel("12:00", first, scientific, spaceCompassPanoramaDefaultEventNames, 1_000L))
        assertEquals("Minimum · 12:00 T+00:00 · +20.8°",
            formatSpaceCompassPanoramaPathPointLabel("12:00", second, scientific, spaceCompassPanoramaDefaultEventNames, 1_000L))
    }

    @Test fun eventNamesAndElevationUseTheFrozenLanguageAndFormattingForEachPresentation() {
        val translated = mapOf(SpaceCompassSunPathEvent.CULMINATION to "Culmine")
        val marker = point(SpaceCompassSunPathEvent.CULMINATION)
        assertEquals("Culmine · 17:42 T+00:00 · +20,8°",
            formatSpaceCompassPanoramaPathPointLabel("17:42", marker, italian, translated, 1_000L))
        assertEquals("Culmination · 17:42 T+00:00 · +20.8°",
            formatSpaceCompassPanoramaPathPointLabel("17:42", marker, scientific, spaceCompassPanoramaDefaultEventNames, 1_000L))
    }

    @Test fun pathLabelsAndCurrentLabelsShareTheSameAngleConventionAboveAndBelowTheHorizon() {
        assertEquals("04:00 · -21.4°", formatSpaceCompassPanoramaPointLabel("04:00", -21.4, scientific))
        assertEquals("Current · 04:00 · -21.4°", formatSpaceCompassPanoramaPointLabel("04:00", -21.4, scientific, "Current"))
        assertEquals("Current · 04:00 · +90.0°", formatSpaceCompassPanoramaPointLabel("04:00", 90.0, scientific, "Current"))
    }

    @Test fun repeatedLocalHoursStillUseTheFrozenUtcDurationInBothDirections() {
        val before = Instant.parse("2026-10-25T00:30:00Z").toEpochMilli()
        val after = Instant.parse("2026-10-25T01:30:00Z").toEpochMilli()
        fun label(time: Long, captured: Long) = formatSpaceCompassPanoramaPathPointLabel("02:30",
            SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(150.0, 20.75), SpaceCompassSunPathEvent.CULMINATION),
            italian, mapOf(SpaceCompassSunPathEvent.CULMINATION to "Culmine"), captured)
        assertEquals("Culmine · 02:30 T−01:00 · +20,8°", label(after, before))
        assertEquals("Culmine · 02:30 T+01:00 · +20,8°", label(before, after))
        assertEquals("Culmine · 02:30 T+00:00 · +20,8°", label(after, after))
    }

    @Test fun countdownDigitsFollowSelectedAndInternationalProfilesWithoutSeconds() {
        val arabic = scientific.copy(numeric = SpaceCompassNumericFormat.SYSTEM,
            deviceLocale = Locale.forLanguageTag("ar-u-nu-arab"))
        assertEquals("T−٠١:٠١", formatSpaceCompassPanoramaPointCountdown(3_661_000L, 0L, arabic))
        assertEquals("T−01:01", formatSpaceCompassPanoramaPointCountdown(3_661_000L, 0L,
            arabic.copy(numeric = SpaceCompassNumericFormat.INTERNATIONAL)))
        assertEquals("T+01:01", formatSpaceCompassPanoramaPointCountdown(0L, 3_661_000L, scientific))
    }
}
