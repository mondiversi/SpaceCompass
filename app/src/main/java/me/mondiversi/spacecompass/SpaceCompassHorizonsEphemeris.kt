package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Vector
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal data class SpaceCompassHorizonsSample(val timeMs: Long, val x: Double, val y: Double, val z: Double)

/** Apparent geocentric vectors, not spacecraft telemetry or a fabricated closed orbit. */
internal data class SpaceCompassHorizonsEphemeris(val body: SpaceCompassCelestialBody, val samples: List<SpaceCompassHorizonsSample>) {
    init {
        require(body.usesHorizons && samples.size >= 2)
        require(samples.zipWithNext().all { (a, b) -> a.timeMs < b.timeMs })
        require(samples.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
    }

    fun at(timeMs: Long): Vector? {
        if (timeMs !in samples.first().timeMs..samples.last().timeMs) return null
        val b = samples.indexOfFirst { it.timeMs >= timeMs }.coerceAtLeast(1)
        val a = samples[b - 1]
        val next = samples[b]
        val fraction = (timeMs - a.timeMs).toDouble() / (next.timeMs - a.timeMs)
        return Vector(a.x + (next.x - a.x) * fraction, a.y + (next.y - a.y) * fraction,
            a.z + (next.z - a.z) * fraction, spaceCompassAstronomyTime(timeMs))
    }
}

private data class SpaceCompassHorizonsTarget(val command: String, val header: String, val rangeAu: ClosedFloatingPointRange<Double>)
private fun spaceCompassHorizonsTarget(body: SpaceCompassCelestialBody) = when (body) {
    SpaceCompassCelestialBody.SEDNA -> SpaceCompassHorizonsTarget("90377;", "90377 Sedna", 60.0..120.0)
    SpaceCompassCelestialBody.VOYAGER_1 -> SpaceCompassHorizonsTarget("-31", "Voyager 1 (spacecraft) (-31)", 50.0..500.0)
    SpaceCompassCelestialBody.VOYAGER_2 -> SpaceCompassHorizonsTarget("-32", "Voyager 2 (spacecraft) (-32)", 50.0..500.0)
    else -> throw IllegalArgumentException("No Horizons target for $body")
}

/** Apparent pointing is Earth-centered; geometric motion is Sun-centered. Validate both explicitly. */
private fun validateSpaceCompassHorizonsHeader(body: SpaceCompassCelestialBody, text: String, geometric: Boolean) {
    val target = spaceCompassHorizonsTarget(body)
    require(Regex("(?m)^Target body name:\\s*${Regex.escape(target.header)}(?:\\s|\\(|$)").containsMatchIn(text))
    require(text.contains(if (geometric) "Center body name: Sun (10)" else "Center body name: Earth (399)") && text.contains("Output units    : AU-D") &&
        text.contains("Reference frame : ICRF") && text.contains(" UT "))
    require(text.contains(if (geometric) "Output type     : GEOMETRIC cartesian states" else "LT+S CORRECTED"))
    if (geometric) require(text.contains("Center-site name: BODY CENTER"))
}
private fun spaceCompassHorizonsRows(text: String): List<List<String>> {
    val start = text.indexOf("\$\$SOE")
    val end = text.indexOf("\$\$EOE")
    require(start >= 0 && end > start)
    return text.substring(start + 5, end).lineSequence().filter { it.isNotBlank() }
        .map { line -> line.split(',').map(String::trim) }.toList()
}
private fun spaceCompassHorizonsTime(columns: List<String>): Long {
    val jd = columns[0].toDouble()
    require(jd.isFinite() && jd in 2400000.0..2600000.0)
    return ((jd - 2440587.5) * 86_400_000).roundToLong()
}
private fun validateSpaceCompassHorizonsTimes(times: List<Long>) {
    require(times.size in 25..100 && times.zipWithNext().all { (a, b) -> b - a in 3_599_900..3_600_100 })
}
internal fun parseSpaceCompassHorizonsEphemeris(body: SpaceCompassCelestialBody, text: String): SpaceCompassHorizonsEphemeris {
    validateSpaceCompassHorizonsHeader(body, text, geometric = false)
    val target = spaceCompassHorizonsTarget(body)
    val samples = spaceCompassHorizonsRows(text).map { columns ->
        require(columns.size >= 5)
        val xyz = (2..4).map { columns[it].toDouble() }
        require(xyz.all { it.isFinite() } && sqrt(xyz.sumOf { it * it }) in target.rangeAu)
        SpaceCompassHorizonsSample(spaceCompassHorizonsTime(columns), xyz[0], xyz[1], xyz[2])
    }
    validateSpaceCompassHorizonsTimes(samples.map { it.timeMs })
    return SpaceCompassHorizonsEphemeris(body, samples)
}

/** Never reuse apparent or Earth-centered states as heliocentric orbital/outward speed. */
internal fun parseSpaceCompassHorizonsMotion(body: SpaceCompassCelestialBody, text: String): SpaceCompassHorizonsMotion {
    validateSpaceCompassHorizonsHeader(body, text, geometric = true)
    val target = spaceCompassHorizonsTarget(body)
    val samples = spaceCompassHorizonsRows(text).map { columns ->
        require(columns.size >= 8)
        val xyz = (2..4).map { columns[it].toDouble() }
        require(xyz.all { it.isFinite() } && sqrt(xyz.sumOf { it * it }) in target.rangeAu)
        SpaceCompassHorizonsStateSample(spaceCompassHorizonsTime(columns), xyz[0], xyz[1], xyz[2],
            columns[5].toDouble(), columns[6].toDouble(), columns[7].toDouble())
    }
    validateSpaceCompassHorizonsTimes(samples.map { it.timeMs })
    return SpaceCompassHorizonsMotion(body, samples)
}

/** Public center-based data only. No observer coordinates or device identifiers leave the phone. */
internal fun spaceCompassHorizonsUrl(body: SpaceCompassCelestialBody, now: Long, geometricMotion: Boolean = false): String {
    val day = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
    val values = linkedMapOf("format" to "text", "COMMAND" to "'${spaceCompassHorizonsTarget(body).command}'",
        "CENTER" to if (geometricMotion) "'500@10'" else "'500@399'", "MAKE_EPHEM" to "'YES'", "EPHEM_TYPE" to "'VECTORS'",
        "START_TIME" to "'$day'", "STOP_TIME" to "'${day.plusDays(3)}'", "STEP_SIZE" to "'1 h'",
        "REF_PLANE" to "'FRAME'", "REF_SYSTEM" to "'ICRF'", "VEC_CORR" to if (geometricMotion) "'NONE'" else "'LT+S'",
        "VEC_TABLE" to if (geometricMotion) "'2'" else "'1'", "OUT_UNITS" to "'AU-D'", "CSV_FORMAT" to "'YES'", "TIME_TYPE" to "'UT'")
    return "https://ssd.jpl.nasa.gov/api/horizons.api?" + values.entries.joinToString("&") {
        it.key + "=" + URLEncoder.encode(it.value, "UTF-8")
    }
}
