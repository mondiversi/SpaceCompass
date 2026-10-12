package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPageDataExportTest {
    private val zone=ZoneId.of("Europe/Rome")
    private val day=LocalDate.parse("2026-10-12")
    private val start=day.atStartOfDay(zone).toInstant().toEpochMilli()
    private val time=start+12*3_600_000L
    private val strings=SpaceCompassTextResources { id,args -> "field-$id"+args.joinToString() }
    private fun profile(units: SpaceCompassUnits=SpaceCompassUnits(pressure=SpaceCompassPressureUnit.PASCAL)) =
        SpaceCompassDataExportProfile(spaceCompassPanoramaInternationalFormatting,units,strings)
    private fun sections(body: SpaceCompassCelestialBody,units: SpaceCompassUnits=SpaceCompassUnits()) =
        spaceCompassCelestialDetailSections(body,spaceCompassCelestialFacts(body),time,SpaceCompassCelestialRemoteData(),
            384400.0,null,null,null,strings,spaceCompassPanoramaInternationalFormatting,units)
    private fun point(hour: Int) = SpaceCompassSunPathPoint(start+hour*3_600_000L,SpaceCompassSunPosition(123.0,20.0))
    private fun path(body: SpaceCompassCelestialBody) = SpaceCompassSunDailyPath(day,zone,
        listOf(point(0),point(24)),(0..23).map(::point),body)
    private fun document() = SpaceCompassExportDocument("Observatory · Moon",listOf("Observation time" to "Frozen time"),
        listOf(SpaceCompassCelestialDetailSection("Identity","identity",listOf(
            SpaceCompassCelestialDetailRow("Name","Moon","Natural satellite")),"identity"),
            SpaceCompassCelestialDetailSection("Physical","mass",listOf(SpaceCompassCelestialDetailRow("Mass","7.342 × 10²² kg")),
                "physical","Reference estimate")),listOf("Composite model, not a live photograph"),
        listOf("Section","Parameter","Value","Notes"),"Notes",false)

    @Test fun dailyExportContainsOnlyTheOpenBodyAndItsSelectedPoints() {
        val moon=path(SpaceCompassCelestialBody.MOON);val sun=path(SpaceCompassCelestialBody.SUN)
        val state=SpaceCompassSunDailyPathUiState();state.select(moon,point(1));state.select(sun,point(2))
        val capture=spaceCompassSingleOrbitExportSnapshot(moon,point(12),state.selectedPoints,time)
        val table=captureSpaceCompassOrbitTable(capture,mapOf(moon.body to point(12).position))
        assertEquals(listOf(moon.body),table.bodies)
        assertEquals((0..23).toList(),table.rows.filter { it.kind==SpaceCompassOrbitExportRowKind.POINT }.map { it.index })
        assertTrue(table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.POINT && it.index==1 }.cells.getValue(moon.body).selected)
        assertFalse(table.rows.any { sun.body in it.cells })
        assertEquals(time,table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.CURRENT }.cells.getValue(moon.body).point.timeMs)
    }

    @Test fun dailyDataFreezesTheCurrentPositionWithoutRecalculatingAtZeroGps() {
        val moon=path(SpaceCompassCelestialBody.MOON)
        val position=SpaceCompassSunPosition(220.5,-30.25)
        val current=point(12).copy(position=position)
        val capture=spaceCompassSingleOrbitExportSnapshot(moon,current,emptyList(),time+600_000L)
        assertFalse(capture.observerPositionKnown)
        val table=captureSpaceCompassOrbitTable(capture,mapOf(moon.body to position))
        assertEquals(current.timeMs,table.observationTimeMs)
        assertEquals(position,table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.CURRENT }.cells.values.single().point.position)
    }

    @Test fun eventAndSelectedFlagsRemainAfterSourceSelectionsAreCleared() {
        val moon=path(SpaceCompassCelestialBody.MOON)
        val event=point(6).copy(event=SpaceCompassSunPathEvent.SUNRISE)
        val daily=moon.copy(markers=moon.markers+event)
        val state=SpaceCompassSunDailyPathUiState();state.select(daily,event)
        val capture=spaceCompassSingleOrbitExportSnapshot(daily,point(12),state.selectedPoints,time)
        val table=captureSpaceCompassOrbitTable(capture,mapOf(moon.body to point(12).position))
        state.clearSelection()
        val csv=renderSpaceCompassOrbitCsv(table,capture,time,"Moon")
        assertTrue(csv.contains("* ! Rise"));assertTrue(csv.contains("* Selected"))
    }

    @Test fun observatoryCsvContainsIdentityPhysicalDataAndReferenceNotes() {
        val grid=spaceCompassExportDocumentGrid(document())
        assertTrue(grid.all { it.size==4 })
        assertTrue(grid.any { it[0]=="Identity" && it[1]=="Name" && it[2]=="Moon" && it[3]=="Natural satellite" })
        assertTrue(grid.any { it[1]=="Mass" && it[3]=="Reference estimate" })
        assertTrue(grid.any { it[0]=="Notes" && it[3].contains("not a live photograph") })
        assertTrue(renderSpaceCompassExportDocumentCsv(document()).startsWith("\uFEFF\"Section\","))
    }

    @Test fun observatoryReadableTableGroupsSectionsAndKeepsScientificDesignation() {
        val txt=renderSpaceCompassExportDocumentTxt(document())
        assertTrue(txt.contains("Identity\n"));assertTrue(txt.contains("Physical\n"))
        assertTrue(txt.contains("Name | Moon"));assertTrue(txt.contains("Natural satellite"))
        assertTrue(txt.contains("Reference estimate"));assertTrue(txt.contains("Frozen time"))
    }

    @Test fun readableTableKeepsMultilineDatesAndNotesInTheValueColumn() {
        val doc=document().copy(sections=listOf(SpaceCompassCelestialDetailSection("Spatial context","coordinates",
            listOf(SpaceCompassCelestialDetailRow("Calculation time","2026-10-12\n14:44:13 UTC","J2000\nGeocentric")),"spatial")))
        val txt=renderSpaceCompassExportDocumentTxt(doc)
        val blank=" ".repeat("Calculation time".length)+" | "
        assertTrue(txt.contains("Calculation time | 2026-10-12\n"+blank+"14:44:13 UTC\n"))
        assertTrue(txt.contains(blank+"J2000\n"+blank+"Geocentric\n"))
        assertTrue(renderSpaceCompassExportDocumentCsv(doc).contains("2026-10-12\n14:44:13 UTC"))
    }

    @Test fun localizedCsvUsesSemicolonAndEscapesExternalNotes() {
        val document=document().copy(decimalComma=true,notes=listOf("Quote \"this\", then\nnext line","=BAD(A1)"))
        val csv=renderSpaceCompassExportDocumentCsv(document)
        assertTrue(csv.startsWith("\uFEFF\"Section\";"));assertTrue(csv.contains("Quote \"\"this\"\", then\nnext line"))
        assertTrue(csv.contains("\"'=BAD(A1)\""))
    }

    @Test fun sharedSectionsIncludeIdentityPhysicalAtmosphereOrbitAndSpatialData() {
        val data=sections(SpaceCompassCelestialBody.MOON)
        assertEquals(setOf("celestial-view-information-island","celestial-view-physical-island","celestial-view-stellar-island",
            "celestial-view-atmosphere-island","celestial-view-orbit-island","celestial-view-spatial-island"),data.map { it.tag }.toSet())
        assertTrue(data.flatMap { it.rows }.any { it.label=="field-${spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.MOON)!!.temperature.kind.labelResource}" })
    }

    @Test fun metricAndImperialExportsUseTheSameConversionsAsTheVisibleDetails() {
        val body=SpaceCompassCelestialBody.MOON;val facts=spaceCompassCelestialFacts(body)
        val metric=sections(body,SpaceCompassUnits(pressure=SpaceCompassPressureUnit.PASCAL)).flatMap { it.rows }
        val imperial=sections(body,SpaceCompassUnits(feet=true,pounds=true,fahrenheit=true,pressure=SpaceCompassPressureUnit.PSI)).flatMap { it.rows }
        assertEquals(formatSpaceCompassCelestialMass(body,facts,SpaceCompassNumericFormat.INTERNATIONAL,false),
            metric.single { it.label=="field-${R.string.celestial_view_mass}" }.value)
        assertEquals(formatSpaceCompassCelestialMass(body,facts,SpaceCompassNumericFormat.INTERNATIONAL,true),
            imperial.single { it.label=="field-${R.string.celestial_view_mass}" }.value)
        assertTrue(metric.single { it.tag=="celestial-view-gravity" }.value.contains("m/s²"))
        assertTrue(imperial.single { it.tag=="celestial-view-gravity" }.value.contains("ft/s²"))
    }

    @Test fun everyCatalogObjectCanProduceCompleteTypedSectionsWithoutFabricatingMissingValues() {
        for (body in SpaceCompassCelestialBody.entries) {
            val data=sections(body)
            assertTrue(body.name,data.any { it.tag=="celestial-view-information-island" })
            assertTrue(data.flatMap { it.rows }.all { it.value.isNotBlank() })
        }
    }

    @Test fun imageExportChoicesAreIndependentAndKeepTheirStablePreferenceKeys() {
        val current=SpaceCompassCaptureExportType.CURRENT_IMAGE
        val rotation=SpaceCompassCaptureExportType.ROTATION_IMAGE
        assertNotEquals(current.key,rotation.key)
        assertEquals(setOf(current,rotation),SpaceCompassCaptureExportType.fromStored(setOf(current.key,rotation.key)))
        assertEquals(setOf(current),SpaceCompassCaptureExportType.fromStored(setOf(current.key)))
        assertEquals("image/jpeg",current.mimeType);assertEquals("image/jpeg",rotation.mimeType)
    }

    @Test fun rotationGeometryAndZoomAreCopiedBeforeLaterManipulation() {
        val rotation=SpaceCompassCelestialRotation(yaw=30.0).drag(12.0,20.0,500.0)
        val viewport=SpaceCompassCelestialViewportState().transform(2.0)
        val images=SpaceCompassObservatoryImages(SpaceCompassCelestialBody.MOON,rotation.geometry(),rotation.geometry(),
            viewport.copy(),androidx.compose.ui.unit.IntSize(600,400))
        val later=rotation.drag(80.0,40.0,500.0)
        assertNotEquals(later.geometry(),images.rotation);assertEquals(rotation.geometry(),images.rotation)
        assertEquals(2.0,images.viewport.zoom,0.0)
    }

    @Test fun frozenTextDoesNotChangeWhenSystemLocaleOrClockZoneChanges() {
        val oldLocale=Locale.getDefault();val oldZone=TimeZone.getDefault()
        val data=document();val csv=renderSpaceCompassExportDocumentCsv(data)
        try {
            Locale.setDefault(Locale.JAPANESE);TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            assertEquals(csv,renderSpaceCompassExportDocumentCsv(data))
        } finally { Locale.setDefault(oldLocale);TimeZone.setDefault(oldZone) }
    }

    @Test fun satellitePageExportKeepsTheNextPassSeparateFromCurrentOrbitalPoints() {
        val daily=path(SpaceCompassCelestialBody.ISS).copy(issPass=listOf(point(26).copy(event=SpaceCompassSunPathEvent.CULMINATION)))
        val snapshot=spaceCompassSingleOrbitExportSnapshot(daily,point(12),emptyList(),time)
        val table=spaceCompassSingleOrbitExportTable(snapshot,daily,point(12))
        assertEquals(24,table.rows.count { it.kind==SpaceCompassOrbitExportRowKind.POINT })
        val pass=table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.PASS }
        assertEquals(daily.issPass.single(),pass.cells.getValue(daily.body).point)
        assertTrue(renderSpaceCompassOrbitCsv(table,snapshot,time,"ISS").contains("! Next pass · Culmination"))
    }
}
