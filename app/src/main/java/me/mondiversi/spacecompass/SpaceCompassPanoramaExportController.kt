package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable

/** The panorama keeps its frozen capture; only display options are copied when export is opened. */
@Composable
internal fun rememberSpaceCompassCaptureExport(data: SpaceCompassPanoramaPreviewData,
    position: SpaceCompassPanoramaPosition, labels: SpaceCompassPanoramaLabelMode,
    center: SpaceCompassPanoramaCenter, previewReady: Boolean,
    showGrid: Boolean = data.snapshot?.showGrid ?: SPACE_COMPASS_MAIN_GRID_DEFAULT,
    showReferences: Boolean = data.snapshot?.showSkyReferences ?: SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
    showGridNumbers: Boolean = data.snapshot?.showGridNumbers ?: SPACE_COMPASS_GRID_NUMBERS_DEFAULT): SpaceCompassCaptureExportState {
    val overlays = SpaceCompassCaptureOverlays(showGrid, showReferences, showGridNumbers)
    val profiles = rememberSpaceCompassDataExportProfiles()
    val images=spaceCompassCaptureImageTypes(data.cameraPhoto!=null)
    return rememberSpaceCompassDataExport(data.file.absolutePath,
        images+listOf(SpaceCompassCaptureExportType.TXT,SpaceCompassCaptureExportType.CSV),
        previewReady,
        data.selectedPresentation != null) {
        SpaceCompassDataExportSource(data.file.absolutePath+spaceCompassPanoramaVariantKey(position,labels,
            SpaceCompassPanoramaExportMode.SELECTED,center)+"-${overlays.cacheKey}",
            mapOf(SpaceCompassCaptureExportType.IMAGE to 1,
                SpaceCompassCaptureExportType.FRAMED_IMAGE to if(data.snapshot?.let { it.skyFraming!=null||it.globe?.framing!=null }==true) 1 else 0,
                SpaceCompassCaptureExportType.PANORAMA_IMAGE to 1,
                SpaceCompassCaptureExportType.CSV to if (data.orbitTable?.bodies?.isNotEmpty()==true) 1 else 0,
                SpaceCompassCaptureExportType.TXT to if (data.orbitTable?.bodies?.isNotEmpty()==true) 1 else 0),
            initialPosition = position) { context,types,mode,disclosure ->
            prepareSpaceCompassCaptureExport(context,data,types,disclosure,labels,mode,center,profiles.forMode(mode),showGrid,showReferences,showGridNumbers)
        }
    }
}
