package me.mondiversi.spacecompass

import kotlin.math.round

internal const val SPACE_COMPASS_TRAVEL_SPEED_KEY = "observatory_travel_speed"
internal const val SPACE_COMPASS_TRAVEL_LIGHT_KM_S = 299_792.458
internal const val SPACE_COMPASS_TRAVEL_YEAR_SECONDS = 31_557_600.0

internal data class SpaceCompassTravelSpeed(val key: String, val kmPerSecond: Double,
    val nameResource: Int? = null, val lightFraction: Double? = null, val warp: Double? = null)

/** Equal slider steps are deliberately nonlinear in physical speed. Ordinary modes are illustrative. */
internal val spaceCompassTravelSpeeds = listOf(
    SpaceCompassTravelSpeed("walk", 5.0 / 3600, R.string.travel_walking),
    SpaceCompassTravelSpeed("bicycle", 20.0 / 3600, R.string.travel_bicycle),
    SpaceCompassTravelSpeed("car", 100.0 / 3600, R.string.travel_car),
    SpaceCompassTravelSpeed("aircraft", 900.0 / 3600, R.string.travel_aircraft),
    SpaceCompassTravelSpeed("rocket", 28_000.0 / 3600, R.string.travel_rocket),
    SpaceCompassTravelSpeed("probe", 200_000.0 / 3600, R.string.travel_probe),
    SpaceCompassTravelSpeed("light-0.01", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * .01, lightFraction = .01),
    SpaceCompassTravelSpeed("light-0.1", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * .1, lightFraction = .1),
    SpaceCompassTravelSpeed("light", SPACE_COMPASS_TRAVEL_LIGHT_KM_S, R.string.travel_light),
    // Rounded TNG comparison factors; no physical warp model or interpolation is asserted.
    SpaceCompassTravelSpeed("warp-2", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 10, warp = 2.0),
    SpaceCompassTravelSpeed("warp-3", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 39, warp = 3.0),
    SpaceCompassTravelSpeed("warp-5", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 214, warp = 5.0),
    SpaceCompassTravelSpeed("warp-7", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 656, warp = 7.0),
    SpaceCompassTravelSpeed("warp-9", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 1516, warp = 9.0),
    SpaceCompassTravelSpeed("warp-9.9", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 3053, warp = 9.9),
    SpaceCompassTravelSpeed("warp-9.99", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 7912, warp = 9.99),
    // Approximate comparison from The Star Trek Book, p. 266; published fictional scales vary.
    SpaceCompassTravelSpeed("warp-9.999", SPACE_COMPASS_TRAVEL_LIGHT_KM_S * 200_000, warp = 9.999)
)

internal fun spaceCompassRestoreTravelSpeed(key: String?): Int =
    spaceCompassTravelSpeeds.indexOfFirst { it.key == key }.coerceAtLeast(0)

/** Fiction requires an explicitly supplied reference estimate; direction-only data is not a range. */
internal fun spaceCompassTravelDistanceKm(body: SpaceCompassCelestialBody, distanceKm: Double?,
    referenceEstimateKm: Double? = null): Double? =
    if (body.isFictional) referenceEstimateKm?.takeIf { it.isFinite() && it >= 0 }
    else distanceKm?.takeIf { body.catalogType != SpaceCompassCatalogType.CONSTELLATION &&
        body.deepSkyReference?.hasMeasuredDistance != false && it.isFinite() && it >= 0 }

internal fun spaceCompassTravelSeconds(distanceKm: Double?, speed: SpaceCompassTravelSpeed): Double? =
    distanceKm?.takeIf { it.isFinite() && it >= 0 && speed.kmPerSecond.isFinite() && speed.kmPerSecond > 0 }
        ?.let { (it / speed.kmPerSecond).takeIf(Double::isFinite) }

internal enum class SpaceCompassTravelTimeUnit(val seconds: Double, val symbol: String) {
    NANOSECOND(1e-9, "ns"), MICROSECOND(1e-6, "µs"), MILLISECOND(.001, "ms"), SECOND(1.0, "s"),
    MINUTE(60.0, "min"), HOUR(3600.0, "h"), DAY(86400.0, "d"),
    MONTH(SPACE_COMPASS_TRAVEL_YEAR_SECONDS / 12, "mo"), YEAR(SPACE_COMPASS_TRAVEL_YEAR_SECONDS, "y")
}
internal data class SpaceCompassTravelDuration(val amount: Double, val unit: SpaceCompassTravelTimeUnit)

/** Promote rounded boundary values and retain finite, very long estimates without Long overflow. */
internal fun spaceCompassTravelDuration(seconds: Double?): SpaceCompassTravelDuration? {
    if (seconds == null || !seconds.isFinite() || seconds < 0) return null
    if (seconds == 0.0) return SpaceCompassTravelDuration(0.0, SpaceCompassTravelTimeUnit.SECOND)
    val units = SpaceCompassTravelTimeUnit.entries
    var index = units.indexOfLast { seconds >= it.seconds }.coerceAtLeast(0)
    var amount = seconds / units[index].seconds
    if (index < units.lastIndex && round(amount * 100) / 100 >= units[index + 1].seconds / units[index].seconds) {
        index++
        amount = seconds / units[index].seconds
    }
    return SpaceCompassTravelDuration(amount, units[index])
}
