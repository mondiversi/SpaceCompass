package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_PANORAMA_LABEL_MODE_KEY = "panorama_label_mode"

/** Display policy is independent of selected rings, angular axes and position disclosure. */
internal enum class SpaceCompassPanoramaLabelMode(val key: String, val labelResource: Int) {
    ALL("all", R.string.panorama_labels_all),
    OBJECTS_SELECTED("objects-selected", R.string.panorama_labels_objects_selected),
    SELECTED("selected", R.string.panorama_labels_selected),
    NONE("none", R.string.panorama_labels_none);

    val showsAll: Boolean get() = this == ALL
    val showsObjects: Boolean get() = this == ALL || this == OBJECTS_SELECTED
    val showsSelected: Boolean get() = this != NONE

    fun showsPoint(selected: Boolean): Boolean = showsAll || (showsSelected && selected)

    companion object {
        /** Preserve the previous switch on upgrade; a saved label choice takes precedence. */
        fun fromStored(value: String?, legacyEnabled: Boolean = true): SpaceCompassPanoramaLabelMode =
            entries.firstOrNull { it.key == value } ?: if (legacyEnabled) ALL else NONE
    }
}
