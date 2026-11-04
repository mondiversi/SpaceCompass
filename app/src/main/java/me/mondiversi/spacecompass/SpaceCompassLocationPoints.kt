package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Properties
import kotlin.math.abs

private const val locationPointLimit = 10_000
private val locationPointBodies by lazy { spaceCompassAllCelestialOrder.associateBy { it.name } }

/** Canonical UTC instants and stable IDs keep pins independent of export language and numeric units. */
internal fun spaceCompassCheckLocationPoint(saved: SpaceCompassSelectedOrbitPoint) {
    val key = saved.key
    val point = saved.point
    val zone = ZoneId.of(key.zoneId)
    val date = Instant.ofEpochMilli(point.timeMs).atZone(zone).toLocalDate()
    require(key.body.name in locationPointBodies && key.date.year in 1899..2101 && date.year in 1899..2101)
    require(abs(ChronoUnit.DAYS.between(key.date, date)) <= 1)
    if (key.isCurrent) require(key.date == date)
    require(point.position.azimuthDegrees.isFinite() && point.position.azimuthDegrees in 0.0..360.0)
    require(point.position.elevationDegrees.isFinite() && point.position.elevationDegrees in -90.0..90.0)
    point.approach?.let {
        require(it.other.name in locationPointBodies && it.other != key.body &&
            it.separationDegrees.isFinite() && it.separationDegrees in 0.0..180.0)
    }
    require(key == spaceCompassOrbitPointKey(key.body, point, zone, key.date, current = key.isCurrent, comparisonId = key.comparisonId))
}

/** Optional v1 extension: earlier apps ignore these properties and retain the original Location schema. */
internal fun Properties.putSpaceCompassLocationPoints(points: List<SpaceCompassSelectedOrbitPoint>?,
    selection: SpaceCompassCelestialSelection?) {
    if (points == null) return
    require(selection != null && points.size <= locationPointLimit)
    require(points.map { it.key }.toSet().size == points.size)
    setProperty("pointsIncluded", "true")
    setProperty("pointsVersion", "1")
    setProperty("pointsCount", points.size.toString())
    points.forEachIndexed { index, saved ->
        spaceCompassCheckLocationPoint(saved)
        require(saved.key.body in selection.selected)
        val p = saved.point
        setProperty("point.$index", listOf(saved.key.body.name, saved.key.date.toString(), saved.key.zoneId,
            p.timeMs.toString(), p.position.azimuthDegrees.toString(), p.position.elevationDegrees.toString(),
            saved.key.isCurrent.toString(), p.event.name, p.coincidentEvents.sortedBy { it.ordinal }.joinToString("+") { it.name },
            p.moonPhase?.angleDegrees?.toString().orEmpty(), p.moonPhase?.illuminatedFraction?.toString().orEmpty(),
            p.approach?.other?.name.orEmpty(), p.approach?.separationDegrees?.toString().orEmpty()).joinToString(","))
    }
}

internal fun Properties.readSpaceCompassLocationPoints(selection: SpaceCompassCelestialSelection?):
    List<SpaceCompassSelectedOrbitPoint>? {
    val included = getProperty("pointsIncluded")?.toBooleanStrict() ?: return null
    if (!included) return null
    require(selection != null && getProperty("pointsVersion") == "1")
    val count = requireNotNull(getProperty("pointsCount")).toInt()
    require(count in 0..locationPointLimit)
    val points = (0 until count).mapNotNull { index ->
        val value = requireNotNull(getProperty("point.$index"))
        require(value.length <= 2048)
        val f = value.split(',')
        require(f.size == 13)
        val body = locationPointBodies[f[0]] ?: return@mapNotNull null
        if (body !in selection.selected) return@mapNotNull null
        val date = LocalDate.parse(f[1])
        val zone = ZoneId.of(f[2])
        val current = f[6].toBooleanStrict()
        val phase = if (f[9].isEmpty() && f[10].isEmpty()) null
            else SpaceCompassMoonPhase(f[9].toDouble(), f[10].toDouble())
        val approach = if (f[11].isEmpty() && f[12].isEmpty()) null else {
            val other = locationPointBodies[f[11]] ?: return@mapNotNull null
            SpaceCompassApproach(other, f[12].toDouble())
        }
        val point = SpaceCompassSunPathPoint(f[3].toLong(), SpaceCompassSunPosition(f[4].toDouble(), f[5].toDouble()),
            SpaceCompassSunPathEvent.valueOf(f[7]), phase,
            f[8].takeIf { it.isNotEmpty() }?.split('+')?.map { SpaceCompassSunPathEvent.valueOf(it) }?.toSet().orEmpty(), approach)
        SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(body, point, zone, date, current), point, null)
            .also(::spaceCompassCheckLocationPoint)
    }
    require(points.map { it.key }.toSet().size == points.size)
    return points
}

/** Strip live path caches and omit points whose object is not part of the exported scene. */
internal fun SpaceCompassScenarioArchive.withCelestialObjects(selection: SpaceCompassCelestialSelection,
    points: List<SpaceCompassSelectedOrbitPoint>, comparisons: List<SpaceCompassTemporalComparison>? = null,
    activeComparisonId: String? = null, antipodesVisible: Boolean? = null,
    personalMoments: List<SpaceCompassPersonalMoment>? = null): SpaceCompassScenarioArchive = copy(celestialSelection = selection,
        comparisons = comparisons, activeComparisonId = activeComparisonId, antipodesVisible = antipodesVisible,
        personalMoments = personalMoments?.filter { it.enabled },
        selectedOrbitPoints = points.filter { saved ->
            if (saved.key.comparisonId == null) saved.key.body in selection.selected
            else comparisons.orEmpty().any { it.id == saved.key.comparisonId && it.body == saved.key.body } ||
                personalMoments.orEmpty().any { it.enabled && it.id == saved.key.comparisonId && it.body == saved.key.body }
        }.map { it.copy(path = null) })
