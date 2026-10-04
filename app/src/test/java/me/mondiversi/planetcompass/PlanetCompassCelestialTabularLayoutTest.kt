package me.mondiversi.planetcompass

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialTabularLayoutTest {
    private fun source(name: String) = File("src/main/java/me/mondiversi/planetcompass/$name").readText()

    @Test fun landscapePlacesTheToolbarInsideOnlyTheSkyColumnAndKeepsDetailsFullHeight() {
        val screen = source("PlanetCompassSunFinderScreen.kt")
        val split = screen.substringAfter("testTag(\"celestial-landscape-layout\")")
            .substringBefore("} else Column(Modifier.fillMaxSize())")
        assertTrue(split.contains("Column(Modifier.weight(1f).fillMaxHeight())"))
        assertTrue(split.contains("toolbar()"))
        assertTrue(split.contains("pointing(true)"))
        assertTrue(split.contains("details(Modifier.weight(1f).fillMaxHeight().padding(10.dp)"))
        assertFalse(screen.contains("viewportWidth * 0.43f"))
        assertTrue(screen.contains("viewportWidth / 2 - 20.dp >= minimumPanelWidth"))
        assertTrue(screen.substringAfter("} else Column(Modifier.fillMaxSize())").contains("toolbar()"))
    }

    @Test fun viewerHasNoTopGapInEitherLayoutWithoutChangingSideBottomOrContentPadding() {
        val viewer = source("PlanetCompassCelestialViewerScreen.kt")
        assertTrue(viewer.contains(".padding(start = 10.dp, end = 10.dp, bottom = 6.dp)"))
        assertTrue(viewer.contains("Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp))"))
        val placement = viewer.substringAfter("BoxWithConstraints(Modifier.fillMaxSize()")
        assertFalse(placement.contains(".padding(top"))
        assertFalse(placement.contains("vertical = 6.dp"))
        val portrait = viewer.substringAfter("} else Column(Modifier.fillMaxSize(),")
            .substringBefore("model(Modifier.weight")
        assertFalse(portrait.contains(".padding(top"))
        assertTrue(viewer.contains(".padding(12.dp).testTag(\"celestial-view-information\")"))
    }

    @Test fun everyBodyUsesTwoObserverDistanceUnitsOnOneLineWithoutRepeatingItsName() {
        for (body in PlanetCompassCelestialBody.entries) for (numeric in PlanetCompassNumericFormat.entries) {
            val result = formatPlanetCompassCelestialTableDistance(body, PLANET_COMPASS_AU_KM * 1.234567, numeric)!!
            assertFalse(result.contains('\n'))
            val rows = result.split(" · ")
            assertEquals(2, rows.size)
            assertTrue(rows[0].endsWith(" Mkm"))
            assertTrue(rows[1].endsWith("${formatPlanetCompassNumber(1.234567, 4, numeric, minimumDigits = 0)} AU"))
            assertTrue(rows.none { it.contains(body.name) })
        }
    }

    @Test fun smallAndLiveDistancesRetainEnoughPrecisionForMeaningfulUpdates() {
        assertEquals("0.0008 Mkm · < 0.0001 AU", formatPlanetCompassCelestialTableDistance(
            PlanetCompassCelestialBody.ISS, 800.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertNotEquals(formatPlanetCompassCelestialTableDistance(PlanetCompassCelestialBody.ISS, 800.0, PlanetCompassNumericFormat.INTERNATIONAL),
            formatPlanetCompassCelestialTableDistance(PlanetCompassCelestialBody.ISS, 820.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("0.3844 Mkm · 0.0026 AU", formatPlanetCompassCelestialTableDistance(
            PlanetCompassCelestialBody.MOON, 384_400.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("149,6 Mkm · 1 AU", formatPlanetCompassCelestialTableDistance(
            PlanetCompassCelestialBody.SUN, PLANET_COMPASS_AU_KM, PlanetCompassNumericFormat.EUROPEAN))
        assertNotEquals(formatPlanetCompassCelestialTableDistance(PlanetCompassCelestialBody.VOYAGER_1, 25e9, PlanetCompassNumericFormat.INTERNATIONAL),
            formatPlanetCompassCelestialTableDistance(PlanetCompassCelestialBody.VOYAGER_1, 25e9 + 34, PlanetCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun invalidRangesStayUnknownAndPositiveTinyRangesNeverAppearAsZero() {
        for (body in PlanetCompassCelestialBody.entries) {
            for (km in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
                assertNull(formatPlanetCompassCelestialTableDistance(body, km, PlanetCompassNumericFormat.INTERNATIONAL))
            assertEquals("0 Mkm · 0 AU", formatPlanetCompassCelestialTableDistance(body, 0.0, PlanetCompassNumericFormat.INTERNATIONAL))
            val tiny = formatPlanetCompassCelestialTableDistance(body, 0.001, PlanetCompassNumericFormat.INTERNATIONAL)!!
            assertTrue(tiny.startsWith("< "))
            assertTrue(tiny.endsWith("< 0.0001 AU"))
        }
    }

    @Test fun allTwentyLanguagesProvideTheTwoNeutralTableLabelsAndCompatiblePlaceholders() {
        val files = File("src/main/res").listFiles()!!.map { File(it, "celestial_layout.xml") }.filter { it.isFile }
        assertEquals(20, files.size)
        for (file in files) {
            val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            val values = (0 until strings.length).associate { index ->
                val node = strings.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent
            }
            for (key in listOf("celestial_table_distance", "celestial_table_speed")) {
                val template = values.getValue(key).replace("%1\$s", PLANET_COMPASS_SUN_DATA_MARKER)
                val row = planetCompassSunOptionalDataRow(template, "1.2346 AU", key)
                assertTrue(file.path, row.label.isNotBlank())
                assertEquals("1.2346 AU", row.value)
                assertEquals("—", planetCompassSunOptionalDataRow(template, null, key).value)
            }
        }
    }

    @Test fun phonePointingReturnsToTheEnvironmentTableWithoutBeingDuplicatedInTheSky() {
        val screen = source("PlanetCompassSunFinderScreen.kt")
        val orientation = screen.substringAfter("val orientationRows = listOf(").substringBefore("val speedTemplate")
        assertTrue(orientation.contains("sun-data-heading"))
        assertTrue(orientation.contains("sun-data-tilt"))
        assertFalse(orientation.contains("celestial-distance"))
        val body = screen.substringAfter("val rows = if (!hasActiveBody) emptyList() else listOf(").substringBefore("val locationRows")
        for (tag in listOf("celestial-distance", "celestial-speed", "sun-data-azimuth", "sun-data-elevation"))
            assertTrue(body.contains(tag))
        assertFalse(body.contains("sun-data-heading") || body.contains("sun-data-tilt"))
        assertTrue(screen.contains("orientationRows = orientationRows"))
        assertTrue(source("PlanetCompassSunFinderDataPanel.kt").contains("PlanetCompassSunFinderDataTable(orientationRows + locationRows"))
        assertFalse(screen.contains("PlanetCompassSunFinderOrientationOverlay"))
        assertFalse(screen.contains("PlanetCompassCelestialDistance("))
        assertFalse(screen.contains("orientationCoordinates"))
        assertTrue(source("PlanetCompassCelestialOffscreenMarker.kt").contains("val excluded = contextExclusions +"))
    }

    @Test fun bothDataIslandsUseOneSurfaceStyleWithARealGapAndAScrollFallback() {
        val panel = source("PlanetCompassSunFinderDataPanel.kt")
        assertTrue(source("PlanetCompassCelestialBodyDataIsland.kt").contains("sun-finder-body-data"))
        assertTrue(panel.contains("sun-finder-environment-data"))
        assertTrue(panel.contains("verticalArrangement = Arrangement.spacedBy(8.dp)"))
        assertTrue(panel.contains(".verticalScroll(scroll)"))
        assertTrue(panel.contains("textAlign = TextAlign.End"))
        assertTrue(panel.contains("fontFeatureSettings = \"tnum\""))
        assertFalse(panel.contains("divideAfterPointing"))
        assertTrue(source("PlanetCompassSunDailyPathLayer.kt").contains("PlanetCompassCelestialTelescopeIcon(Modifier.size(22.dp))"))
    }

    @Test fun environmentValuesAreCenteredWithDetailsBelowAltitudeAndWeatherInTheDialogOnly() {
        val readouts = source("PlanetCompassCelestialCompactReadouts.kt")
        assertTrue(readouts.contains("Arrangement.spacedBy(4.dp)"))
        assertTrue(readouts.contains("text = AnnotatedString(row.value)"))
        assertTrue(readouts.contains("contentDescription = row.announcement"))
        assertTrue(readouts.contains("celestial-body-angles"))
        assertTrue(source("PlanetCompassSunFinderDataPanel.kt").contains("Alignment.CenterVertically"))
        val panel = source("PlanetCompassSunFinderDataPanel.kt")
        assertTrue(panel.contains("sun-finder-environment-values"))
        val environment = panel.substringBefore("if (info) PlanetCompassSunFinderModelInfo")
        assertFalse(environment.contains("Text(weatherText"))
        assertTrue(panel.contains("Text(weatherText, Modifier.testTag(\"celestial-environment-weather\"))"))
        assertTrue(environment.indexOf("testTag(\"sun-finder-location-data\")") <
            environment.indexOf("testTag(\"sun-finder-info\")"))
        assertTrue(environment.contains("Modifier.align(Alignment.BottomEnd).testTag(\"sun-finder-info\")"))
        assertTrue(environment.contains(".defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)"))
        assertTrue(environment.contains("contentAlignment = Alignment.BottomEnd"))
        assertTrue(environment.contains(".padding(horizontal = 8.dp, vertical = 2.dp)"))
        assertTrue(environment.contains("style.lineHeight.toDp() } + 4.dp"))
        assertTrue(environment.contains("testTag(\"sun-finder-compass-column\").padding(end = 8.dp)"))
        assertFalse(panel.contains("Box(Modifier.height(48.dp)"))
        assertTrue(panel.contains("sun-finder-compass-column"))
        assertTrue(panel.contains("R.string.celestial_details_compact"))
        assertTrue(panel.contains("textDecoration = TextDecoration.Underline"))
        assertTrue(panel.contains("val compassSize = if (compact) 68.dp else 80.dp"))
        assertFalse(panel.contains("PlanetCompassSensorInfoButton"))
        assertFalse(panel.contains("R.string.sun_finder_safety"))
        assertTrue(panel.contains("R.string.celestial_environment_info_title"))
        assertFalse(panel.contains("R.string.sun_finder_title"))
    }

    @Test fun bodyTitleGroupsRealArrowsAndActionsWithoutEmptyFutureSlots() {
        val island = source("PlanetCompassCelestialBodyDataIsland.kt")
        assertTrue(island.contains("celestial-body-island-title"))
        assertTrue(island.contains("celestial-body-header"))
        assertTrue(island.contains("textAlign = TextAlign.Center"))
        assertFalse(island.contains("clipOp") || island.contains(".offset("))
        assertTrue(island.contains("LayoutDirection.Rtl"))
        assertEquals(2, Regex("if \\(navigation != null\\) IconButton").findAll(island).count())
        assertFalse(island.contains("else Spacer(Modifier.width(48.dp))"))
        assertTrue(island.contains("actions()"))
        assertTrue(source("PlanetCompassSunFinderDataPanel.kt").contains("bodyNavigation: PlanetCompassCelestialBodyNavigation? = null"))
    }

    @Test fun environmentInfoTitleActionAndExplanationAreAvailableInEveryLanguage() {
        val files = File("src/main/res").listFiles()!!.map { File(it, "celestial_layout.xml") }.filter { it.isFile }
        assertEquals(20, files.size)
        for (file in files) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            val values = (0 until nodes.length).associate { index ->
                val node = nodes.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent
            }
            for (key in listOf("celestial_environment_info_title", "celestial_environment_info_action",
                "celestial_environment_orientation_note", "celestial_details_compact"))
                assertTrue(file.path, values.getValue(key).isNotBlank())
            for (key in listOf("celestial_altitude", "celestial_gps_coordinates")) {
                val row = planetCompassSunDataRow(values.getValue(key).replace("%1\$s", PLANET_COMPASS_SUN_DATA_MARKER), "112 m", key)
                assertEquals("112 m", row.value)
                assertTrue(row.label.isNotBlank())
            }
            val accuracy = values.getValue("celestial_gps_coordinate_accuracy").replace("%1\$s", PLANET_COMPASS_SUN_DATA_MARKER)
            assertEquals("±10 m", planetCompassSunOptionalDataRow(accuracy, "10 m", "accuracy").value)
            assertEquals("—", planetCompassSunOptionalDataRow(accuracy, null, "accuracy").value)
        }
    }

    @Test fun planetCompassIsOneUntranslatedBrandNameInEveryLanguage() {
        val files = File("src/main/res").listFiles()!!.map { File(it, "celestial_layout.xml") }.filter { it.isFile }
        assertEquals(20, files.size)
        for (file in files) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            val titles = (0 until nodes.length).map { nodes.item(it) }
                .filter { it.attributes.getNamedItem("name").nodeValue == "celestial_orbits_title" }
            if (file.parentFile?.name == "values") {
                assertEquals(1, titles.size)
                assertEquals("Planet Compass", titles.single().textContent)
                assertEquals("false", titles.single().attributes.getNamedItem("translatable").nodeValue)
            } else assertTrue("${file.path}: uses the shared brand name", titles.isEmpty())
        }
    }

    @Test fun coordinatesAndHorizontalAccuracyAreInDetailsOnlyBeforeAltitudeAndWeather() {
        val screen = source("PlanetCompassSunFinderScreen.kt")
        val compact = screen.substringAfter("val locationRows = listOf(").substringBefore("val locationInfoRows")
        assertFalse(compact.contains("celestial_gps_coordinates"))
        val info = screen.substringAfter("val locationInfoRows = listOf(").substringBefore("val weatherText")
        assertTrue(info.indexOf("sun-info-coordinates") < info.indexOf("sun-info-accuracy"))
        assertTrue(info.indexOf("sun-info-accuracy") < info.indexOf("sun-info-altitude"))
        assertTrue(info.contains("it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy >= 0f"))
        assertTrue(info.contains("formatPlanetCompassCelestialGpsCoordinates(fix?.latitude, fix?.longitude, numeric)"))
        assertTrue(screen.contains("formatPlanetCompassCelestialAltitude(altitude"))
        assertTrue(screen.contains("takeIf { it.hasVerticalAccuracy() }"))
        assertTrue(compact.contains("R.string.celestial_altitude"))
        assertTrue(compact.contains("height, \"sun-data-altitude\""))
        assertTrue(screen.contains("locationInfoRows = locationInfoRows"))
        assertTrue(screen.contains("sun-info-accuracy"))
        val panel = source("PlanetCompassSunFinderDataPanel.kt")
        assertTrue(panel.contains("PlanetCompassSunFinderDataTable(rows - angles.toSet()"))
        assertTrue(panel.contains("angleRows = angles"))
        val dialog = panel.substringAfter("private fun PlanetCompassSunFinderModelInfo")
        assertTrue(dialog.indexOf("PlanetCompassSunFinderDataTable(locationInfoRows") < dialog.indexOf("Text(weatherText"))
        assertTrue(dialog.indexOf("Text(weatherText") < dialog.indexOf("celestial_environment_orientation_note"))
        assertFalse(panel.contains("rows.filter { it.tag == \"celestial-distance\" }"))
        assertTrue(screen.contains("PlanetCompassMenuTitle(stringResource(R.string.celestial_orbits_title)"))
        assertTrue(source("PlanetCompassCelestialControls.kt").contains("PlanetCompassCelestialCatalogIcon"))
        assertFalse(source("PlanetCompassCelestialControls.kt").contains("PlanetCompassDisclosureChevron"))
        assertFalse(source("PlanetCompassSunFinderSky.kt").contains("pathTint = if (overlays.isEmpty())"))
    }

    @Test fun angleAndScalarRowsShareExactlyTheSameVerticalPaddingWithoutAnExtraGap() {
        val panel = source("PlanetCompassSunFinderDataPanel.kt")
        val body = panel.substringAfter("val angles = rows.filter").substringBefore("PlanetCompassSunFinderDataIsland(primaryText")
        assertFalse(body.contains("Arrangement.spacedBy"))
        assertTrue(body.contains("angleRows = angles"))
        assertTrue(panel.contains("Row(Modifier.fillMaxWidth().padding(vertical = if (compact) 0.dp else 1.dp)"))
        assertTrue(panel.contains("PlanetCompassCelestialInlineAngles(angleRows, style, primaryText, secondaryText,\n                Modifier.padding(vertical = if (compact) 0.dp else 1.dp))"))
    }

    @Test fun pairedAnglesAndTighterHeaderKeepTableAlignmentAndTouchTargets() {
        val readouts = source("PlanetCompassCelestialCompactReadouts.kt")
        assertTrue(readouts.contains("rows.joinToString(\" – \") { it.label }"))
        assertTrue(readouts.contains("if (index > 0) Text(\"–\""))
        assertFalse(readouts.contains("Text(\"·\""))
        assertTrue(readouts.contains("Modifier.testTag(row.tag).clearAndSetSemantics"))
        val island = source("PlanetCompassCelestialBodyDataIsland.kt")
        assertTrue(island.contains(".padding(horizontal = 12.dp)"))
        val header = island.substringAfter("testTag(\"celestial-body-header\")").substringBefore("HorizontalDivider")
        assertTrue(header.contains(".padding(vertical = 1.dp)"))
        val content = island.substringAfter("testTag(\"celestial-body-content\")")
        assertTrue(content.contains(".padding(vertical = if (compact) 8.dp else 10.dp)"))
        assertFalse(island.contains(".padding(horizontal = 10.dp, vertical = if (compact) 3.dp else 4.dp)"))
        assertTrue(island.contains("Modifier.size(48.dp).testTag(\"celestial-body-previous\")"))
        assertTrue(island.contains("Modifier.size(48.dp).testTag(\"celestial-body-next\")"))
    }

    @Test fun selectionCountReusesTheAcquisitionBubbleAndDoesNotDependOnTheInspectedObject() {
        val controls = source("PlanetCompassCelestialControls.kt")
        val badge = controls.substringAfter("if (selectedBodies.isNotEmpty()) PlanetCompassAnimatedCountBadge(")
            .substringBefore("PlanetCompassAdaptiveDropdownMenu")
        assertTrue(badge.contains("countText = selectedBodies.size.toString()"))
        assertTrue(badge.contains("minWidth = 18.dp, minHeight = 18.dp"))
        assertTrue(badge.contains("syncInProgress = false"))
        assertTrue(badge.contains("fontSize = 9.sp, horizontalPadding = 4.dp, shadowElevation = 2.dp"))
        assertFalse(badge.contains("remote.") || badge.contains("body.name"))
        assertTrue(controls.contains("stateDescription = selectedBodies.size.toString()"))
    }

    @Test fun everyOrbitalAndOutwardSpeedLabelUsesParenthesesInAllTwentyLanguages() {
        val folders = File("src/main/res").listFiles()!!.filter { File(it, "celestial.xml").isFile }
        assertEquals(20, folders.size)
        for (folder in folders) {
            fun strings(name: String): Map<String, String> {
                val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(folder, name))
                    .getElementsByTagName("string")
                return (0 until nodes.length).associate { index ->
                    val node = nodes.item(index)
                    node.attributes.getNamedItem("name").nodeValue to node.textContent
                }
            }
            val values = strings("celestial.xml") + strings("celestial_catalog.xml")
            for (key in listOf("celestial_speed_earth_orbit", "celestial_speed_orbit_earth",
                "celestial_speed_orbit_sun", "celestial_speed_orbit_jupiter", "celestial_speed_outward_sun")) {
                assertTrue("${folder.name}: $key", Regex("[(（][^()（）]+[)）]").containsMatchIn(values.getValue(key)))
                assertTrue(values.getValue(key).contains("%1\$s"))
            }
            assertEquals(values.getValue("celestial_speed_orbit_earth"), values.getValue("celestial_speed_earth_orbit"))
        }
    }
}
