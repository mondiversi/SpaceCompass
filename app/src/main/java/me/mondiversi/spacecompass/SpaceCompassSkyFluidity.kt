package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_SKY_FLUIDITY_KEY = "sky_fluidity"

/** Stable saved choices; Balanced is the default compromise between motion and battery use. */
internal enum class SpaceCompassSkyFluidity(val stored: String, val sensorPeriodUs: Int,
    val minimumUpdateIntervalNs: Long) {
    LOW("low", 66_667, 60_000_000L),
    BALANCED("balanced", 33_333, 30_000_000L),
    HIGH("high", 20_000, 16_666_667L);

    companion object {
        fun fromStored(value: String?): SpaceCompassSkyFluidity = entries.firstOrNull { it.stored == value } ?: BALANCED
    }
}
