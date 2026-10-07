package me.mondiversi.spacecompass

import sgp4.TLE
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.*

internal const val SPACE_COMPASS_ISS_MAX_AGE_MS = 72 * 3_600_000L
internal const val SPACE_COMPASS_ISS_WARNING_AGE_MS = 24 * 3_600_000L
internal const val SPACE_COMPASS_ISS_FUTURE_EPOCH_ALLOWANCE_MS = 6 * 3_600_000L

/** Vallado SGP4 produces TEME km. UTC GMST -> ECEF -> WGS84 local ENU, not a Kepler approximation. */
internal class SpaceCompassIssOrbit private constructor(val line1: String, val line2: String, override val epochMs: Long) : SpaceCompassSatelliteOrbit {
    private val tle = TLE(line1, line2)
    // TLE mean motion is in revolutions per UTC day; never assume a fixed 90-minute orbit.
    override val periodMs: Long get() = (86_400_000.0 / tle.n).roundToLong()
    override fun usable(timeMs: Long) = timeMs - epochMs in -SPACE_COMPASS_ISS_FUTURE_EPOCH_ALLOWANCE_MS..SPACE_COMPASS_ISS_MAX_AGE_MS
    @Synchronized
    private fun temeState(timeMs: Long): Array<DoubleArray> {
        val state = tle.getRV((timeMs - epochMs) / 60_000.0)
        require(tle.sgp4Error == 0 && state.size == 2 && state.all { it.size == 3 && it.all(Double::isFinite) } &&
            sqrt(state[0].sumOf { it * it }) in 6400.0..9000.0)
        require(spaceCompassCelestialVelocityMagnitude(state[1][0], state[1][1], state[1][2]) in 6.0..9.0)
        return state
    }
    override fun teme(timeMs: Long): DoubleArray = temeState(timeMs)[0]
    override fun speedKmPerSecond(timeMs: Long): Double = temeState(timeMs)[1].let {
        spaceCompassCelestialVelocityMagnitude(it[0], it[1], it[2])
    }
    companion object {
        fun parse(text: String): SpaceCompassIssOrbit {
            val lines = text.lineSequence().map { it.trimEnd() }.filter { it.startsWith("1 ") || it.startsWith("2 ") }.toList()
            require(lines.size == 2) { "Missing ISS orbital elements" }
            lines.forEachIndexed { index, line ->
                require(line.length == 69 && line[0] == ('1' + index) && line.substring(2, 7) == "25544")
                val checksum = line.take(68).sumOf { if (it.isDigit()) it.digitToInt() else if (it == '-') 1 else 0 } % 10
                require(line[68].digitToIntOrNull() == checksum) { "Invalid TLE checksum" }
            }
            require(lines[0][7] == 'U' && lines[0].substring(9, 17).trim() == "98067A" && lines[0][62] == '0')
            val yearShort = lines[0].substring(18, 20).toInt()
            val year = if (yearShort < 57) 2000 + yearShort else 1900 + yearShort
            val day = lines[0].substring(20, 32).toDouble()
            require(day >= 1 && day < LocalDate.of(year, 1, 1).lengthOfYear() + 1)
            val epoch = LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() +
                ((day - 1) * 86_400_000).roundToLong()
            val orbit = SpaceCompassIssOrbit(lines[0], lines[1], epoch)
            require(orbit.tle.parseErrors.isNullOrEmpty() && orbit.tle.n in 14.0..17.0 &&
                orbit.tle.ecc in 0.0..0.02 && orbit.tle.incDeg in 50.0..53.0)
            orbit.teme(epoch)
            return orbit
        }
    }
}
