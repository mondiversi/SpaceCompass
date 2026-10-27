package me.mondiversi.spacecompass

/** The summary and badge count only saved, independently enabled override sections. */
internal val SpaceCompassObserverPlan.activeSections: List<SpaceCompassObserverSection>
    get() = buildList {
        if (simulatePosition) add(SpaceCompassObserverSection.POSITION)
        if (simulateAltitude) add(SpaceCompassObserverSection.ALTITUDE)
        if (simulateZone) add(SpaceCompassObserverSection.ZONE)
        if (simulateDate) add(SpaceCompassObserverSection.DATE)
        if (simulateTime) add(SpaceCompassObserverSection.TIME)
    }

internal val SpaceCompassObserverPlan?.activeSectionCount: Int
    get() = (this?.activeSections?.size ?: 0) + if (this?.scenarioClock?.settings?.enabled == true) 1 else 0
