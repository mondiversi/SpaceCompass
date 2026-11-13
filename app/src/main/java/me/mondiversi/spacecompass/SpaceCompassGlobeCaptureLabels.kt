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
        else -> when(legacyEnabled) {
            false -> SpaceCompassPanoramaLabelMode.NONE
            true -> SpaceCompassPanoramaLabelMode.KEY_SELECTED
            null -> SpaceCompassPanoramaLabelMode.SELECTED
        }
    }
    return if(mode==SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED) SpaceCompassPanoramaLabelMode.KEY_SELECTED else mode
}

/** Identical scope to photographic/panoramic labels, including selected antipodes when displayed. */
internal fun spaceCompassGlobeCaptureShowsLabel(mode: SpaceCompassPanoramaLabelMode,active: Boolean,
    current: Boolean,antipode: Boolean,events: Set<SpaceCompassSunPathEvent>,selected: Boolean=false): Boolean =
    mode.showsPoint(selected,active,keyPoint=!antipode&&(current||events.any { it!=SpaceCompassSunPathEvent.HOUR }))

/** Active-track labels follow the picker; explicit selections remain visible across every track. */
internal fun spaceCompassGlobeMainShowsLabel(mode: SpaceCompassPanoramaLabelMode,active: Boolean,
    current: Boolean,antipode: Boolean,events: Set<SpaceCompassSunPathEvent>,selected: Boolean=false): Boolean =
    (active || selected) && spaceCompassGlobeCaptureShowsLabel(mode,active,current,antipode,events,selected)

/** Every live Globe label retains clock and T± together, using its track's own reference moment. */
internal fun formatSpaceCompassGlobeMainPointTime(timeMs: Long,referenceTimeMs: Long,
    zone: java.util.TimeZone,formatting: SpaceCompassPanoramaFormatting): String {
    val moment=formatSpaceCompassPanoramaMarkerTime(timeMs,referenceTimeMs,zone,formatting)
    return moment+" · "+formatSpaceCompassPanoramaPointCountdown(timeMs,referenceTimeMs,formatting)
}

internal data class SpaceCompassGlobeLabelDetails(val full: String,val compact: String,val minimal: String)

/** Detail reduction never loses the clock/T pair; angles and object headings return when space permits. */
internal fun formatSpaceCompassGlobeMainLabelDetails(bodyName: String,eventName: String,moment: String,
    position: SpaceCompassSunPosition,formatting: SpaceCompassPanoramaFormatting): SpaceCompassGlobeLabelDetails {
    val title=listOf(bodyName,eventName).filter(String::isNotBlank).joinToString(" · ")
    val minimal=listOf(eventName,moment).filter(String::isNotBlank).joinToString(" · ")
    return SpaceCompassGlobeLabelDetails(
        formatSpaceCompassPanoramaPointLabel(moment,position,formatting,title),
        (eventName.ifBlank { bodyName })+"\n"+moment,minimal)
}
