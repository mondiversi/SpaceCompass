package me.mondiversi.spacecompass

/** A galaxy centre and its own nucleus are persistent co-located catalogue targets,
 * not two bodies coming into conjunction. Other line-of-sight encounters remain eligible. */
private val SpaceCompassCelestialBody.galacticNucleus: SpaceCompassCelestialBody? get() = when (this) {
    SpaceCompassCelestialBody.ANDROMEDA_GALAXY, SpaceCompassCelestialBody.ANDROMEDA_CORE -> SpaceCompassCelestialBody.ANDROMEDA_CORE
    SpaceCompassCelestialBody.M87_GALAXY, SpaceCompassCelestialBody.M87 -> SpaceCompassCelestialBody.M87
    SpaceCompassCelestialBody.MILKY_WAY_CENTER, SpaceCompassCelestialBody.SAGITTARIUS_A -> SpaceCompassCelestialBody.SAGITTARIUS_A
    SpaceCompassCelestialBody.M51, SpaceCompassCelestialBody.M51_CORE -> SpaceCompassCelestialBody.M51_CORE
    else -> null
}

internal fun spaceCompassSharesGalacticNucleus(a: SpaceCompassCelestialBody, b: SpaceCompassCelestialBody): Boolean =
    a.galacticNucleus?.let { it == b.galacticNucleus } == true
