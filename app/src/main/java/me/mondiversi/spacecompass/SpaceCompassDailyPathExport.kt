package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import java.util.UUID

internal fun spaceCompassSingleOrbitExportSnapshot(path: SpaceCompassSunDailyPath,
    current: SpaceCompassSunPathPoint?, selections: List<SpaceCompassSelectedOrbitPoint>, observationTime: Long) =
    SpaceCompassPanoramaSnapshot(current?.timeMs ?: observationTime,0.0,0.0,0.0,SpaceCompassSunSkyPhase.NIGHT,null,
        listOf(SpaceCompassPanoramaObject(path.body,path.body.name,path)),SpaceCompassCelestialRemoteData(),
        "",emptyList(),emptyList(),observerPositionKnown=false,
        selectedPoints=selections.filter { it.key.body==path.body }.toList(),zoneId=path.zone.id)

internal fun spaceCompassSingleOrbitPresentation(snapshot: SpaceCompassPanoramaSnapshot,
    profile: SpaceCompassDataExportProfile): SpaceCompassPanoramaSnapshot {
    val strings=profile.strings
    return snapshot.copy(objects=snapshot.objects.map { it.copy(name=strings.getString(it.body.nameResource)) },
        bodyNames=SpaceCompassCelestialBody.entries.associateWith { strings.getString(it.nameResource) },
        formatting=profile.formatting,currentPointName=strings.getString(R.string.celestial_point_current),
        pointEventNames=mapOf(SpaceCompassSunPathEvent.SUNRISE to R.string.celestial_rise,
            SpaceCompassSunPathEvent.CULMINATION to R.string.sun_path_culmination,
            SpaceCompassSunPathEvent.SUNSET to R.string.celestial_set,
            SpaceCompassSunPathEvent.MINIMUM to R.string.sun_path_minimum,
            SpaceCompassSunPathEvent.APPROACH to R.string.celestial_approach).mapValues { strings.getString(it.value) },
        orbitExportVocabulary=spaceCompassOrbitExportVocabulary(strings))
}

internal fun spaceCompassSingleOrbitExportTable(snapshot: SpaceCompassPanoramaSnapshot,
    path: SpaceCompassSunDailyPath, current: SpaceCompassSunPathPoint?): SpaceCompassCapturedOrbitTable {
    val currentPositions=current?.let { mapOf(path.body to it.position) }.orEmpty()
    val baseTable=captureSpaceCompassOrbitTable(snapshot,currentPositions)
    return baseTable.copy(rows=spaceCompassChronologicalOrbitExportRows(baseTable.rows+path.issPass.mapIndexed { index,point ->
        SpaceCompassOrbitExportRow(SpaceCompassOrbitExportRowKind.PASS,index,event=point.event,
            cells=mapOf(path.body to SpaceCompassOrbitExportCell(point.copy(coincidentEvents=point.coincidentEvents.toSet()),false)))
    }))
}

@Composable
internal fun rememberSpaceCompassDailyPathExport(path: SpaceCompassSunDailyPath, current: SpaceCompassSunPathPoint?,
    selections: List<SpaceCompassSelectedOrbitPoint>, nowMs: Long,
    location: SpaceCompassExportLocation? = null): SpaceCompassCaptureExportState {
    val profiles=rememberSpaceCompassDataExportProfiles()
    val options=listOf(SpaceCompassCaptureExportType.TXT,SpaceCompassCaptureExportType.CSV)
    return rememberSpaceCompassDataExport("orbit-${path.body.name}",options) {
        val capturedAt=System.currentTimeMillis()
        val snapshot=spaceCompassSingleOrbitExportSnapshot(path,current,selections,nowMs)
        val table=spaceCompassSingleOrbitExportTable(snapshot,path,current)
        val selected=spaceCompassSingleOrbitPresentation(snapshot,profiles.selected)
        val international=spaceCompassSingleOrbitPresentation(snapshot,profiles.international)
        val key="orbit-${UUID.randomUUID()}"
        SpaceCompassDataExportSource(key,options.associateWith { 1 },initialPosition=SpaceCompassPanoramaPosition.AREA) { context,types,mode,position ->
            val profile=profiles.forMode(mode)
            val stem=spaceCompassExportFileStem(capturedAt,profile.formatting,
                if (position==SpaceCompassPanoramaPosition.HIDDEN) "UTC" else path.zone.id,
                listOf(profile.strings.getString(path.body.nameResource),profile.strings.getString(R.string.celestial_orbits_title)))
            val presentation=spaceCompassSnapshotForDisclosure(if (mode==SpaceCompassPanoramaExportMode.SELECTED) selected else international,position)
            val directory=spaceCompassPageExportDirectory(context,key,mode,position)
            val place=if (position==SpaceCompassPanoramaPosition.HIDDEN) null else location?.let { fix ->
                spaceCompassPlaceKey(fix.latitude,fix.longitude,profile.formatting.locale.toLanguageTag())?.let { placeKey ->
                    try { lookupSpaceCompassEstimatedPlaceParts(context,placeKey) }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { null }
                }
            }
            val caption=spaceCompassDailyExportCaption(presentation,profile,location,place,position)
            SpaceCompassPreparedCaptureExport(options.filter { it in types }.map { type ->
                writeSpaceCompassPageExportFile(directory,"$stem.${type.extension}") { output ->
                    val text=if (type==SpaceCompassCaptureExportType.CSV) renderSpaceCompassOrbitCsv(table,presentation,capturedAt,caption)
                        else renderSpaceCompassOrbitTxt(table,presentation,capturedAt,caption)
                    output.write(text.toByteArray(Charsets.UTF_8))
                }
            })
        }
    }
}
