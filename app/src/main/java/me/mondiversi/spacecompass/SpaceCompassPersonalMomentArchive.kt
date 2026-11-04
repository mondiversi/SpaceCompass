package me.mondiversi.spacecompass

import java.io.StringReader
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Properties

internal const val SPACE_COMPASS_PERSONAL_MOMENTS_EXTENSION = ".spacecompass-moments"
internal const val SPACE_COMPASS_PERSONAL_MOMENTS_FILE_LIMIT = 512 * 1024
private const val PERSONAL_MOMENTS_FORMAT = "space-compass-personal-moments"

/** A portable, object-scoped file of personal definitions and their checkboxes. */
internal data class SpaceCompassPersonalMomentsFile(val body: SpaceCompassCelestialBody,
    val moments: List<SpaceCompassPersonalMoment>) {
    init { require(moments.isNotEmpty() && moments.all { it.body == body }) }

    fun encode(): String {
        val properties = Properties().apply {
            setProperty("format", PERSONAL_MOMENTS_FORMAT)
            setProperty("object", body.name)
            putPersonalMomentDefinitions(moments)
        }
        val text = StringWriter().also { properties.store(it, null) }.toString()
            .lineSequence().filter { it.isNotEmpty() && !it.startsWith('#') }.sorted().joinToString("\n", postfix = "\n")
        require(text.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_PERSONAL_MOMENTS_FILE_LIMIT)
        return text
    }

    companion object {
        fun decode(text: String): SpaceCompassPersonalMomentsFile {
            require(text.toByteArray(Charsets.UTF_8).size <= SPACE_COMPASS_PERSONAL_MOMENTS_FILE_LIMIT)
            val properties = Properties().apply { load(StringReader(text.removePrefix("\uFEFF"))) }
            require(properties.getProperty("format") == PERSONAL_MOMENTS_FORMAT)
            val body = SpaceCompassCelestialBody.valueOf(requireNotNull(properties.getProperty("object")))
            return SpaceCompassPersonalMomentsFile(body, requireNotNull(properties.readSpaceCompassPersonalMoments()))
        }
    }
}

/** A separate optional extension lets older readers continue to open the base Location. */
internal fun Properties.putSpaceCompassPersonalMoments(moments: List<SpaceCompassPersonalMoment>?,
    points: List<SpaceCompassSelectedOrbitPoint>?, activeId: String?) {
    if (moments == null) return
    putPersonalMomentDefinitions(moments)
    val byId = moments.associateBy { it.id }
    activeId?.takeIf { byId[it]?.enabled == true }?.let { setProperty("activePersonalMoment", it) }
    val pins = points.orEmpty().filter { it.key.comparisonId in byId }
    require(pins.size <= 10_000 && pins.distinctBy { it.key }.size == pins.size)
    setProperty("personalPointsCount", pins.size.toString())
    pins.forEachIndexed { index, pin ->
        val moment = byId.getValue(requireNotNull(pin.key.comparisonId))
        require(moment.enabled)
        val comparison = moment.resolve(ZoneId.of(pin.key.zoneId))
        setProperty("personalPoint.$index", moment.id + "|" + encodeSpaceCompassComparisonPin(comparison, pin))
    }
}

private fun Properties.putPersonalMomentDefinitions(moments: List<SpaceCompassPersonalMoment>) {
    require(moments.size <= SPACE_COMPASS_PERSONAL_MOMENT_LIMIT && moments.distinctBy { it.id }.size == moments.size)
    setProperty("personalMomentsVersion", "1")
    setProperty("personalMomentsCount", moments.size.toString())
    moments.forEachIndexed { index, moment ->
        setProperty("personalMoment.$index", listOf(moment.body.name, moment.date.toString(),
            moment.time?.toString().orEmpty(), moment.enabled.toString(), moment.name).joinToString("|"))
    }
}

internal fun Properties.readSpaceCompassPersonalMoments(): List<SpaceCompassPersonalMoment>? {
    val version = getProperty("personalMomentsVersion") ?: return null
    require(version == "1")
    val count = requireNotNull(getProperty("personalMomentsCount")).toInt()
    require(count in 0..SPACE_COMPASS_PERSONAL_MOMENT_LIMIT)
    val moments = (0 until count).map { index ->
        val fields = requireNotNull(getProperty("personalMoment.$index")).split('|', limit = 5)
        require(fields.size == 5)
        SpaceCompassPersonalMoment(SpaceCompassCelestialBody.valueOf(fields[0]), fields[4], LocalDate.parse(fields[1]),
            fields[2].takeIf { it.isNotEmpty() }?.let(LocalTime::parse), fields[3].toBooleanStrict())
    }
    require(moments.distinctBy { it.id }.size == count)
    require(getProperty("activePersonalMoment")?.let { id -> moments.any { it.id == id && it.enabled } } != false)
    return moments
}

internal fun Properties.readSpaceCompassPersonalPins(moments: List<SpaceCompassPersonalMoment>):
    List<SpaceCompassSelectedOrbitPoint> {
    val count = getProperty("personalPointsCount")?.toInt() ?: 0
    require(count in 0..10_000)
    val byId = moments.filter { it.enabled }.associateBy { it.id }
    return (0 until count).map { index ->
        val fields = requireNotNull(getProperty("personalPoint.$index")).split('|', limit = 2)
        require(fields.size == 2)
        val moment = byId.getValue(fields[0])
        val pin = decodeSpaceCompassComparisonPin(moment.id, moment.body, fields[1])
        checkSpaceCompassComparisonPin(moment.resolve(ZoneId.of(pin.key.zoneId)), pin)
        pin
    }.also { pins -> require(pins.distinctBy { it.key }.size == count) }
}
