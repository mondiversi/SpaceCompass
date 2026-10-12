package me.mondiversi.spacecompass

import android.content.Context

/** Preparation reads only values captured by the entry point, including image orientation. */
internal class SpaceCompassDataExportSource(val key: String,
    val counts: Map<SpaceCompassCaptureExportType,Int>,
    val initialPosition: SpaceCompassPanoramaPosition? = null,
    val prepare: suspend (Context,Set<SpaceCompassCaptureExportType>,SpaceCompassPanoramaExportMode,SpaceCompassPanoramaPosition) -> SpaceCompassPreparedCaptureExport)
