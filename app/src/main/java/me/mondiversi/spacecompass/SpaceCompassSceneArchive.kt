package me.mondiversi.spacecompass

import java.io.StringReader
import java.time.ZoneId
import java.util.Base64
import java.util.Properties

internal const val SPACE_COMPASS_SCENE_EXTENSION = ".spacecompass-scene"

/** Only scene switches travel: device, camera, favorites and application preferences stay local. */
internal data class SpaceCompassSceneDisplay(val guides: Boolean, val forward24Hours: Boolean,
    val weather: Boolean, val translucentObjects: Boolean)

/** A distinct envelope prevents old Location readers from applying an absent observer configuration. */
internal data class SpaceCompassSceneArchive(val contents: SpaceCompassScenarioArchive,
    val locationIncluded: Boolean, val display: SpaceCompassSceneDisplay?, val capturedAtMs: Long) {
    fun encode(): String {
        require(contents.celestialSelection != null)
        val canonical = if (locationIncluded) contents else contents.withoutLocation()
        val values = sortedMapOf("format" to "space-compass-scene", "version" to "1",
            "locationIncluded" to locationIncluded.toString(), "capturedAtMs" to capturedAtMs.toString(),
            "contents" to Base64.getEncoder().encodeToString(canonical.encode().toByteArray(Charsets.UTF_8)))
        display?.let {
            values["guides"] = it.guides.toString(); values["forward24Hours"] = it.forward24Hours.toString()
            values["weather"] = it.weather.toString(); values["translucentObjects"] = it.translucentObjects.toString()
        }
        return values.entries.joinToString("\n", postfix="\n") { "${it.key}=${it.value}" }.also {
            require(it.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_SCENARIO_FILE_LIMIT)
        }
    }
    companion object {
        fun decode(text: String): SpaceCompassSceneArchive {
            require(text.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_SCENARIO_FILE_LIMIT)
            val p=Properties().apply { load(StringReader(text.removePrefix("\uFEFF"))) }
            // Previous versions stored optional celestial content in their Location document.
            if (p.getProperty("format")=="space-compass-scenario") {
                val legacy=SpaceCompassScenarioArchive.decode(text)
                require(legacy.celestialSelection != null)
                return SpaceCompassSceneArchive(legacy,true,null,legacy.configuration.timeMs)
            }
            require(p.getProperty("format")=="space-compass-scene" && p.getProperty("version")=="1")
            fun bool(key: String)=requireNotNull(p.getProperty(key)).toBooleanStrict()
            val encoded=requireNotNull(p.getProperty("contents"))
            val archive=SpaceCompassScenarioArchive.decode(Base64.getDecoder().decode(encoded).toString(Charsets.UTF_8))
            require(archive.celestialSelection != null)
            val include=bool("locationIncluded")
            if (!include) require(archive == archive.withoutLocation())
            val display=if (p.getProperty("guides")==null) {
                require(listOf("forward24Hours","weather","translucentObjects").none(p::containsKey));null
            } else SpaceCompassSceneDisplay(bool("guides"),bool("forward24Hours"),bool("weather"),bool("translucentObjects"))
            return SpaceCompassSceneArchive(archive,include,display,requireNotNull(p.getProperty("capturedAtMs")).toLong())
        }
    }
}

/** Discard celestial extensions when a legacy mixed file is deliberately imported as a Location. */
internal fun SpaceCompassScenarioArchive.locationOnly() = copy(celestialSelection=null,selectedOrbitPoints=null,
    comparisons=null,activeComparisonId=null,antipodesVisible=null,personalMoments=null)

/** No dormant coordinates, clock settings or inferred observer metadata enter a scene without Location. */
internal fun SpaceCompassScenarioArchive.withoutLocation() = copy(configuration=SpaceCompassObserverPlan(
    0.0,0.0,0.0,0L,"UTC",simulatePosition=false,simulateTime=false,simulateAltitude=false,
    simulateZone=false,simulateDate=false,momentZoneId="UTC"),timeSettings=SpaceCompassScenarioTimeSettings(),
    temporalSettingsIncluded=false)

internal fun spaceCompassSceneFileName(name: String) =
    spaceCompassScenarioFileName(name.removeSuffix(SPACE_COMPASS_SCENE_EXTENSION))
        .removeSuffix(SPACE_COMPASS_SCENARIO_EXTENSION)+SPACE_COMPASS_SCENE_EXTENSION

/** Resolve automatic values once; subsequent export checkbox/profile changes reuse that same instant. */
internal data class SpaceCompassConfigurationExport(val configured: SpaceCompassScenarioArchive,
    val complete: SpaceCompassScenarioArchive?, val capturedAtMs: Long, val display: SpaceCompassSceneDisplay? = null) {
    fun location(includeAutomatic: Boolean) = if (includeAutomatic) complete else configured
    fun scene(includeLocation: Boolean, includeAutomatic: Boolean = true): SpaceCompassSceneArchive? {
        val value=when {
            !includeLocation -> configured.withoutLocation()
            includeAutomatic -> complete ?: return null
            else -> configured
        }
        return SpaceCompassSceneArchive(value,includeLocation,display,capturedAtMs)
    }
}

internal fun spaceCompassCaptureLocationExport(observer: SpaceCompassObserverState, name: String,
    now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): SpaceCompassConfigurationExport {
    val configured=SpaceCompassScenarioArchive.capture(observer,name,now,zone).locationOnly()
    val effective=spaceCompassScenarioCurrentValues(observer.plan,observer.devicePlace,observer.timeSettings,now,zone)
    return SpaceCompassConfigurationExport(configured,configured.withAutomaticValues(effective),effective.instant)
}

/** Incoming checked moments win; unshared definitions remain available, but unchecked, on this device. */
internal fun spaceCompassScenePersonalMoments(local: List<SpaceCompassPersonalMoment>,
    incoming: List<SpaceCompassPersonalMoment>): List<SpaceCompassPersonalMoment> =
    (incoming + local.map { it.copy(enabled=false) }).distinctBy { it.id }
        .also { require(it.size <= SPACE_COMPASS_PERSONAL_MOMENT_LIMIT) }
