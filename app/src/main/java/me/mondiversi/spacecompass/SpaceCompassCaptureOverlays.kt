package me.mondiversi.spacecompass

import androidx.compose.runtime.saveable.listSaver

/** Frozen per-image display choices. Preview edits never update the main-view preferences. */
internal data class SpaceCompassCaptureOverlays(val grid: Boolean = SPACE_COMPASS_MAIN_GRID_DEFAULT,
    val references: Boolean = SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
    val numbers: Boolean = SPACE_COMPASS_GRID_NUMBERS_DEFAULT) {
    val enabled: Boolean get() = grid || references
    val cacheKey: String get() = "grid-$grid-guides-$references-numbers-$numbers"
    fun applyTo(snapshot: SpaceCompassPanoramaSnapshot) = snapshot.copy(showGrid = grid, showSkyReferences = references, showGridNumbers = numbers)
    companion object {
        fun fromSnapshot(snapshot: SpaceCompassPanoramaSnapshot?) = SpaceCompassCaptureOverlays(
            snapshot?.showGrid ?: SPACE_COMPASS_MAIN_GRID_DEFAULT,
            snapshot?.showSkyReferences ?: SPACE_COMPASS_SKY_REFERENCES_DEFAULT,
            snapshot?.showGridNumbers ?: SPACE_COMPASS_GRID_NUMBERS_DEFAULT)
    }
}

internal val spaceCompassCaptureOverlaysSaver = listSaver<SpaceCompassCaptureOverlays, Boolean>(
    save = { listOf(it.grid, it.references, it.numbers) }, restore = { SpaceCompassCaptureOverlays(it[0], it[1], it.getOrNull(2) ?: SPACE_COMPASS_GRID_NUMBERS_DEFAULT) })
