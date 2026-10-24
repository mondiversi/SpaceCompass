package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import android.graphics.Bitmap
import java.util.TimeZone
import java.util.UUID

internal data class SpaceCompassObservatoryImages(val body: SpaceCompassCelestialBody,
    val current: SpaceCompassCelestialViewGeometry?, val rotation: SpaceCompassCelestialViewGeometry,
    val viewport: SpaceCompassCelestialViewportState, val size: IntSize)
internal data class SpaceCompassObservatoryPresentation(val document: SpaceCompassExportDocument,
    val currentCaption: String, val rotationCaption: String, val missingImage: String, val credits: String)

internal fun spaceCompassObservatoryPresentation(body: SpaceCompassCelestialBody, timeMs: Long, capturedAt: Long,
    zone: TimeZone, remote: SpaceCompassCelestialRemoteData, distance: Double?, phase: SpaceCompassMoonPhase?,
    geometry: SpaceCompassCelestialViewGeometry?, coordinates: SpaceCompassCelestialCoordinates?,
    profile: SpaceCompassDataExportProfile): SpaceCompassObservatoryPresentation {
    val strings=profile.strings
    val facts=spaceCompassCelestialFacts(body)
    val sections=spaceCompassCelestialDetailSections(body,facts,timeMs,remote,distance,phase,geometry?.illuminatedFraction,
        coordinates,strings,profile.formatting,profile.units)
    val name=strings.getString(body.nameResource)
    val observed=formatSpaceCompassPanoramaExportTimestamp(timeMs,zone,profile.formatting)
    val captured=formatSpaceCompassPanoramaExportTimestamp(capturedAt,zone,profile.formatting)
    val notes=spaceCompassObservatoryExportNotes(body,profile)
    val document=SpaceCompassExportDocument(strings.getString(R.string.celestial_details_title)+" · "+name,
        listOf(strings.getString(R.string.capture_table_captured) to captured,
            strings.getString(R.string.capture_table_observed) to observed),sections,notes,
        listOf(R.string.data_export_section,R.string.data_export_field,R.string.data_export_value,R.string.data_export_notes)
            .map { strings.getString(it) },strings.getString(R.string.data_export_notes),
        formatSpaceCompassNumber(1.5,1,profile.formatting.numeric,grouping=false,
            systemLocale=profile.formatting.deviceLocale).contains(','))
    return SpaceCompassObservatoryPresentation(document,
        "$name · ${strings.getString(R.string.celestial_view_current)} · $observed",
        "$name · ${strings.getString(R.string.celestial_view_rotation)} · $observed",
        strings.getString(R.string.celestial_view_location_needed),notes.joinToString("\n"))
}

@Composable
internal fun rememberSpaceCompassObservatoryExport(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData, distance: Double?, phase: SpaceCompassMoonPhase?,
    geometry: SpaceCompassCelestialViewGeometry?, coordinates: SpaceCompassCelestialCoordinates?,
    rotation: () -> SpaceCompassCelestialRotation, viewport: () -> SpaceCompassCelestialViewportState,
    modelSize: () -> IntSize): SpaceCompassCaptureExportState {
    val profiles=rememberSpaceCompassDataExportProfiles()
    val zone=TimeZone.getTimeZone(spaceCompassObservationZone())
    val options=listOf(SpaceCompassCaptureExportType.CURRENT_IMAGE,SpaceCompassCaptureExportType.ROTATION_IMAGE,
        SpaceCompassCaptureExportType.TXT,SpaceCompassCaptureExportType.CSV)
    return rememberSpaceCompassDataExport("observatory-${body.name}",options) {
        val capturedAt=System.currentTimeMillis()
        val rotationGeometry=rotation().geometry()
        val images=SpaceCompassObservatoryImages(body,if (body.hasPhysicalFace) geometry else rotationGeometry,
            rotationGeometry,viewport().copy(),modelSize())
        val selected=spaceCompassObservatoryPresentation(body,timeMs,capturedAt,zone,remote,distance,phase,geometry,coordinates,profiles.selected)
        val international=spaceCompassObservatoryPresentation(body,timeMs,capturedAt,zone,remote,distance,phase,geometry,coordinates,profiles.international)
        val key="observatory-${UUID.randomUUID()}"
        val stem=spaceCompassPanoramaFileName(capturedAt).removeSuffix(".jpg")+"_${body.name.lowercase(java.util.Locale.ROOT)}"
        SpaceCompassDataExportSource(key,options.associateWith { 1 }) { context,types,mode,_ ->
            val presentation=if (mode==SpaceCompassPanoramaExportMode.SELECTED) selected else international
            if (types.any { it.extension=="jpg" }) ensureSpaceCompassCelestialTextures(context,setOf(body))
            val directory=spaceCompassPageExportDirectory(context,key,mode)
            SpaceCompassPreparedCaptureExport(options.filter { it in types }.map { type ->
                val suffix=when (type) {
                    SpaceCompassCaptureExportType.CURRENT_IMAGE -> "_current.jpg"
                    SpaceCompassCaptureExportType.ROTATION_IMAGE -> "_rotation.jpg"
                    else -> ".${type.extension}"
                }
                writeSpaceCompassPageExportFile(directory,stem+suffix) { output ->
                    when (type) {
                        SpaceCompassCaptureExportType.CSV -> output.write(renderSpaceCompassExportDocumentCsv(presentation.document).toByteArray(Charsets.UTF_8))
                        SpaceCompassCaptureExportType.TXT -> output.write(renderSpaceCompassExportDocumentTxt(presentation.document).toByteArray(Charsets.UTF_8))
                        else -> {
                            val bitmap=renderSpaceCompassObservatoryImage(context,images,
                                type==SpaceCompassCaptureExportType.CURRENT_IMAGE,presentation)
                            try { check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,output)) }
                            finally { bitmap.recycle() }
                        }
                    }
                }.also { if (type.extension=="jpg") stampSpaceCompassPanoramaFile(it,capturedAt,presentation.credits) }
            })
        }
    }
}
