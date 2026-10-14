package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_PANORAMA_LABEL_MODE_KEY = "panorama_label_mode"

/** Display policy is independent of selected rings, angular axes and position disclosure. */
internal enum class SpaceCompassPanoramaLabelMode(val key: String, val labelResource: Int) {
    ALL("all", R.string.panorama_labels_all),
    KEY_SELECTED("key-selected", R.string.panorama_labels_key_selected),
    OBJECTS_SELECTED("objects-selected", R.string.panorama_labels_objects_selected),
    SELECTED("selected", R.string.panorama_labels_selected),
    NONE("none", R.string.panorama_labels_none);

    val showsAll: Boolean get() = this == ALL
    val showsSelected: Boolean get() = this != NONE

    /** Object means the active target at the shutter, not every checked catalog body. */
    fun showsObject(activeObject: Boolean): Boolean = showsAll || (this == OBJECTS_SELECTED && activeObject)

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
