package me.mondiversi.spacecompass

internal val SpaceCompassObserverDraft.enabledOverrideCount: Int
    get() = listOf(simulatePosition, simulateAltitude, simulateZone, simulateDate, simulateTime).count { it }

/** Bulk activation changes only the five main switches; custom values and clock progression survive. */
internal fun SpaceCompassObserverDraft.withAllOverrides(enabled: Boolean): SpaceCompassObserverDraft = copy(
    simulatePosition = enabled, simulateAltitude = enabled, simulateZone = enabled,
    simulateDate = enabled, simulateTime = enabled)
