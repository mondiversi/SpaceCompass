package me.mondiversi.spacecompass

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

/** Independent reference conversions and mixed saved choices across every catalogue object. */
class SpaceCompassUnitsAuditTest {
    private val american = SpaceCompassNumericFormat.AMERICAN
    private val arabic = Locale.forLanguageTag("ar-EG")
    private val utc = TimeZone.getTimeZone("UTC")
    private val stamp = Instant.parse("2026-10-04T12:30:45Z").toEpochMilli()

    private fun decimal(text: String, format: SpaceCompassNumericFormat): Double {
        val symbols = spaceCompassNumericFormatSymbols(format)
        return text.filterNot { it == symbols.groupingSeparator }
            .replace(symbols.decimalSeparator, '.').toDouble()
    }

    @Test fun speedConversionKeepsTheSameExactMileAsDistanceAndPreservesProbeDirection() {
        assertEquals("1.00", formatSpaceCompassSpeed(1.609344, american, miles = true))
        assertEquals("1.61", formatSpaceCompassSpeed(1.609344, american, miles = false))
        assertEquals("-1.00", formatSpaceCompassSpeed(-1.609344, american, miles = true, allowNegative = true))
        assertEquals("0.00", formatSpaceCompassSpeed(0.0, american, miles = true))
        assertNull(formatSpaceCompassSpeed(-1.609344, american, miles = true))
        for (bad in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertNull(formatSpaceCompassSpeed(bad, american, miles = true, allowNegative = true))
    }

    @Test fun gravityFollowsLengthChoiceButTheEarthGravityRatioIsInvariant() {
        assertEquals("32.2 ft/s² (1 g)", formatSpaceCompassCelestialGravity(9.80665, american, feet = true))
        assertEquals("9.8 m/s² (1 g)", formatSpaceCompassCelestialGravity(9.80665, american, feet = false))
        assertEquals("898,950 ft/s² (27,94 g)", formatSpaceCompassCelestialGravity(274.0,
            SpaceCompassNumericFormat.EUROPEAN, 3, feet = true))
        for (format in SpaceCompassNumericFormat.entries) for (body in SpaceCompassCelestialBody.entries) {
            val gravity = spaceCompassCelestialFacts(body).gravity ?: continue
            val imperial = formatSpaceCompassCelestialGravity(gravity, format, 3, feet = true)
            val metric = formatSpaceCompassCelestialGravity(gravity, format, 3, feet = false)
            assertEquals(body.name, gravity / 0.3048, decimal(imperial.substringBefore(" ft/s²"), format), 0.000501)
            assertEquals(metric.substringAfter('('), imperial.substringAfter('('))
        }
    }

    @Test fun negativeAltitudeAndBothUncertaintiesUseTheSavedLengthFamily() {
        assertEquals("-10 ft (±5 ft)", formatSpaceCompassCelestialAltitude(-3.048, 1.524, american, true))
        assertEquals("5 ft", formatSpaceCompassPhysicalLength(1.524, 0, american, true))
        assertEquals("-3 m (±2 m)", formatSpaceCompassCelestialAltitude(-3.048, 1.524, american, false))
    }

    @Test fun mixedSavedChoicesDoNotCouplePressureTemperatureOrMassToLength() {
        val saved = mapOf(SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY to "imperial", SPACE_COMPASS_MASS_UNIT_KEY to "kg",
            SPACE_COMPASS_PRESSURE_UNIT_KEY to "pa", "temperature" to "c", "coordinates" to "dms")
        for (region in listOf("IT", "US", "GB")) {
            val units = spaceCompassResolveUnits(region, saved::get)
            assertEquals("1.0 mi", formatSpaceCompassPhysicalLength(1609.344, 1, american, units.feet, large = true))
            assertEquals("1.000 × 10⁶ kg", formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.ISS,
                SpaceCompassCelestialFacts(massKg = 1e6), american, units.pounds))
            assertEquals("1.0 kg/ft³", formatSpaceCompassCelestialDensity(35.31466672148859, american, units))
            assertEquals("100,000 Pa", formatSpaceCompassPressure(100000.0, american, units.pressure))
            assertEquals("≈0 °C", formatSpaceCompassCelestialTemperature(
                SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.SURFACE_MEAN, 0.0), american, units.fahrenheit))
            assertTrue(units.dms)
        }
    }

    @Test fun everyPublishedDensityRoundTripsThroughAllFourMassVolumeCombinations() {
        for (format in SpaceCompassNumericFormat.entries) for (feet in listOf(false, true)) for (pounds in listOf(false, true))
            for (body in SpaceCompassCelestialBody.entries) {
                val density = spaceCompassCelestialFacts(body).density ?: continue
                val text = formatSpaceCompassCelestialDensity(density, format, SpaceCompassUnits(feet = feet, pounds = pounds), 3)
                val displayed = decimal(text.substringBefore(' '), format)
                val expected = density * (if (feet) 0.3048 * 0.3048 * 0.3048 else 1.0) / (if (pounds) 0.45359237 else 1.0)
                assertEquals(body.name + text, expected, displayed, 0.000501)
                assertTrue(text.endsWith((if (pounds) "lb" else "kg") + "/" + (if (feet) "ft³" else "m³")))
            }
    }

    @Test fun everyTemperatureRangeConvertsBothEndpointsAndTheKelvinReferenceStaysPhysical() {
        assertEquals("≈-460 °F", formatSpaceCompassCelestialTemperature(
            SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.SURFACE_MEAN, -273.15), american, true))
        for (body in SpaceCompassCelestialBody.entries) for (value in spaceCompassCelestialTemperatures(body)) {
            val text = formatSpaceCompassCelestialTemperature(value, american, true)
            val fields = text.substringAfter('≈').substringBefore(" °F").split(" … ")
            assertEquals(body.name, value.celsius * 9.0 / 5 + 32, decimal(fields.first(), american), 0.501)
            value.maximumCelsius?.let { assertEquals(body.name, it * 9.0 / 5 + 32, decimal(fields.last(), american), 0.501) }
            if (value.kind == SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE)
                assertEquals(value.celsius + 273.15, decimal(text.substringAfter('(').substringBefore(" K)"), american), 0.501)
        }
    }

    @Test fun fixedPressureReferencesAndNumericGroupingFollowTheSameSelectedFormatter() {
        assertEquals("14.5 psi", formatSpaceCompassPressure(100000.0, american, SpaceCompassPressureUnit.PSI))
        assertEquals("100.000 Pa", formatSpaceCompassPressure(100000.0, SpaceCompassNumericFormat.EUROPEAN, SpaceCompassPressureUnit.PASCAL))
        assertEquals("100 000 Pa", formatSpaceCompassPressure(100000.0, SpaceCompassNumericFormat.INTERNATIONAL, SpaceCompassPressureUnit.PASCAL))
        assertEquals("12,345.68", formatSpaceCompassNumber(12345.678, 2, american))
        assertEquals("12.345,68", formatSpaceCompassNumber(12345.678, 2, SpaceCompassNumericFormat.EUROPEAN))
        assertEquals("12 345.68", formatSpaceCompassNumber(12345.678, 2, SpaceCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun everyTranslationAcceptsFormattedPressureReferencesCountsAndZoom() {
        val root = File("src/main/res").takeIf { it.isDirectory } ?: File("app/src/main/res")
        val folders = root.listFiles()!!.filter { File(it, "celestial.xml").isFile }
        assertEquals(20, folders.size)
        for (folder in folders) for ((file, key) in listOf(
            "celestial_temperatures.xml" to "celestial_temperature_atmosphere",
            "celestial_viewer.xml" to "celestial_view_facts_note",
            "celestial_viewer.xml" to "celestial_view_zoom_level",
            "celestial_time.xml" to "celestial_point_number")) {
            val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(folder, file)).getElementsByTagName("string")
            val node = (0 until strings.length).map(strings::item).single { it.attributes.getNamedItem("name").nodeValue == key }
            val argument = if (key.contains("atmosphere") || key.contains("facts_note")) "14.5 psi" else "١٢"
            assertTrue(folder.name + key, String.format(Locale.US, node.textContent, argument).contains(argument))
        }
    }

    @Test fun interfaceLanguageNeverOverridesSavedClockDigitsOrDeviceDefaults() {
        val languages = listOf("en", "it", "es", "fr", "de", "pt", "ru", "tr", "ar", "he", "hi", "zh-CN", "ja", "ko", "sw", "pl", "nl", "id", "fa", "el")
        for (tag in languages) for (format in SpaceCompassNumericFormat.entries) {
            val text = formatSpaceCompassCelestialMoment(stamp, stamp, ZoneId.of("UTC"), SpaceCompassTimeFormat.H24,
                SpaceCompassDateFormat.INTERNATIONAL, Locale.forLanguageTag(tag), Locale.US, format)
            assertEquals(tag + format, "12:30", text)
        }
        assertEquals("١٢:٣٠", formatSpaceCompassCelestialMoment(stamp, stamp, ZoneId.of("UTC"), SpaceCompassTimeFormat.H24,
            SpaceCompassDateFormat.INTERNATIONAL, Locale.US, arabic, SpaceCompassNumericFormat.SYSTEM))
        assertEquals("12:30", formatSpaceCompassCelestialMoment(stamp, stamp, ZoneId.of("UTC"), SpaceCompassTimeFormat.H24,
            SpaceCompassDateFormat.INTERNATIONAL, arabic, arabic, american))
    }

    @Test fun clockChoiceAndLocalizedAmPmSurviveTheNumericOverrideWithoutExtraSeconds() {
        assertEquals("12:30 PM", formatSpaceCompassTimeOnly(stamp, SpaceCompassTimeFormat.H12, Locale.US, utc,
            american, arabic, includeSeconds = false))
        val text = formatSpaceCompassTimeOnly(stamp, SpaceCompassTimeFormat.H12, arabic, utc,
            american, Locale.US, includeSeconds = false)
        assertTrue(text.startsWith("12:30 "))
        assertTrue(text.contains("م"))
        assertFalse(text.contains("45"))
        assertEquals("12:30:45", formatSpaceCompassTimeOnly(stamp, SpaceCompassTimeFormat.H24, arabic, utc, american, Locale.US))
    }

    @Test fun selectedDateOrderAndSelectedDigitsAreIndependentAndYearsNeverGetGrouping() {
        for ((format, expected) in listOf(SpaceCompassDateFormat.INTERNATIONAL to "2026-10-04",
            SpaceCompassDateFormat.EUROPEAN to "04/10/2026", SpaceCompassDateFormat.AMERICAN to "10/04/2026")) {
            assertEquals(expected, formatSpaceCompassDateOnly(stamp, format, arabic, utc, american, arabic))
            val localized = expected.map { if (it in '0'..'9') ('٠'.code + (it - '0')).toChar() else it }.joinToString("")
            assertEquals(localized, formatSpaceCompassDateOnly(stamp, format, Locale.US, utc, SpaceCompassNumericFormat.SYSTEM, arabic))
        }
        assertEquals("2026-10-04 12:30:45", formatSpaceCompassDateTime(stamp, SpaceCompassDateFormat.INTERNATIONAL,
            Locale.US, utc, separator = " ", numeric = SpaceCompassNumericFormat.EUROPEAN))
    }

    @Test fun deviceCalendarAndDstOffsetRemainCorrectWithDifferentInterfaceLanguageAndDigits() {
        val previousDay = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
        assertEquals("12:30 · 10/4/26", formatSpaceCompassCelestialMoment(stamp, previousDay, ZoneId.of("UTC"),
            SpaceCompassTimeFormat.H24, SpaceCompassDateFormat.SYSTEM, arabic, Locale.US, american))
        val first = Instant.parse("2026-10-25T00:30:00Z").toEpochMilli()
        val second = Instant.parse("2026-10-25T01:30:00Z").toEpochMilli()
        assertEquals("02:30 (UTC+02:00)", formatSpaceCompassCelestialMoment(first, first, ZoneId.of("Europe/Rome"),
            SpaceCompassTimeFormat.H24, SpaceCompassDateFormat.INTERNATIONAL, arabic, Locale.US, american))
        assertEquals("02:30 (UTC+01:00)", formatSpaceCompassCelestialMoment(second, first, ZoneId.of("Europe/Rome"),
            SpaceCompassTimeFormat.H24, SpaceCompassDateFormat.INTERNATIONAL, arabic, Locale.US, american))
        assertEquals("T−01:00", formatSpaceCompassPointCountdown(second, first, Locale.US))
    }

    @Test fun integerCountsAndMeasurementsShareDeviceDigitsAndExplicitFormats() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(arabic)
            assertEquals("١٢", formatSpaceCompassNumber(12.0, 0, SpaceCompassNumericFormat.SYSTEM, grouping = false))
            assertEquals("١٢٫٥", formatSpaceCompassNumber(12.5, 1, SpaceCompassNumericFormat.SYSTEM, grouping = false))
            for (format in listOf(american, SpaceCompassNumericFormat.EUROPEAN, SpaceCompassNumericFormat.INTERNATIONAL))
                assertEquals("12", formatSpaceCompassNumber(12.0, 0, format, grouping = false))
        } finally { Locale.setDefault(original) }
    }

    @Test fun nonfiniteNumbersAreUnavailableRatherThanEnglishTechnicalTokens() {
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            for (format in SpaceCompassNumericFormat.entries) assertEquals("—", formatSpaceCompassNumber(bad, 2, format))
    }
}
