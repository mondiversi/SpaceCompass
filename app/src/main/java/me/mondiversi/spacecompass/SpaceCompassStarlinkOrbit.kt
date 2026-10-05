package me.mondiversi.spacecompass

import sgp4.ElsetRec
import sgp4.SGP4
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.roundToLong
import kotlin.math.sqrt

internal const val SPACE_COMPASS_STARLINK_NORAD_ID = 100855
internal const val SPACE_COMPASS_STARLINK_OBJECT_NAME = "STARLINK-40083"
internal const val SPACE_COMPASS_STARLINK_OBJECT_ID = "2026-225A"
internal const val SPACE_COMPASS_STARLINK_OMM_URL = "https://celestrak.org/NORAD/elements/gp.php?CATNR=100855&FORMAT=CSV"

/** OMM-compatible CelesTrak CSV -> SGP4 directly, without lossy five-digit TLE conversion.
 * This is one identified spacecraft, not a generic Starlink constellation or fixed circular orbit.
 */
internal class SpaceCompassStarlinkOrbit private constructor(
    override val epochMs: Long, override val periodMs: Long, private val record: ElsetRec
) : SpaceCompassSatelliteOrbit {
    override fun usable(timeMs: Long) = timeMs - epochMs in -SPACE_COMPASS_ISS_FUTURE_EPOCH_ALLOWANCE_MS..SPACE_COMPASS_ISS_MAX_AGE_MS

    @Synchronized
    private fun state(timeMs: Long): Array<DoubleArray> {
        val r = DoubleArray(3); val v = DoubleArray(3)
        record.error = 0
        require(SGP4.sgp4(record, (timeMs - epochMs) / 60_000.0, r, v) && record.error == 0 &&
            r.all(Double::isFinite) && v.all(Double::isFinite) && sqrt(r.sumOf { it * it }) in 6400.0..9000.0 &&
            spaceCompassCelestialVelocityMagnitude(v[0], v[1], v[2]) in 6.0..9.0) { "Invalid Starlink SGP4 state" }
        return arrayOf(r, v)
    }
    override fun teme(timeMs: Long) = state(timeMs)[0]
    override fun speedKmPerSecond(timeMs: Long) = state(timeMs)[1].let {
        spaceCompassCelestialVelocityMagnitude(it[0], it[1], it[2])
    }

    companion object {
        fun parse(text: String): SpaceCompassStarlinkOrbit {
            require(text.length <= 65_536)
            val lines = text.removePrefix("\uFEFF").lineSequence().filter { it.isNotBlank() }.toList()
            require(lines.size == 2) { "Expected exactly one Starlink OMM record" }
            val headers = ommCsvFields(lines[0]); val fields = ommCsvFields(lines[1])
            require(headers.size == fields.size && headers.distinct().size == headers.size)
            val values = headers.zip(fields).toMap()
            fun field(key: String) = requireNotNull(values[key]) { "Missing OMM $key" }
            fun number(key: String) = field(key).toDouble().also { require(it.isFinite()) { "Invalid OMM $key" } }
            fun default(key: String, expected: String) {
                require(values[key] == null || values[key] == expected) { "Unsupported OMM $key" }
            }
            require(field("NORAD_CAT_ID").toInt() == SPACE_COMPASS_STARLINK_NORAD_ID &&
                field("OBJECT_NAME") == SPACE_COMPASS_STARLINK_OBJECT_NAME && field("OBJECT_ID") == SPACE_COMPASS_STARLINK_OBJECT_ID) {
                "Wrong Starlink identity"
            }
            // CelesTrak omits these redundant defaults in its GP CSV, as documented by the provider.
            default("CENTER_NAME", "EARTH"); default("REF_FRAME", "TEME")
            default("TIME_SYSTEM", "UTC"); default("MEAN_ELEMENT_THEORY", "SGP4")
            require(field("EPHEMERIS_TYPE").toInt() == 0 && field("CLASSIFICATION_TYPE") == "U")
            val epochText = field("EPOCH")
            val instant = if (epochText.endsWith("Z")) Instant.parse(epochText)
                else LocalDateTime.parse(epochText).toInstant(ZoneOffset.UTC)
            val utc = instant.atZone(ZoneOffset.UTC)
            require(utc.year in 1957..2099)
            val motion = number("MEAN_MOTION").also { require(it in 14.0..17.0) }
            val inclination = number("INCLINATION").also { require(it in 0.0..180.0) }
            val eccentricity = number("ECCENTRICITY").also { require(it in 0.0..0.02) }
            fun angle(key: String) = number(key).also { require(it >= 0.0 && it < 360.0) } * PI / 180.0
            val radiansPerRevolutionMinute = 1440.0 / (2 * PI)
            val rec = ElsetRec().apply {
                satID = SPACE_COMPASS_STARLINK_NORAD_ID.toString(); intldesg = SPACE_COMPASS_STARLINK_OBJECT_ID
                classification = 'U'; ephtype = 0
                elnum = field("ELEMENT_SET_NO").toLong().also { require(it >= 0) }
                revnum = field("REV_AT_EPOCH").toLong().also { require(it >= 0) }
                epochyr = utc.year % 100
                epochdays = utc.dayOfYear + (utc.hour * 3600 + utc.minute * 60 + utc.second + utc.nano / 1e9) / 86400.0
                val jd = SGP4.jday(utc.year, utc.monthValue, utc.dayOfMonth, utc.hour, utc.minute, utc.second + utc.nano / 1e9)
                jdsatepoch = jd[0]; jdsatepochF = jd[1]
                inclo = inclination * PI / 180.0; nodeo = angle("RA_OF_ASC_NODE")
                argpo = angle("ARG_OF_PERICENTER"); mo = angle("MEAN_ANOMALY"); ecco = eccentricity
                no_kozai = motion / radiansPerRevolutionMinute
                bstar = number("BSTAR").also { require(kotlin.math.abs(it) <= 1.0) }
                ndot = number("MEAN_MOTION_DOT") / (radiansPerRevolutionMinute * 1440.0)
                nddot = number("MEAN_MOTION_DDOT") / (radiansPerRevolutionMinute * 1440.0 * 1440.0)
            }
            require(SGP4.sgp4init('a', rec) && rec.error == 0) { "Invalid Starlink OMM elements" }
            return SpaceCompassStarlinkOrbit(instant.toEpochMilli(), (86_400_000 / motion).roundToLong(), rec).also {
                it.teme(it.epochMs)
            }
        }
    }
}

/** GP CSV has one physical line per record. Handle quoted fields and reject malformed/ambiguous input. */
private fun ommCsvFields(line: String): List<String> {
    val fields = mutableListOf<String>(); val field = StringBuilder()
    var quoted = false; var closed = false; var index = 0
    while (index < line.length) {
        val ch = line[index++]
        when {
            quoted && ch == '"' -> if (index < line.length && line[index] == '"') { field.append('"'); index++ }
                else { quoted = false; closed = true }
            quoted -> field.append(ch)
            ch == ',' -> { fields += field.toString().trim(); field.setLength(0); closed = false }
            ch == '"' -> { require(field.isEmpty() && !closed); quoted = true }
            else -> { require(!closed); field.append(ch) }
        }
    }
    require(!quoted)
    fields += field.toString().trim()
    return fields
}
