package me.mondiversi.spacecompass

import java.io.StringReader
import java.io.StringWriter
import java.time.ZoneId
import java.util.Properties

internal const val SPACE_COMPASS_SCENARIO_FILE_LIMIT = 1_048_576
internal const val SPACE_COMPASS_SCENARIO_EXTENSION = ".spacecompass"
internal const val SPACE_COMPASS_SCENARIO_MIME = "application/octet-stream"

/** Portable configuration only: no running-clock anchor, checkpoint, device ID or elapsed time. */
internal data class SpaceCompassScenarioArchive(val name: String, val configuration: SpaceCompassObserverPlan,
    val timeSettings: SpaceCompassScenarioTimeSettings,
    val celestialSelection: SpaceCompassCelestialSelection? = null, val temporalSettingsIncluded: Boolean = true) {
    val custom: Boolean get() = configuration.activeSections.isNotEmpty()

    fun configurationAtImport(now: Long, deviceZone: ZoneId): SpaceCompassObserverPlan {
        val base = configuration.copy(advanceTime = false, clockOffsetMs = null)
        val offset = if (configuration.simulateTime && configuration.advanceTime)
            Math.subtractExact(base.observationTimeMs(now, deviceZone), now) else null
        return base.copy(advanceTime = configuration.advanceTime, clockOffsetMs = offset)
    }

    fun encode(): String {
        require(name.isNotBlank() && name.length <= 80 && name.none(Char::isISOControl))
        require(timeSettings.rate in spaceCompassScenarioRates)
        val c = configuration
        if (!temporalSettingsIncluded) require(!c.simulateDate && !c.simulateTime && !c.advanceTime &&
            c.clockOffsetMs == null && c.timeMs == 0L && c.momentZoneId == "UTC" && timeSettings == SpaceCompassScenarioTimeSettings())
        val properties = Properties().apply {
            setProperty("format", "space-compass-scenario"); setProperty("version", "1"); setProperty("name", name)
            setProperty("latitude", c.latitude.toString()); setProperty("longitude", c.longitude.toString())
            setProperty("altitudeMeters", c.altitudeMeters.toString()); setProperty("zoneId", c.zoneId)
            setProperty("momentMs", c.timeMs.toString()); setProperty("momentZoneId", c.momentZoneId)
            setProperty("positionEnabled", c.simulatePosition.toString()); setProperty("altitudeEnabled", c.simulateAltitude.toString())
            setProperty("zoneEnabled", c.simulateZone.toString()); setProperty("dateEnabled", c.simulateDate.toString())
            setProperty("timeEnabled", c.simulateTime.toString()); setProperty("advanceWithClock", c.advanceTime.toString())
            c.automaticAltitudeMeters?.let { setProperty("automaticAltitudeMeters", it.toString()) }
            c.automaticZoneId?.let { setProperty("automaticZoneId", it) }
            setProperty("speedEnabled", timeSettings.enabled.toString()); setProperty("speedRate", timeSettings.rate.toString())
            setProperty("resume", timeSettings.resume.name)
            // Legacy readers require neutral temporal fields; new readers leave the recipient's resume preference intact.
            if (!temporalSettingsIncluded) setProperty("temporalSettingsIncluded", "false")
            celestialSelection?.let { selection ->
                require(selection.selected == selection.ordered.toSet())
                setProperty("objectsIncluded", "true")
                setProperty("objects", selection.ordered.joinToString(",") { it.name })
                setProperty("activeObject", selection.active?.name.orEmpty())
            }
        }
        // Properties escapes control characters and delimiters. Omit its generated wall-clock comment.
        val out = StringWriter().also { properties.store(it, null) }.toString()
            .lineSequence().filter { it.isNotEmpty() && !it.startsWith('#') }.sorted().joinToString("\n", postfix = "\n")
        require(out.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_SCENARIO_FILE_LIMIT)
        decode(out) // Validate before offering an archive for delivery.
        return out
    }

    companion object {
        fun capture(observer: SpaceCompassObserverState, name: String, now: Long = System.currentTimeMillis(),
            deviceZone: ZoneId = ZoneId.systemDefault()): SpaceCompassScenarioArchive {
            val active = observer.plan
            val stored = observer.savedPlan ?: SpaceCompassObserverPlan(observer.devicePlace?.latitude ?: 0.0,
                observer.devicePlace?.longitude ?: 0.0, observer.devicePlace?.altitude ?: 0.0, now, deviceZone.id,
                simulatePosition = false, simulateTime = false, simulateAltitude = false, simulateZone = false, simulateDate = false)
            val c = stored.copy(simulatePosition = active?.simulatePosition == true,
                simulateAltitude = active?.simulateAltitude == true, simulateZone = active?.simulateZone == true,
                simulateDate = active?.simulateDate == true, simulateTime = active?.simulateTime == true,
                clockOffsetMs = if (active?.simulateTime == true && stored.advanceTime) 0L else null)
            return SpaceCompassScenarioArchive(name, c, observer.timeSettings)
        }

        fun decode(text: String): SpaceCompassScenarioArchive {
            require(text.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_SCENARIO_FILE_LIMIT)
            val p = Properties().apply { load(StringReader(text.removePrefix("\uFEFF"))) }
            fun value(key: String): String = requireNotNull(p.getProperty(key))
            fun bool(key: String) = value(key).toBooleanStrict()
            require(value("format") == "space-compass-scenario" && value("version") == "1")
            val name = value("name"); require(name.isNotBlank() && name.length <= 80 && name.none(Char::isISOControl))
            val advance = bool("advanceWithClock"); val time = bool("timeEnabled")
            val c = requireNotNull(spaceCompassObserverPlan(value("latitude").toDouble(), value("longitude").toDouble(),
                value("altitudeMeters").toDouble(), value("momentMs").toLong(), value("zoneId"),
                simulatePosition = bool("positionEnabled"), simulateTime = time, simulateAltitude = bool("altitudeEnabled"),
                simulateZone = bool("zoneEnabled"), automaticAltitudeMeters = p.getProperty("automaticAltitudeMeters")?.toDouble(),
                automaticZoneId = p.getProperty("automaticZoneId"), simulateDate = bool("dateEnabled"),
                momentZoneId = value("momentZoneId"), advanceTime = advance,
                clockOffsetMs = if (advance && time) 0L else null, allowInactive = true))
            val settings = SpaceCompassScenarioTimeSettings(bool("speedEnabled"), value("speedRate").toInt(),
                SpaceCompassScenarioResume.valueOf(value("resume")))
            require(settings.rate in spaceCompassScenarioRates)
            val selection = p.getProperty("objectsIncluded")?.let { included ->
                if (!included.toBooleanStrict()) null else {
                    val names = value("objects").takeIf { it.isNotBlank() }?.split(',')?.toSet() ?: emptySet()
                    restoreSpaceCompassCelestialSelection(names, value("activeObject").takeIf { it.isNotEmpty() })
                }
            }
            val temporal = p.getProperty("temporalSettingsIncluded")?.toBooleanStrict() ?: true
            if (!temporal) require(!c.simulateDate && !c.simulateTime && !c.advanceTime && c.clockOffsetMs == null &&
                c.timeMs == 0L && c.momentZoneId == "UTC" && settings == SpaceCompassScenarioTimeSettings())
            return SpaceCompassScenarioArchive(name, c, settings, selection, temporal)
        }
    }
}

internal fun spaceCompassScenarioFileName(name: String): String {
    val safe = name.trim().removeSuffix(SPACE_COMPASS_SCENARIO_EXTENSION)
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.')
    require(safe.isNotBlank())
    return spaceCompassLimitFileText(safe,80,trimDelimiters=false) + SPACE_COMPASS_SCENARIO_EXTENSION
}
