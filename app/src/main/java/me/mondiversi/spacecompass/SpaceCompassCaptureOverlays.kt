package me.mondiversi.spacecompass

import androidx.compose.runtime.saveable.listSaver

/** Frozen per-image display choices. Preview edits never update the main-view preferences. */
internal data class SpaceCompassCaptureOverlays(val grid: Boolean = SPACE_COMPASS_MAIN_GRID_DEFAULT,
    val references: Boolean = SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
    val numbers: Boolean = SPACE_COMPASS_GRID_NUMBERS_DEFAULT,
    val opacityPercent: Int = SPACE_COMPASS_GRID_OPACITY_DEFAULT) {
    val enabled: Boolean get() = grid || references
    val cacheKey: String get() = "grid-$grid-guides-$references-numbers-$numbers-opacity-${spaceCompassGridOpacity(opacityPercent)}"
    fun applyTo(snapshot: SpaceCompassPanoramaSnapshot) = snapshot.copy(showGrid = grid, showSkyReferences = references,
        showGridNumbers = numbers, gridOpacityPercent = spaceCompassGridOpacity(opacityPercent))
    companion object {
        fun fromSnapshot(snapshot: SpaceCompassPanoramaSnapshot?) = SpaceCompassCaptureOverlays(
            snapshot?.showGrid ?: SPACE_COMPASS_MAIN_GRID_DEFAULT,
            snapshot?.showSkyReferences ?: SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
            snapshot?.showGridNumbers ?: SPACE_COMPASS_GRID_NUMBERS_DEFAULT,
            spaceCompassGridOpacity(snapshot?.gridOpacityPercent ?: SPACE_COMPASS_GRID_OPACITY_DEFAULT))
    }
}

internal val spaceCompassCaptureOverlaysSaver = listSaver<SpaceCompassCaptureOverlays, Any>(
    save = { listOf(it.grid, it.references, it.numbers, spaceCompassGridOpacity(it.opacityPercent)) },
    restore = { SpaceCompassCaptureOverlays(it[0] as Boolean, it[1] as Boolean,
        it.getOrNull(2) as? Boolean ?: SPACE_COMPASS_GRID_NUMBERS_DEFAULT,
        spaceCompassGridOpacity(it.getOrNull(3) as? Int ?: SPACE_COMPASS_GRID_OPACITY_DEFAULT)) })
