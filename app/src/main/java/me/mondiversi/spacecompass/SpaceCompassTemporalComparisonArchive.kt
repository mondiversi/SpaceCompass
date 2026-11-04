package me.mondiversi.spacecompass

import java.util.Properties

internal fun checkSpaceCompassComparisonPin(c: SpaceCompassTemporalComparison, pin: SpaceCompassSelectedOrbitPoint) {
    spaceCompassCheckLocationPoint(pin)
    require(c.body == pin.key.body && c.id == pin.key.comparisonId && c.zoneId == pin.key.zoneId)
    require(pin.key.date == c.date)
    if (pin.key.isCurrent) require(c.hasReference && pin.point.timeMs == c.timeMs)
}

internal fun encodeSpaceCompassComparisonPin(c: SpaceCompassTemporalComparison, pin: SpaceCompassSelectedOrbitPoint): String {
    checkSpaceCompassComparisonPin(c, pin)
    val plain = pin.copy(key = pin.key.copy(comparisonId = null), path = null)
    return Properties().apply {
        putSpaceCompassLocationPoints(listOf(plain), SpaceCompassCelestialSelection(setOf(c.body), c.body))
    }.getProperty("point.0")
}

internal fun decodeSpaceCompassComparisonPin(id: String, body: SpaceCompassCelestialBody, text: String): SpaceCompassSelectedOrbitPoint {
    val p = Properties().apply {
        setProperty("pointsIncluded", "true"); setProperty("pointsVersion", "1")
        setProperty("pointsCount", "1"); setProperty("point.0", text)
    }
    val pin = p.readSpaceCompassLocationPoints(SpaceCompassCelestialSelection(setOf(body), body))!!.single()
    return pin.copy(key = pin.key.copy(comparisonId = id))
}

/** Optional properties preserve v1 readers: legacy live objects/points keep their exact representation. */
internal fun Properties.putSpaceCompassComparisons(comparisons: List<SpaceCompassTemporalComparison>?,
    points: List<SpaceCompassSelectedOrbitPoint>?, activeId: String?, antipodes: Boolean?) {
    if (comparisons == null) return
    require(comparisons.size <= 512 && comparisons.distinctBy { it.id }.size == comparisons.size)
    require(activeId == null || comparisons.any { it.id == activeId })
    setProperty("comparisonsVersion", "1")
    setProperty("comparisonsCount", comparisons.size.toString())
    comparisons.forEachIndexed { i, c ->
        setProperty("comparison.$i", listOf(c.id, c.body.name, c.timeMs.toString(), c.zoneId,
            c.source.name, c.origin).joinToString("|"))
        setProperty("comparisonAnnual.$i", c.annualExtreme.toString())
    }
    activeId?.let { setProperty("activeComparison", it) }
    antipodes?.let { setProperty("antipodesVisible", it.toString()) }
    val pins = points.orEmpty().filter { it.key.comparisonId != null }
    require(pins.size <= 10_000)
    setProperty("comparisonPointsCount", pins.size.toString())
    pins.forEachIndexed { i, pin ->
        val c = comparisons.single { it.id == pin.key.comparisonId }
        setProperty("comparisonPoint.$i", c.id + "|" + encodeSpaceCompassComparisonPin(c, pin))
    }
}

internal fun Properties.readSpaceCompassComparisons(): List<SpaceCompassTemporalComparison>? {
    val version = getProperty("comparisonsVersion") ?: return null
    require(version == "1")
    val count = requireNotNull(getProperty("comparisonsCount")).toInt()
    require(count in 0..512)
    val result = (0 until count).map { i ->
        val f = requireNotNull(getProperty("comparison.$i")).split('|')
        require(f.size == 6)
        SpaceCompassTemporalComparison(f[0], SpaceCompassCelestialBody.valueOf(f[1]), f[2].toLong(), f[3],
            SpaceCompassComparisonSource.valueOf(f[4]), f[5], getProperty("comparisonAnnual.$i")?.toBooleanStrict() ?: false)
    }
    require(result.distinctBy { it.id }.size == result.size)
    require(getProperty("activeComparison")?.let { id -> result.any { it.id == id } } != false)
    getProperty("antipodesVisible")?.toBooleanStrict()
    return result
}

internal fun Properties.readSpaceCompassComparisonPoints(comparisons: List<SpaceCompassTemporalComparison>):
    List<SpaceCompassSelectedOrbitPoint> {
    val count = getProperty("comparisonPointsCount")?.toInt() ?: 0
    require(count in 0..10_000)
    val result = (0 until count).map { i ->
        val f = requireNotNull(getProperty("comparisonPoint.$i")).split('|', limit = 2)
        require(f.size == 2)
        val c = comparisons.single { it.id == f[0] }
        decodeSpaceCompassComparisonPin(c.id, c.body, f[1]).also { checkSpaceCompassComparisonPin(c, it) }
    }
    require(result.distinctBy { it.key }.size == result.size)
    return result
}
