package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_GLOBE_LABEL_MODE_KEY="globe_label_mode"

internal val SPACE_COMPASS_GLOBE_MAIN_LABEL_MODES = SpaceCompassPanoramaLabelMode.entries.filter {
    it != SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED
}

/** Main-view migration only. Captured images continue to support all five scopes. */
internal fun spaceCompassGlobeLabelMode(value: String?,legacyEnabled: Boolean?=null): SpaceCompassPanoramaLabelMode {
    val mode = SpaceCompassPanoramaLabelMode.entries.firstOrNull { it.key==value } ?: when(value) {
        "all-objects" -> SpaceCompassPanoramaLabelMode.ALL
        "selected-object" -> SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED
        else -> if(legacyEnabled==false) SpaceCompassPanoramaLabelMode.NONE else SpaceCompassPanoramaLabelMode.KEY_SELECTED
    }
    return if(mode==SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED) SpaceCompassPanoramaLabelMode.KEY_SELECTED else mode
}

/** Identical scope to photographic/panoramic labels, including selected antipodes when displayed. */
internal fun spaceCompassGlobeCaptureShowsLabel(mode: SpaceCompassPanoramaLabelMode,active: Boolean,
    current: Boolean,antipode: Boolean,events: Set<SpaceCompassSunPathEvent>,selected: Boolean=false): Boolean =
    mode.showsPoint(selected,active,keyPoint=!antipode&&(current||events.any { it!=SpaceCompassSunPathEvent.HOUR }))

/** The live picker describes points of the island's active object, including its fixed-date variant. */
internal fun spaceCompassGlobeMainShowsLabel(mode: SpaceCompassPanoramaLabelMode,active: Boolean,
    current: Boolean,antipode: Boolean,events: Set<SpaceCompassSunPathEvent>,selected: Boolean=false): Boolean =
    active && spaceCompassGlobeCaptureShowsLabel(mode,true,current,antipode,events,selected)
