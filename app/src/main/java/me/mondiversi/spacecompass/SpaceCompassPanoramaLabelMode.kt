package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_PANORAMA_LABEL_MODE_KEY = "panorama_label_mode"

internal const val SPACE_COMPASS_SKY_LABEL_MODE_KEY = "sky_panorama_label_mode"

/** The same live scopes and first-install default as the Globe, saved independently. */
internal fun spaceCompassSkyLabelMode(value: String?) = spaceCompassGlobeLabelMode(value)

/** Two instances of one body retain independent selected-only curve labels. */
internal fun SpaceCompassPanoramaSnapshot.showsOrbitName(item: SpaceCompassPanoramaObject,
    selections: List<SpaceCompassPanoramaSelectedPoint>): Boolean = labelMode.showsOrbitName(isActive(item),
        selections.any { it.body == item.body && it.comparison?.id == item.comparison?.id })

/** Display policy is independent of selected rings, angular axes and position disclosure. */
internal enum class SpaceCompassPanoramaLabelMode(val key: String, val labelResource: Int) {
    ALL("all", R.string.panorama_labels_all),
    OBJECTS_SELECTED("objects-selected", R.string.panorama_labels_objects_selected),
    KEY_SELECTED("key-selected", R.string.panorama_labels_key_selected),
    SELECTED("selected", R.string.panorama_labels_selected),
    NONE("none", R.string.panorama_labels_none);

    val showsAll: Boolean get() = this == ALL
    val showsSelected: Boolean get() = this != NONE

    /** Object means the active target at the shutter, not every checked catalog body. */
    fun showsObject(activeObject: Boolean): Boolean = showsAll || (this == OBJECTS_SELECTED && activeObject)

    /** Curve identities do not enable hourly pills or duplicate names over current thumbnails. */
    fun showsOrbitName(activeObject: Boolean, selectedObject: Boolean): Boolean =
        showsObject(activeObject) || this == KEY_SELECTED || (showsSelected && selectedObject)

    fun showsPoint(selected: Boolean, activeObject: Boolean = false, keyPoint: Boolean = false): Boolean =
        showsObject(activeObject) || (this == KEY_SELECTED && keyPoint) || (showsSelected && selected)

    /** A coincident rise/set/extremum is still a key point even if the primary event is HOUR. */
    fun showsPathPoint(point: SpaceCompassSunPathPoint, activeObject: Boolean = false): Boolean =
        showsPoint(selected = false, activeObject = activeObject,
            keyPoint = point.events.any { it != SpaceCompassSunPathEvent.HOUR })

    fun showsCurrentPoint(activeObject: Boolean = false): Boolean =
        showsPoint(selected = false, activeObject = activeObject, keyPoint = true)

    companion object {
        val DEFAULT: SpaceCompassPanoramaLabelMode = KEY_SELECTED

        /** Keep explicit saved modes and legacy switches; only absent settings use the new default. */
        fun fromStored(value: String?, legacyEnabled: Boolean? = null): SpaceCompassPanoramaLabelMode =
            entries.firstOrNull { it.key == value } ?: when (legacyEnabled) {
                true -> ALL
                false -> NONE
                null -> DEFAULT
            }
    }
}
