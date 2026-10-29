package me.mondiversi.spacecompass

/** Normalize catalogue separators, never identify a host just by angular proximity. */
internal fun spaceCompassUnresolvedReferenceKey(value: String): String = value.lowercase(java.util.Locale.ROOT)
    .filter { it.isLetterOrDigit() }.replace(Regex("^(ngc|ic)0+([0-9]+)"),"$1$2")

private val unresolvedExoplanetHosts: Map<SpaceCompassCelestialBody,String> by lazy {
    buildMap {
        spaceCompassCelestialCatalogOrder.forEach { body ->
            val host = when (body) {
                SpaceCompassCelestialBody.PROXIMA_B,SpaceCompassCelestialBody.PROXIMA_CENTAURI -> "Proxima Cen"
                SpaceCompassCelestialBody.TRAPPIST_1_E -> "TRAPPIST-1"
                SpaceCompassCelestialBody.TOI_700_D -> "TOI-700"
                SpaceCompassCelestialBody.KEPLER_186_F -> "Kepler-186"
                else -> if (body.catalogType==SpaceCompassCatalogType.EXOPLANET) body.largeCatalogEntry?.facts?.parentName else null
            }
            host?.let { put(body,spaceCompassUnresolvedReferenceKey(it)) }
        }
    }
}

/** Host-star directions are not resolved planetary orbits or genuine close-approach events. */
internal fun spaceCompassSharesUnresolvedTarget(a: SpaceCompassCelestialBody,b: SpaceCompassCelestialBody): Boolean =
    spaceCompassSharesGalacticNucleus(a,b) || unresolvedExoplanetHosts[a]?.let { it==unresolvedExoplanetHosts[b] }==true
