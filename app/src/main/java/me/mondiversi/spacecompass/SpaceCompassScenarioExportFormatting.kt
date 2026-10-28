package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/** Readable fields use the chosen profile; portable import properties always retain canonical units. */
internal fun spaceCompassScenarioExportFields(archive: SpaceCompassScenarioArchive,
    profile: SpaceCompassDataExportProfile, place: String? = null): List<Pair<String,String>> {
    val c = archive.configuration
    val f = profile.formatting
    val strings = profile.strings
    val zone = TimeZone.getTimeZone(c.momentZoneId)
    val dateLocale = if (f.date==SpaceCompassDateFormat.SYSTEM) f.deviceLocale else f.locale
    fun number(value: Double, decimals: Int) = formatSpaceCompassNumber(value,decimals,f.numeric,
        grouping=false,minimumDigits=0,systemLocale=f.deviceLocale)
    return buildList {
        if (c.simulatePosition) add(strings.getString(R.string.observer_place) to
            (place ?: formatSpaceCompassSelectedCoordinates(c.latitude,c.longitude,f.numeric,f.dms,f.deviceLocale).orEmpty()))
        if (c.simulateAltitude) add(strings.getString(R.string.observer_altitude) to
            (number(if (profile.units.feet) c.altitudeMeters/.3048 else c.altitudeMeters,1)+
                if (profile.units.feet) "ft" else "m"))
        if (c.simulateZone) add(strings.getString(R.string.observer_zone) to
            formatSpaceCompassObserverUtcOffset(ZoneId.of(c.zoneId).rules.getOffset(Instant.ofEpochMilli(c.timeMs))))
        if (c.simulateDate) add(strings.getString(R.string.observer_date) to formatSpaceCompassDateOnly(c.timeMs,
            f.date,dateLocale,zone,f.numeric,f.deviceLocale,allowHistorical=true))
        if (c.simulateTime) add(strings.getString(R.string.observer_time) to formatSpaceCompassTimeOnly(c.timeMs,
            f.time,f.locale,zone,f.numeric,f.deviceLocale,includeSeconds=false,allowHistorical=true))
        if (archive.timeSettings.enabled) add(strings.getString(R.string.scenario_speed) to
            (number(archive.timeSettings.rate.toDouble(),0)+"x"))
    }
}

internal fun spaceCompassScenarioExportSuggestedName(archive: SpaceCompassScenarioArchive,
    profile: SpaceCompassDataExportProfile, place: String? = null): String {
    val c = archive.configuration
    val fields = spaceCompassScenarioExportFields(archive,profile,place).iterator()
    fun next(enabled: Boolean, label: Boolean = true): String? = if (!enabled) null else fields.next().let {
        // Keep a negative physical value distinct from the hyphen that joins label and value.
        val value = it.second.replaceFirst(Regex("^-"),"−")
        if (label) "${it.first}-$value" else value
    }
    val parts = SpaceCompassScenarioNameParts(next(c.simulatePosition,false),next(c.simulateAltitude),
        next(c.simulateZone),next(c.simulateDate),next(c.simulateTime),next(archive.timeSettings.enabled))
    return spaceCompassScenarioSuggestedName(archive,parts,profile.strings.getString(R.string.observer_current_scenario))
}

/** Comment-only presentation remains readable and compatible with version 1 importers in every locale. */
internal fun spaceCompassScenarioExportText(archive: SpaceCompassScenarioArchive,
    mode: SpaceCompassPanoramaExportMode, profile: SpaceCompassDataExportProfile): String {
    val strings = profile.strings
    val resume = when (archive.timeSettings.resume) {
        SpaceCompassScenarioResume.RESTART -> R.string.scenario_restart
        SpaceCompassScenarioResume.ACTIVE -> R.string.scenario_active
        SpaceCompassScenarioResume.ALWAYS -> R.string.scenario_always
    }
    val fields = listOf(strings.getString(R.string.scenario_file_name) to archive.name,
        strings.getString(R.string.panorama_export_profile) to strings.getString(
            if (mode==SpaceCompassPanoramaExportMode.SELECTED) R.string.panorama_export_selected else R.string.panorama_export_international))+
        spaceCompassScenarioExportFields(archive,profile)+
        (strings.getString(R.string.scenario_resume) to strings.getString(resume))
    fun safeComment(value: String) = value.replace(Regex("[\\p{Cc}\\p{Cf}]")," ")
    val text = fields.joinToString("\n",postfix="\n") { "# ${safeComment(it.first)}: ${safeComment(it.second)}" }+archive.encode()
    require(text.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_SCENARIO_FILE_LIMIT)
    return text
}
