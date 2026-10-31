package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import java.util.UUID

/** Both delivery formats share the frozen year and the app-wide formatting/disclosure controls. */
@Composable
internal fun rememberSpaceCompassSkyHeightsExport(body: SpaceCompassCelestialBody, timeMs: Long,
    result: SpaceCompassObservationMomentsUiState): SpaceCompassCaptureExportState {
    val profiles = rememberSpaceCompassDataExportProfiles()
    val options = listOf(SpaceCompassCaptureExportType.TXT, SpaceCompassCaptureExportType.CSV)
    return rememberSpaceCompassDataExport("sky-heights-${body.name}", options) {
        val snapshot = captureSpaceCompassSkyHeights(result, timeMs, System.currentTimeMillis())
        val key = "sky-heights-${UUID.randomUUID()}"
        SpaceCompassDataExportSource(key, options.associateWith { 1 },
            initialPosition = SpaceCompassPanoramaPosition.AREA) { context, types, mode, disclosure ->
            val profile = profiles.forMode(mode)
            val place = if (disclosure == SpaceCompassPanoramaPosition.HIDDEN) null else
                spaceCompassPlaceKey(snapshot.place.latitude, snapshot.place.longitude,
                    profile.formatting.locale.toLanguageTag())?.let {
                    try { lookupSpaceCompassEstimatedPlaceParts(context, it) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                }
            val document = spaceCompassSkyHeightsDocument(snapshot, profile, place, disclosure)
            val zone = if (disclosure == SpaceCompassPanoramaPosition.HIDDEN) "UTC" else snapshot.calendar.zone.id
            val year = formatSpaceCompassNumber(snapshot.calendar.year.toDouble(), 0, profile.formatting.numeric,
                grouping = false, systemLocale = profile.formatting.deviceLocale)
            val stem = spaceCompassExportFileStem(snapshot.capturedAtMs, profile.formatting, zone,
                listOf(profile.strings.getString(body.nameResource), profile.strings.getString(R.string.observation_heights_title), year))
            val directory = spaceCompassPageExportDirectory(context, key, mode, disclosure)
            SpaceCompassPreparedCaptureExport(options.filter { it in types }.map { type ->
                writeSpaceCompassPageExportFile(directory, "$stem.${type.extension}") { output ->
                    val text = if (type == SpaceCompassCaptureExportType.CSV) renderSpaceCompassSkyHeightsCsv(document)
                        else renderSpaceCompassSkyHeightsTxt(document)
                    output.write(text.toByteArray(Charsets.UTF_8))
                }
            })
        }
    }
}
