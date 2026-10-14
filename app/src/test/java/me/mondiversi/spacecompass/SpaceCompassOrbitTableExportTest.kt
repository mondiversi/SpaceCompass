package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassOrbitTableExportTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.parse("2026-10-12")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val time = start + 12 * 3_600_000L
    private fun point(index: Int) = SpaceCompassSunPathPoint(start + index*3_600_000L,
        SpaceCompassSunPosition(123.45,-20.75))
    private fun path(body: SpaceCompassCelestialBody) = SpaceCompassSunDailyPath(date,zone,
        listOf(point(0),point(24)),(0..23).map(::point),body)
    private fun snapshot(paths: List<SpaceCompassSunDailyPath>, state: SpaceCompassSunDailyPathUiState? = null,
        known: Boolean = false) = SpaceCompassPanoramaSnapshot(time,45.1,8.9,99.0,SpaceCompassSunSkyPhase.AFTERNOON,
        null,paths.map { SpaceCompassPanoramaObject(it.body,it.body.name,it) },SpaceCompassCelestialRemoteData(),
        "",emptyList(),emptyList(),observerPositionKnown=known,selectedPoints=state?.selectedPoints?.toList().orEmpty(),zoneId=zone.id)

    @Test fun columnsAreObjectsAndFirstRowsAreAllTwentyFourPoints() {
        val capture = snapshot(listOf(path(SpaceCompassCelestialBody.SUN),path(SpaceCompassCelestialBody.MOON)))
        val table = captureSpaceCompassOrbitTable(capture)
        assertEquals((0..23).toList(),table.rows.filter { it.kind==SpaceCompassOrbitExportRowKind.POINT }.map { it.index })
        val grid = spaceCompassOrbitExportGrid(table,capture,time,"Local area")
        assertEquals(15,grid.first().size)
        assertTrue(grid.all { it.size==15 })
        assertEquals("SUN — Time",grid[0][1]); assertEquals("MOON — Time",grid[0][8])
        assertEquals("Point 0",grid[5][0]); assertEquals("Point 23",grid.last()[0])
        assertEquals("123.5",grid[5][3]); assertEquals("-20.8",grid[5][4])
    }

    @Test fun captureKeepsSelectionsAndMergedEventsAfterLiveStateChanges() {
        val ordinary = path(SpaceCompassCelestialBody.SUN)
        val event = point(6).copy(coincidentEvents=setOf(SpaceCompassSunPathEvent.SUNRISE))
        val capturedPath = ordinary.copy(markers=ordinary.markers.map { if (it.timeMs==event.timeMs) event else it })
        val state = SpaceCompassSunDailyPathUiState(); state.select(capturedPath,event)
        val capture = snapshot(listOf(capturedPath),state)
        val table = captureSpaceCompassOrbitTable(capture)
        state.clearSelection()
        val selected = table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.POINT && it.index==6 }.cells.values.single()
        assertTrue(selected.selected); assertEquals(event,selected.point)
        assertTrue(table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.EVENT }.cells.values.single().selected)
        val grid = spaceCompassOrbitExportGrid(table,capture,time,"Area")
        assertEquals("* ! Point 6",grid[11][0]); assertEquals("* Selected",grid[11][6])
        assertEquals("Rise",grid[11][5])
    }

    @Test fun currentCellUsesFrozenObservationTimeEvenForFutureScenarios() {
        val future = Instant.parse("2027-08-15T12:34:00Z").toEpochMilli()
        val capture = snapshot(listOf(path(SpaceCompassCelestialBody.SUN)),known=true).copy(timeMs=future)
        val table = captureSpaceCompassOrbitTable(capture)
        val current = table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.CURRENT }.cells.values.single()
        assertEquals(future,current.point.timeMs)
        assertEquals(calculateSpaceCompassSunPosition(future,capture.latitude,capture.longitude,capture.altitude),current.point.position)
        val grid = spaceCompassOrbitExportGrid(table,capture,time,"Area")
        assertTrue(grid[1][1].contains("2026-10-12")); assertTrue(grid[2][1].contains("2027-08-15"))
        assertEquals("T+0h0m",grid.single { it[0]=="! Current" }[2])
    }

    @Test fun satellitePointsKeepTheirTwentyFourOrbitalSamplesInsteadOfCivilHours() {
        val satellite = path(SpaceCompassCelestialBody.ISS).let { base -> base.copy(markers=(0..23).map { index ->
            point(index).copy(timeMs=time+index*240_000L) }) }
        val table = captureSpaceCompassOrbitTable(snapshot(listOf(satellite)))
        assertEquals((0..23).toList(),table.rows.map { it.index })
        assertEquals(time+23*240_000L,table.rows.last().cells.values.single().point.timeMs)
    }

    @Test fun repeatedDstHoursRemainDistinctWithTheirOwnUtcOffset() {
        val day = LocalDate.parse("2026-10-25")
        val first = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val daily = path(SpaceCompassCelestialBody.SUN).copy(date=day,markers=(0..24).map {
            point(0).copy(timeMs=first+it*3_600_000L) })
        val capture = snapshot(listOf(daily)).copy(timeMs=first+12*3_600_000L)
        val table = captureSpaceCompassOrbitTable(capture)
        val repeated = table.rows.filter { it.index==2 }
        assertEquals(2,repeated.size); assertEquals(listOf(0,1),repeated.map { it.occurrence })
        val grid = spaceCompassOrbitExportGrid(table,capture,capture.timeMs,"Area")
        assertTrue(grid.single { it[0]=="Point 2" }[1].endsWith("UTC+02:00)"))
        assertTrue(grid.single { it[0]=="Point 2 (2)" }[1].endsWith("UTC+01:00)"))
    }

    @Test fun springDstMissingHourHasNoInventedPosition() {
        val day=LocalDate.parse("2026-03-29")
        val first=day.atStartOfDay(zone).toInstant().toEpochMilli()
        val daily=path(SpaceCompassCelestialBody.SUN).copy(date=day,markers=(0..22).map {
            point(0).copy(timeMs=first+it*3_600_000L) })
        val capture=snapshot(listOf(daily)).copy(timeMs=first)
        val grid=spaceCompassOrbitExportGrid(captureSpaceCompassOrbitTable(capture),capture,first,"Area")
        assertTrue(grid.single { it[0]=="Point 2" }.drop(1).all { it=="—" })
    }

    @Test fun unavailableDataKeepsTheObjectColumnAndMissingCells() {
        val capture=snapshot(listOf(path(SpaceCompassCelestialBody.SUN))).copy(objects=listOf(
            SpaceCompassPanoramaObject(SpaceCompassCelestialBody.ISS,"ISS",null)))
        val table=captureSpaceCompassOrbitTable(capture)
        assertEquals(24,table.rows.size)
        val grid=spaceCompassOrbitExportGrid(table,capture,time,"Area")
        assertTrue(grid.first()[1].startsWith("ISS"))
        assertTrue(grid.drop(5).all { it.drop(1).all { value -> value=="—" } })
    }

    @Test fun historicalPinnedSatellitePointStaysInTheExport() {
        val original=path(SpaceCompassCelestialBody.ISS)
        val state=SpaceCompassSunDailyPathUiState(); state.select(original,point(1))
        val refreshed=original.copy(markers=listOf(point(12),point(23)))
        val table=captureSpaceCompassOrbitTable(snapshot(listOf(refreshed),state))
        val saved=table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.SELECTED }.cells.values.single()
        assertTrue(saved.selected); assertEquals(point(1),saved.point)
    }

    @Test fun moonIlluminationIsRetainedAlongsideBothAngles() {
        val moon=path(SpaceCompassCelestialBody.MOON)
        val phase=calculateSpaceCompassMoonPhase(time)
        val capture=snapshot(listOf(moon.copy(markers=listOf(point(12).copy(moonPhase=phase)))))
        val grid=spaceCompassOrbitExportGrid(captureSpaceCompassOrbitTable(capture),capture,time,"Area")
        assertEquals(formatSpaceCompassNumber(phase.illuminatedFraction*100,1,SpaceCompassNumericFormat.INTERNATIONAL,grouping=false),
            grid.single { it[0]=="Point 12" }[7])
    }

    @Test fun localizedCsvUsesDecimalCommaAndSemicolonWhileInternationalUsesComma() {
        val capture=snapshot(listOf(path(SpaceCompassCelestialBody.SUN)))
        val table=captureSpaceCompassOrbitTable(capture)
        val italian=capture.copy(formatting=SpaceCompassPanoramaFormatting(locale=Locale.ITALIAN,
            numeric=SpaceCompassNumericFormat.SYSTEM,deviceLocale=Locale.ITALIAN),
            orbitExportVocabulary=capture.orbitExportVocabulary.copy(point="Punto %1\$s"))
        val local=renderSpaceCompassOrbitCsv(table,italian,time,"Area")
        assertTrue(local.startsWith("\uFEFF\"Punto\";")); assertTrue(local.contains("\"123,5\";\"-20,8\""))
        val international=renderSpaceCompassOrbitCsv(table,capture,time,"Area")
        assertTrue(international.startsWith("\uFEFF\"Point\",")); assertTrue(international.contains("\"123.5\",\"-20.8\""))
    }

    @Test fun csvQuotesAndFormulaLikeExternalTextCannotBreakCells() {
        assertEquals("\"He said \"\"hello\"\", here\nnext\"",spaceCompassOrbitCsvCell("He said \"hello\", here\nnext"))
        for (value in listOf("=SUM(A1)","+cmd","-cmd","@SUM(A1)","  =SUM(A1)"))
            assertTrue(spaceCompassOrbitCsvCell(value).startsWith("\"'"))
        assertEquals("\"-20.8\"",spaceCompassOrbitCsvCell("-20.8"))
        assertEquals("\"+20,8\"",spaceCompassOrbitCsvCell("+20,8"))
    }

    @Test fun csvAndTxtKeepFrozenTimeZoneAndProfileWhenSystemSettingsChange() {
        val capture=snapshot(listOf(path(SpaceCompassCelestialBody.SUN)))
        val table=captureSpaceCompassOrbitTable(capture)
        val csv=renderSpaceCompassOrbitCsv(table,capture,time,"Area")
        val txt=renderSpaceCompassOrbitTxt(table,capture,time,"Area")
        val oldLocale=Locale.getDefault(); val oldZone=TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.JAPANESE); TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals(csv,renderSpaceCompassOrbitCsv(table,capture,time,"Area"))
            assertEquals(txt,renderSpaceCompassOrbitTxt(table,capture,time,"Area"))
        } finally { Locale.setDefault(oldLocale); TimeZone.setDefault(oldZone) }
    }

    @Test fun readableTableHasObjectHeadersAndExplicitSelectedKeyFlags() {
        val daily=path(SpaceCompassCelestialBody.SUN)
        val state=SpaceCompassSunDailyPathUiState(); state.select(daily,point(1))
        val capture=snapshot(listOf(daily,path(SpaceCompassCelestialBody.MOON)),state)
        val txt=renderSpaceCompassOrbitTxt(captureSpaceCompassOrbitTable(capture),capture,time,"Area only")
        assertTrue(txt.contains("SUN")); assertTrue(txt.contains("MOON")); assertTrue(txt.contains("* Point 1"))
        assertTrue(txt.contains("Azimuth: 123.5° · Elevation: -20.8°"))
        assertTrue(txt.contains("UTC+02:00")); assertFalse(txt.contains("45.1")); assertFalse(txt.contains("8.9"))
    }

    @Test fun mismatchedCaptureCannotBeFormattedUsingALaterObservation() {
        val capture=snapshot(listOf(path(SpaceCompassCelestialBody.SUN)))
        val table=captureSpaceCompassOrbitTable(capture)
        assertThrows(IllegalArgumentException::class.java) {
            renderSpaceCompassOrbitCsv(table,capture.copy(timeMs=time+60_000L),time,"Area") }
    }

    @Test fun exportTypesKeepMultipleChoicesAndStartEmpty() {
        assertEquals(emptySet<SpaceCompassCaptureExportType>(),SpaceCompassCaptureExportType.fromStored(null))
        assertEquals(emptySet<SpaceCompassCaptureExportType>(),SpaceCompassCaptureExportType.fromStored(emptySet()))
        assertEquals(setOf(SpaceCompassCaptureExportType.CSV,SpaceCompassCaptureExportType.TXT),
            SpaceCompassCaptureExportType.fromStored(setOf("csv","txt","unknown")))
    }

    @Test fun fileNamesNeverOverwriteExistingDocumentsAndHandleNoExtension() {
        assertEquals("capture (2).csv",spaceCompassCaptureUniqueName("capture.csv",setOf("CAPTURE.CSV","capture (1).csv")))
        assertEquals("capture (1)",spaceCompassCaptureUniqueName("capture",setOf("capture")))
        assertEquals("capture.csv",spaceCompassCaptureUniqueName("capture.csv",emptySet()))
    }
    @Test fun specialCurrentAndPinnedPointsAreInterleavedByTheirActualTime() {
        val ordinary=path(SpaceCompassCelestialBody.SUN)
        val rise=point(6).copy(timeMs=point(6).timeMs+30*60_000L,event=SpaceCompassSunPathEvent.SUNRISE)
        val culmination=point(12).copy(timeMs=point(12).timeMs+20*60_000L,event=SpaceCompassSunPathEvent.CULMINATION)
        val set=point(18).copy(timeMs=point(18).timeMs-30*60_000L,event=SpaceCompassSunPathEvent.SUNSET)
        val minimum=point(0).copy(timeMs=start+45*60_000L,event=SpaceCompassSunPathEvent.MINIMUM)
        val daily=ordinary.copy(markers=ordinary.markers+listOf(set,minimum,culmination,rise))
        val pin=point(2).copy(timeMs=point(2).timeMs+15*60_000L)
        val state=SpaceCompassSunDailyPathUiState();state.select(daily,pin)
        val capture=snapshot(listOf(daily),state).copy(timeMs=time+40*60_000L)
        val table=captureSpaceCompassOrbitTable(capture,mapOf(daily.body to point(12).position))
        val times=table.rows.map { it.cells.values.single().point.timeMs }
        assertEquals(times.sorted(),times)
        fun rowAt(moment: Long)=table.rows.indexOfFirst { it.cells.values.single().point.timeMs==moment }
        for ((before,moment,after) in listOf(
            Triple(point(0).timeMs,minimum.timeMs,point(1).timeMs),
            Triple(point(2).timeMs,pin.timeMs,point(3).timeMs),
            Triple(point(6).timeMs,rise.timeMs,point(7).timeMs),
            Triple(point(12).timeMs,culmination.timeMs,capture.timeMs),
            Triple(culmination.timeMs,capture.timeMs,point(13).timeMs),
            Triple(point(17).timeMs,set.timeMs,point(18).timeMs))) {
            assertTrue(rowAt(before)<rowAt(moment));assertTrue(rowAt(moment)<rowAt(after))
        }
        assertTrue(table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.SELECTED }.cells.values.single().selected)
        val grid=spaceCompassOrbitExportGrid(table,capture,time,"Area")
        assertTrue(grid.indexOfFirst { it[0]=="! Rise" } in
            (grid.indexOfFirst { it[0]=="Point 6" }+1) until grid.indexOfFirst { it[0]=="Point 7" })
        val txt=renderSpaceCompassOrbitTxt(table,capture,time,"Area")
        assertTrue(txt.indexOf("Point 6")<txt.indexOf("! Rise"))
        assertTrue(txt.indexOf("! Rise")<txt.indexOf("Point 7"))
    }

    @Test fun differentObjectsRiseTimesStayInTheirOwnChronologicalRows() {
        val sun=path(SpaceCompassCelestialBody.SUN)
        val moon=path(SpaceCompassCelestialBody.MOON)
        val sunRise=point(8).copy(timeMs=point(8).timeMs+30*60_000L,event=SpaceCompassSunPathEvent.SUNRISE)
        val moonRise=point(5).copy(timeMs=point(5).timeMs+30*60_000L,event=SpaceCompassSunPathEvent.SUNRISE)
        val capture=snapshot(listOf(sun.copy(markers=sun.markers+sunRise),moon.copy(markers=moon.markers+moonRise)))
        val table=captureSpaceCompassOrbitTable(capture)
        val riseRows=table.rows.filter { it.kind==SpaceCompassOrbitExportRowKind.EVENT }
        assertEquals(listOf(moonRise.timeMs,sunRise.timeMs),riseRows.map { it.cells.values.single().point.timeMs })
        assertEquals(setOf(moon.body),riseRows.first().cells.keys)
        assertEquals(setOf(sun.body),riseRows.last().cells.keys)
        assertTrue(table.rows.all { row -> row.cells.values.map { it.point.timeMs }.distinct().size<=1 })
        for (body in table.bodies) {
            val times=table.rows.mapNotNull { it.cells[body]?.point?.timeMs }
            assertEquals(times.sorted(),times)
        }
        val grid=spaceCompassOrbitExportGrid(table,capture,time,"Area")
        assertTrue(grid.all { it.size==15 })
        assertEquals(24,table.rows.count { it.kind==SpaceCompassOrbitExportRowKind.POINT })
    }

    @Test fun satelliteSamplesAndCivilHourPointsEachKeepChronologicalColumns() {
        val sun=path(SpaceCompassCelestialBody.SUN)
        val satellite=path(SpaceCompassCelestialBody.ISS).copy(markers=(0..23).map { index ->
            point(index).copy(timeMs=start+11*3_600_000L+30*60_000L+index*300_000L) })
        val table=captureSpaceCompassOrbitTable(snapshot(listOf(sun,satellite)))
        for (body in table.bodies) {
            val points=table.rows.filter { it.kind==SpaceCompassOrbitExportRowKind.POINT && body in it.cells }
            assertEquals((0..23).toList(),points.map { it.index })
            val times=points.map { it.cells.getValue(body).point.timeMs }
            assertEquals(times.sorted(),times)
        }
        assertTrue(table.rows.all { row -> row.cells.values.map { it.point.timeMs }.distinct().size<=1 })
        val times=table.rows.map { it.cells.values.single().point.timeMs }
        assertEquals(times.sorted(),times)
    }

    @Test fun unavailableSampleSlotsFollowKnownTimesWithoutInventedCoordinates() {
        val partial=path(SpaceCompassCelestialBody.ISS).copy(markers=listOf(point(12),point(13)))
        val capture=snapshot(listOf(partial))
        val table=captureSpaceCompassOrbitTable(capture)
        assertEquals(24,table.rows.size)
        assertEquals(listOf(point(12).timeMs,point(13).timeMs),table.rows.take(2).map { it.cells.values.single().point.timeMs })
        assertTrue(table.rows.drop(2).all { it.cells.isEmpty() })
        assertTrue(spaceCompassOrbitExportGrid(table,capture,time,"Area").drop(7).all { it.drop(1).all { value -> value=="—" } })
    }

    @Test fun nextSatellitePassIsInterleavedWhenItFallsWithinTheDisplayedDay() {
        val pass=point(14).copy(timeMs=point(14).timeMs+30*60_000L,event=SpaceCompassSunPathEvent.CULMINATION)
        val daily=path(SpaceCompassCelestialBody.ISS).copy(issPass=listOf(pass))
        val capture=spaceCompassSingleOrbitExportSnapshot(daily,point(12),emptyList(),time)
        val table=spaceCompassSingleOrbitExportTable(capture,daily,point(12))
        val index=table.rows.indexOfFirst { it.kind==SpaceCompassOrbitExportRowKind.PASS }
        assertEquals(point(14),table.rows[index-1].cells.values.single().point)
        assertEquals(point(15),table.rows[index+1].cells.values.single().point)
        assertEquals(pass,table.rows[index].cells.values.single().point)
    }

}
