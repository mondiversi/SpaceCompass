package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Uses cached administrative labels only; changing export profiles never triggers another lookup. */
@Composable
internal fun spaceCompassScenarioNameSuggestion(profiles: SpaceCompassDataExportProfiles):
    (SpaceCompassScenarioArchive, SpaceCompassPanoramaExportMode) -> String {
    val repository = spaceCompassPlaceRepository(LocalContext.current)
    return { archive, mode ->
        val c = archive.configuration
        val profile = profiles.forMode(mode)
        val place = if (c.simulatePosition) listOf(profile.formatting.locale,profiles.selected.formatting.locale,
            profiles.international.formatting.locale).distinct().firstNotNullOfOrNull { locale ->
            spaceCompassPlaceKey(c.latitude,c.longitude,locale.toLanguageTag())?.let(repository::cached)
        }?.substringBefore(',') else null
        spaceCompassScenarioExportSuggestedName(archive,profile,place)
    }
}
