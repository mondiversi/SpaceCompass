package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources

/** Presentation stays separate from the animated model and the shared scientific table data. */
@Composable
internal fun SpaceCompassCelestialDetailSections(body: SpaceCompassCelestialBody,
    facts: SpaceCompassCelestialFacts, timeMs: Long, remote: SpaceCompassCelestialRemoteData,
    distance: Double?, phase: SpaceCompassMoonPhase?, illuminatedFraction: Double?,
    coordinates: SpaceCompassCelestialCoordinates?, primaryText: Color, secondaryText: Color,
    orbitContent: (@Composable () -> Unit)? = null) {
    val profile = rememberSpaceCompassDataExportProfiles().selected
    for (section in spaceCompassCelestialDetailSections(body,facts,timeMs,remote,distance,phase,
        illuminatedFraction,coordinates,profile.strings,profile.formatting,LocalSpaceCompassUnits.current)) {
        SpaceCompassCelestialDetailTable(section.title,section.icon,section.rows,primaryText,secondaryText,
            section.tag,section.note,extraContent = orbitContent.takeIf { section.tag == "celestial-view-orbit-island" })
    }
}
