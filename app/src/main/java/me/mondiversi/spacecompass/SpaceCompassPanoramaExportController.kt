package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable

/** The panorama keeps its frozen capture; only display options are copied when export is opened. */
@Composable
internal fun rememberSpaceCompassCaptureExport(data: SpaceCompassPanoramaPreviewData,
    position: SpaceCompassPanoramaPosition, labels: SpaceCompassPanoramaLabelMode,
    center: SpaceCompassPanoramaCenter, previewReady: Boolean): SpaceCompassCaptureExportState {
    val profiles = rememberSpaceCompassDataExportProfiles()
    return rememberSpaceCompassDataExport(data.file.absolutePath,
        listOf(SpaceCompassCaptureExportType.IMAGE,SpaceCompassCaptureExportType.TXT,SpaceCompassCaptureExportType.CSV),
        previewReady,
        data.selectedPresentation != null) {
        SpaceCompassDataExportSource(data.file.absolutePath+spaceCompassPanoramaVariantKey(position,labels,
            SpaceCompassPanoramaExportMode.SELECTED,center),
            mapOf(SpaceCompassCaptureExportType.IMAGE to 1,
                SpaceCompassCaptureExportType.CSV to if (data.orbitTable?.bodies?.isNotEmpty()==true) 1 else 0,
                SpaceCompassCaptureExportType.TXT to if (data.orbitTable?.bodies?.isNotEmpty()==true) 1 else 0),
            initialPosition = position) { context,types,mode,disclosure ->
            prepareSpaceCompassCaptureExport(context,data,types,disclosure,labels,mode,center,profiles.forMode(mode))
        }
    }
}
