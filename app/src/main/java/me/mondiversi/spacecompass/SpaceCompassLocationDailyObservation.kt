package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

/** The Location clock is independent of an Observatory comparison's frozen viewing instant. */
internal data class SpaceCompassLocationDailyObservation(val timeMs: Long, val zone: ZoneId,
    val overlay: SpaceCompassCelestialOverlay? = null)

internal fun spaceCompassLocationDailyOverlay(body: SpaceCompassCelestialBody,
    overlay: SpaceCompassCelestialOverlay?): SpaceCompassCelestialOverlay? =
    overlay?.takeIf { it.body == body && it.path?.comparison == null }

/** Never fill a refreshing live table with a frozen comparison or a different local day/zone. */
internal fun spaceCompassLocationDailyPath(body: SpaceCompassCelestialBody, timeMs: Long, zone: ZoneId,
    forward24Hours: Boolean, calculated: SpaceCompassSunDailyPath?, retained: SpaceCompassSunDailyPath?): SpaceCompassSunDailyPath? {
    val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    fun accepts(path: SpaceCompassSunDailyPath) = path.body == body && path.comparison == null &&
        path.zone == zone && path.forward24Hours == forward24Hours && if (forward24Hours)
        path.samples.isNotEmpty() && timeMs in path.samples.first().timeMs..path.samples.last().timeMs else path.date == date
    return calculated?.takeIf(::accepts) ?: retained?.takeIf(::accepts)
}
