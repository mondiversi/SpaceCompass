package me.mondiversi.spacecompass

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.pow

internal const val SPACE_COMPASS_STARLINK_SATCAT_URL = "https://www.satcat.com/sats/100855"
internal const val SPACE_COMPASS_SATCAT_MAX_BYTES = 512 * 1024

/** Public, copyable Space-Track TLE on Satcat; no login, private endpoints or JS execution.
 * A0855 is the complete Alpha-5 representation of 100855, not a truncated ID.
 * Normalize into the same independently validated SGP4 CSV/cache as the primary provider. */
internal fun spaceCompassSatcatStarlinkCsv(page: String): String {
    return spaceCompassStarlinkTleCsv(spaceCompassSatcatTle(page, SPACE_COMPASS_STARLINK_OBJECT_NAME))
}

internal fun spaceCompassStarlinkTleCsv(text: String): String {
    val lines = text.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
    require(lines.size == 3 && lines[0] == "0 $SPACE_COMPASS_STARLINK_OBJECT_NAME")
    val a = lines[1]; val b = lines[2]
    for ((index, line) in listOf(a, b).withIndex()) {
        require(line.length == 69 && line[0] == ('1' + index) && line.substring(2, 7) == "A0855") { "Wrong Starlink Alpha-5 identity" }
        val checksum = line.take(68).sumOf { if (it in '0'..'9') it - '0' else if (it == '-') 1 else 0 } % 10
        require(line[68].digitToIntOrNull() == checksum) { "Invalid TLE checksum" }
    }
    require(a[7] == 'U' && a.substring(9,17).trim() == "26225A" && a[62] == '0')
    val yy = a.substring(18,20).toInt()
    val year = if (yy < 57) 2000 + yy else 1900 + yy
    val day = a.substring(20,32).toBigDecimal()
    require(day >= BigDecimal.ONE && day < BigDecimal(LocalDate.of(year,1,1).lengthOfYear()+1))
    val nanos = (day - BigDecimal.ONE).multiply(BigDecimal("86400000000000")).setScale(0, RoundingMode.HALF_UP).longValueExact()
    val epoch = LocalDate.of(year,1,1).atStartOfDay(ZoneOffset.UTC).toInstant().plusNanos(nanos)
    fun number(value: String) = value.trim().toDouble().also { require(it.isFinite()) }.toString()
    fun compact(value: String): String {
        require(Regex("[ +\\-][0-9]{5}[+\\-][0-9]").matches(value))
        val mantissa = ("0." + value.substring(1,6)).toDouble() * if (value[0] == '-') -1 else 1
        return (mantissa * 10.0.pow(value.substring(6).toInt())).toString()
    }
    val keys = "OBJECT_NAME,OBJECT_ID,EPOCH,MEAN_MOTION,ECCENTRICITY,INCLINATION,RA_OF_ASC_NODE,ARG_OF_PERICENTER,MEAN_ANOMALY,EPHEMERIS_TYPE,CLASSIFICATION_TYPE,NORAD_CAT_ID,ELEMENT_SET_NO,REV_AT_EPOCH,BSTAR,MEAN_MOTION_DOT,MEAN_MOTION_DDOT"
    val values = listOf(SPACE_COMPASS_STARLINK_OBJECT_NAME, SPACE_COMPASS_STARLINK_OBJECT_ID, epoch.toString(),
        number(b.substring(52,63)), number("0."+b.substring(26,33)), number(b.substring(8,16)),
        number(b.substring(17,25)), number(b.substring(34,42)), number(b.substring(43,51)), "0", "U",
        SPACE_COMPASS_STARLINK_NORAD_ID.toString(), a.substring(64,68).trim().toLong().toString(),
        b.substring(63,68).trim().toLong().toString(), compact(a.substring(53,61)),
        number(a.substring(33,43)), compact(a.substring(44,52)))
    return "$keys\n${values.joinToString(",")}\n".also { SpaceCompassStarlinkOrbit.parse(it) }
}

/** Same validated source handling as ISS, retaining the last successful provider. */
internal class SpaceCompassStarlinkSources {
    private val sources = SpaceCompassSatelliteSources(SPACE_COMPASS_STARLINK_OMM_URL,
        SPACE_COMPASS_STARLINK_SATCAT_URL, ::spaceCompassSatcatStarlinkCsv) { text, now ->
        require(SpaceCompassStarlinkOrbit.parse(text).usable(now)) { "Starlink elements are too old" }
    }
    suspend fun load(now: Long, fetch: suspend (String, Int) -> String): String = sources.load(now, fetch)
}
