package me.mondiversi.spacecompass

/** The screen and both frozen export profiles use exactly the same event names and numbering. */
internal fun spaceCompassObservationMomentTitle(entry: SpaceCompassObservationMomentRow,
    profile: SpaceCompassDataExportProfile): String {
    if (entry.annualExtreme) return profile.strings.getString(
        if (entry.kind == SpaceCompassObservationMomentKind.MAXIMUM) R.string.observation_height_maximum
        else R.string.observation_height_minimum)
    val single = entry.number == null
    val id = when (entry.kind) {
        SpaceCompassObservationMomentKind.MAXIMUM -> if (single) R.string.observation_height_cycle_maximum_single
            else R.string.observation_height_cycle_maximum
        SpaceCompassObservationMomentKind.MINIMUM -> if (single) R.string.observation_height_cycle_minimum_single
            else R.string.observation_height_cycle_minimum
        SpaceCompassObservationMomentKind.MID_RISING -> if (single) R.string.observation_height_mid_rising_single
            else R.string.observation_height_mid_rising
        SpaceCompassObservationMomentKind.MID_SETTING -> if (single) R.string.observation_height_mid_setting_single
            else R.string.observation_height_mid_setting
    }
    return if (single) profile.strings.getString(id) else profile.strings.getString(id,
        formatSpaceCompassNumber(entry.number!!.toDouble(), 0, profile.formatting.numeric,
            grouping = false, systemLocale = profile.formatting.deviceLocale))
}
