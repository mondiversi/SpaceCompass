package me.mondiversi.spacecompass

import sgp4.TLE
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Validated NORAD identity plus SGP4/TEME propagation. No stale-epoch extrapolation. */
internal class SpaceCompassCatalogSatelliteOrbit private constructor(
    val body: SpaceCompassCelestialBody, val line1: String, val line2: String,
    override val epochMs: Long
) : SpaceCompassSatelliteOrbit {
    private val tle = TLE(line1, line2)
    override val periodMs: Long get() = (86_400_000.0 / tle.n).roundToLong()
    override fun usable(timeMs: Long) = timeMs - epochMs in
        -SPACE_COMPASS_ISS_FUTURE_EPOCH_ALLOWANCE_MS..SPACE_COMPASS_ISS_MAX_AGE_MS

    @Synchronized
    private fun state(timeMs: Long): Array<DoubleArray> {
        require(usable(timeMs)) { "Satellite elements do not cover the requested time" }
        val state = tle.getRV((timeMs - epochMs) / 60_000.0)
        require(tle.sgp4Error == 0 && state.size == 2 && state.all {
            it.size == 3 && it.all(Double::isFinite) })
        require(sqrt(state[0].sumOf { it * it }) in requireNotNull(body.catalogSatellite).radiusKm)
        require(spaceCompassCelestialVelocityMagnitude(state[1][0], state[1][1], state[1][2]) in requireNotNull(body.catalogSatellite).speedKmSecond)
        return state
    }
    override fun teme(timeMs: Long) = state(timeMs)[0]
    override fun speedKmPerSecond(timeMs: Long) = state(timeMs)[1].let {
        spaceCompassCelestialVelocityMagnitude(it[0], it[1], it[2]) }

    companion object {
        fun parse(body: SpaceCompassCelestialBody, text: String): SpaceCompassCatalogSatelliteOrbit {
            val identity = requireNotNull(body.catalogSatellite)
            val lines = text.lineSequence().map { it.trimEnd() }
                .filter { it.startsWith("1 ") || it.startsWith("2 ") }.toList()
            require(lines.size == 2) { "Missing or ambiguous orbital elements" }
            lines.forEachIndexed { index, line ->
                require(line.length == 69 && line[0] == ('1' + index) && line.substring(2, 7) == identity.norad)
                val checksum = line.take(68).sumOf {
                    if (it.isDigit()) it.digitToInt() else if (it == '-') 1 else 0 } % 10
                require(line[68].digitToIntOrNull() == checksum) { "Invalid TLE checksum" }
            }
            require(lines[0][7] == 'U' && lines[0].substring(9, 17).trim() == identity.international && lines[0][62] == '0')
            val shortYear = lines[0].substring(18, 20).toInt()
            val year = if (shortYear < 57) 2000 + shortYear else 1900 + shortYear
            val day = lines[0].substring(20, 32).toDouble()
            require(day.isFinite() && day >= 1 && day < LocalDate.of(year, 1, 1).lengthOfYear() + 1)
            val epoch = LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() +
                ((day - 1) * 86_400_000).roundToLong()
            val orbit = SpaceCompassCatalogSatelliteOrbit(body, lines[0], lines[1], epoch)
            require(orbit.tle.parseErrors.isNullOrEmpty() && orbit.tle.n in identity.meanMotion &&
                orbit.tle.ecc in 0.0..0.02 && orbit.tle.incDeg in identity.inclination)
            orbit.teme(epoch)
            return orbit
        }
    }
}

internal class SpaceCompassCatalogSatelliteSources(private val body: SpaceCompassCelestialBody) {
    private val identity = requireNotNull(body.catalogSatellite)
    private val sources = SpaceCompassSatelliteSources(
        "https://celestrak.org/NORAD/elements/gp.php?CATNR=${identity.norad}&FORMAT=TLE",
        "https://www.satcat.com/sats/${identity.norad}",
        { page -> spaceCompassSatcatTle(page, identity.publicName) }
    ) { text, now -> require(SpaceCompassCatalogSatelliteOrbit.parse(body, text).usable(now)) {
        "Satellite elements are too old" } }

    private var preferSatnogs = false
    private var satnogsStopped = false
    private suspend fun satnogs(now: Long, fetch: suspend (String, Int) -> String): String {
        check(!satnogsStopped) { "Satellite JSON provider stopped for this session" }
        try {
            val raw = fetch("https://db.satnogs.org/api/tle/?norad_cat_id=${identity.norad}", 65_536)
            val text = spaceCompassSatnogsTle(raw)
            require(SpaceCompassCatalogSatelliteOrbit.parse(body, text).usable(now)) { "Satellite elements are too old" }
            return text
        } catch (error: SpaceCompassCelestialHttpException) {
            satnogsStopped = true
            throw error
        }
    }
    suspend fun load(now: Long, fetch: suspend (String, Int) -> String): String {
        if (preferSatnogs) {
            try { return satnogs(now, fetch) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { /* Independently validated original providers remain available. */ }
        }
        try { return sources.load(now, fetch) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (original: Exception) {
            if (satnogsStopped) throw original
            return satnogs(now, fetch).also { preferSatnogs = true }
        }
    }
}
