package me.mondiversi.spacecompass

import java.util.TimeZone

internal data class SpaceCompassCelestialDetailSection(val title: String, val icon: String,
    val rows: List<SpaceCompassCelestialDetailRow>, val tag: String, val note: String? = null)

/** One table model for the on-screen observatory and both frozen export profiles. */
internal fun spaceCompassCelestialDetailSections(body: SpaceCompassCelestialBody,
    facts: SpaceCompassCelestialFacts, timeMs: Long, remote: SpaceCompassCelestialRemoteData,
    distance: Double?, phase: SpaceCompassMoonPhase?, illuminatedFraction: Double?,
    coordinates: SpaceCompassCelestialCoordinates?, resources: SpaceCompassTextResources, formatting: SpaceCompassPanoramaFormatting,
    units: SpaceCompassUnits): List<SpaceCompassCelestialDetailSection> {
    val numeric = formatting.numeric
    val displayDistance = distance?.takeIf { it.isFinite() && it >= 0 }
        ?: spaceCompassReferenceDistanceKm(body, timeMs, remote)
    val sections = mutableListOf<SpaceCompassCelestialDetailSection>()
    fun detailRow(label: Int, value: String, tag: String? = null) =
        SpaceCompassCelestialDetailRow(resources.getString(label), value, tag = tag)
    fun number(value: Double?, decimals: Int, unit: String) = value?.takeIf { it.isFinite() }
        ?.let { "${formatSpaceCompassNumber(it, decimals, numeric)} $unit" } ?: "—"
    val objectName = resources.getString(body.nameResource)
    val designation = body.scientificDesignation?.takeUnless { it.equals(objectName, ignoreCase = true) }
    val identity = listOf(SpaceCompassCelestialDetailRow(resources.getString(R.string.celestial_view_name),
        objectName, designation, "celestial-view-name")) + listOfNotNull(body.scientificTypeResource?.let {
        SpaceCompassCelestialDetailRow(resources.getString(R.string.celestial_view_classification),
            listOfNotNull(resources.getString(it), facts.spectralType).joinToString(" · "), tag = "celestial-view-classification")
    })
    val sourceRows = body.largeCatalogEntry?.let { listOf(detailRow(R.string.catalogue_source, it.sourceLabel)) }.orEmpty() +
        body.fictionalReference?.let { listOf(detailRow(R.string.scifi_universe, it.universe), detailRow(R.string.scifi_reference, it.referenceName)) }.orEmpty()
    sections += SpaceCompassCelestialDetailSection(resources.getString(R.string.celestial_section_identity), "identity", identity + sourceRows,
        "celestial-view-information-island")

    val physicalBody = !body.isExtendedSkyObject && body != SpaceCompassCelestialBody.EARTH_CENTER
    val volume = spaceCompassCelestialSphericalVolumeKm3(body, facts)
    if (physicalBody) {
        val physical = listOf(
            detailRow(R.string.celestial_view_diameter, formatSpaceCompassCelestialDiameter(facts, numeric, units.feet)),
        ) + listOfNotNull(volume?.let { detailRow(R.string.celestial_view_volume,
            formatSpaceCompassCelestialVolume(it, numeric, units.feet)!!) }) + listOf(
            detailRow(if (body.isVoyager) R.string.celestial_view_antenna else R.string.celestial_view_size,
                formatSpaceCompassPhysicalLength(facts.dimensionMeters, 1, numeric, units.feet)),
            detailRow(R.string.celestial_view_mass, formatSpaceCompassCelestialMass(body, facts, numeric, units.pounds)),
            detailRow(R.string.celestial_view_gravity, formatSpaceCompassCelestialGravity(facts.gravity, numeric,
                fractionDigits = if (body == SpaceCompassCelestialBody.POLARIS) 2 else 1, feet = units.feet),
                tag = "celestial-view-gravity"),
            detailRow(R.string.celestial_view_density, formatSpaceCompassCelestialDensity(facts.density, numeric, units,
                fractionDigits = if (body == SpaceCompassCelestialBody.POLARIS) 3 else 0))
        ) + listOfNotNull(spaceCompassHorizonDiameterKm(body)?.let { detailRow(R.string.celestial_horizon_diameter,
            "≈ " + formatSpaceCompassPhysicalLength(it * 1000, 0, numeric, units.feet, large = true)) })
        sections += SpaceCompassCelestialDetailSection(resources.getString(R.string.celestial_section_physical), SPACE_COMPASS_MASS_UNIT_KEY,
            physical, "celestial-view-physical-island",
            note = volume?.let { resources.getString(R.string.celestial_volume_note) })
    }

    val stellar = buildList {
        facts.apparentMagnitude?.let { add(detailRow(R.string.celestial_apparent_magnitude,
            formatSpaceCompassNumber(it, 2, numeric, minimumDigits = 0))) }
        facts.luminositySolar?.let { add(detailRow(R.string.celestial_luminosity,
            "≈ " + formatSpaceCompassNumber(it, if (it < 1) 4 else 0, numeric, minimumDigits = 0) + " L☉")) }
        facts.binaryPeriodDays?.let { add(SpaceCompassCelestialDetailRow(
            resources.getString(R.string.celestial_view_revolution_period) + " (" + when (body) {
                SpaceCompassCelestialBody.POLARIS -> "Aa–Ab"
                SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.SIRIUS -> "A–B"
                else -> "★–WD"
            } + ")", number(it, 3, "d"))) }
        for (component in spaceCompassStellarComponents(body)) {
            add(SpaceCompassCelestialDetailRow(resources.getString(R.string.celestial_luminosity) + " (${component.name})",
                formatSpaceCompassNumber(component.luminositySolar, 4, numeric, minimumDigits = 0) + " L☉"))
            add(SpaceCompassCelestialDetailRow(resources.getString(R.string.celestial_view_mass) + " (${component.name})",
                formatSpaceCompassCelestialMass(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialFacts(
                    massSolar = component.massSolar, massSolarError = component.massError), numeric, units.pounds)))
            add(SpaceCompassCelestialDetailRow(resources.getString(R.string.celestial_view_diameter) + " (${component.name})",
                formatSpaceCompassPhysicalLength(2 * component.radiusSolar * 695700000, 0, numeric, units.feet, large = true) +
                    " (±" + formatSpaceCompassPhysicalLength(2 * component.radiusError * 695700000, 0, numeric, units.feet, large = true) + ")"))
        }
    }
    sections += SpaceCompassCelestialDetailSection(resources.getString(R.string.celestial_section_stellar), "whats_new", stellar,
        "celestial-view-stellar-island")

    if (physicalBody) {
        val temperatures = spaceCompassCelestialTemperatures(body) +
            listOfNotNull(spaceCompassCelestialCoreTemperature(body)?.temperature)
        val referencePressure = formatSpaceCompassPressure(SpaceCompassPressureUnit.BAR.pascals, numeric, units.pressure)!!
        val atmosphere = buildList {
            spaceCompassAtmosphericPressure(body)?.let { reference ->
                formatSpaceCompassAtmosphericPressure(reference, numeric, units.pressure)?.let {
                    add(detailRow(reference.kind.labelResource, it))
                }
            }
            if (temperatures.isEmpty()) add(detailRow(R.string.celestial_temperature, "—"))
            for (temperature in temperatures) {
                val label = if (temperature.kind == SpaceCompassCelestialTemperatureKind.ATMOSPHERE_ONE_BAR)
                    resources.getString(temperature.kind.labelResource, referencePressure) else resources.getString(temperature.kind.labelResource)
                add(SpaceCompassCelestialDetailRow(label + (temperature.component?.let { " ($it)" } ?: ""),
                    formatSpaceCompassCelestialTemperature(temperature, numeric, units.fahrenheit)))
            }
        }
        sections += SpaceCompassCelestialDetailSection(resources.getString(R.string.celestial_section_atmosphere), "temperature", atmosphere,
            "celestial-view-atmosphere-island")
    }

    val observation = buildList {
        if (physicalBody) {
            add(detailRow(R.string.celestial_view_parent, when {
                facts.parentName != null -> facts.parentName
                facts.parentIsEarth -> resources.getString(R.string.celestial_view_earth)
                facts.parent != null -> resources.getString(facts.parent.nameResource)
                else -> "—"
            }))
            add(detailRow(R.string.celestial_view_rotation_period, formatSpaceCompassRotationPeriod(facts, numeric)))
            add(detailRow(R.string.celestial_view_retrograde, if (facts.rotationHours == null) "—" else
                resources.getString(if (facts.rotationHours < 0) R.string.celestial_view_yes else R.string.celestial_view_no)))
            add(detailRow(R.string.celestial_view_revolution_period, number(facts.revolutionDays ?:
                remote.satelliteOrbit(body)?.takeIf { it.usable(timeMs) }?.periodMs?.div(86_400_000.0), 4, "d")))
            add(detailRow(R.string.celestial_view_min_distance,
                formatSpaceCompassSelectedDistance(body, facts.minimumParentKm, numeric, units.distance, units.feet) ?: "—"))
            add(detailRow(R.string.celestial_view_max_distance,
                formatSpaceCompassSelectedDistance(body, facts.maximumParentKm, numeric, units.distance, units.feet) ?: "—"))
        }
        add(detailRow(if (body.isFictional) R.string.scifi_estimated_distance else R.string.celestial_view_observer_distance,
            formatSpaceCompassSelectedDistance(body, displayDistance, numeric, units.distance, units.feet) ?: "—"))
        if (physicalBody) {
            add(detailRow(R.string.celestial_view_phase, phase?.let { resources.getString(it.kind.nameResource) } ?: "—"))
            add(detailRow(R.string.celestial_view_illuminated, number(
                (illuminatedFraction ?: phase?.illuminatedFraction)?.times(100), 1, "%")))
        }
    }
    sections += SpaceCompassCelestialDetailSection(resources.getString(R.string.celestial_section_orbit), "distance", observation,
        "celestial-view-orbit-island", body.fictionalReference?.let {
            resources.getString(R.string.scifi_distance_reference_note, it.referenceName)
        })

    sections += spaceCompassCelestialSpatialSection(body, coordinates, resources, formatting, units)
    return sections
}

private fun spaceCompassCelestialSpatialSection(body: SpaceCompassCelestialBody,
    coordinates: SpaceCompassCelestialCoordinates?, resources: SpaceCompassTextResources, formatting: SpaceCompassPanoramaFormatting,
    units: SpaceCompassUnits): SpaceCompassCelestialDetailSection {
    val membership = spaceCompassCelestialMembership(body)
    val numeric = formatting.numeric
    fun detailRow(label: Int, value: String, tag: String? = null) =
        SpaceCompassCelestialDetailRow(resources.getString(label), value, tag = tag)
    val rows = buildList {
        (membership.systemResource?.let { resources.getString(it) } ?: membership.systemDesignation)?.let {
            add(detailRow(R.string.celestial_spatial_system, it))
        }
        add(detailRow(R.string.celestial_spatial_galaxy, membership.galaxyDesignation ?: when (membership.galaxy) {
            SpaceCompassCelestialGalaxy.UNKNOWN, SpaceCompassCelestialGalaxy.NONE -> "—"
            SpaceCompassCelestialGalaxy.LMC -> resources.getString(R.string.celestial_large_magellanic_cloud)
            SpaceCompassCelestialGalaxy.SMC -> resources.getString(R.string.celestial_small_magellanic_cloud)
            SpaceCompassCelestialGalaxy.M51 -> resources.getString(R.string.celestial_m51)
            SpaceCompassCelestialGalaxy.M87 -> "Messier 87 · NGC 4486"
            SpaceCompassCelestialGalaxy.MILKY_WAY -> resources.getString(R.string.celestial_spatial_milky_way)
            SpaceCompassCelestialGalaxy.ANDROMEDA -> resources.getString(R.string.celestial_andromeda_galaxy_title) + " · M31"
            SpaceCompassCelestialGalaxy.TON_618_HOST -> resources.getString(R.string.celestial_spatial_host_galaxy, "TON 618")
        }))
        add(detailRow(R.string.celestial_spatial_ra, coordinates?.let {
            formatSpaceCompassRightAscension(it.rightAscensionHours, numeric, units.dms) } ?: "—"))
        add(detailRow(R.string.celestial_spatial_dec, coordinates?.let {
            formatSpaceCompassDeclination(it.declinationDegrees, numeric, units.dms) } ?: "—"))
        coordinates?.let {
            add(detailRow(R.string.celestial_spatial_reference, it.frame.reference))
            it.timeMs?.let { instant -> add(detailRow(R.string.celestial_spatial_instant,
                formatSpaceCompassDateTime(instant, formatting.date,
                    formatting.locale, TimeZone.getTimeZone("UTC"), separator = "\n",
                    timeFormat = formatting.time, numeric = numeric,
                    deviceLocale = formatting.deviceLocale) + " UTC")) }
        }
    }
    val note = listOfNotNull(coordinates?.let { resources.getString(when (it.frame) {
        SpaceCompassCelestialCoordinateFrame.CATALOG_ICRS -> R.string.celestial_spatial_catalog_note
        SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_TEME -> R.string.celestial_spatial_teme_note
        else -> R.string.celestial_spatial_geocentric_note
    }) }, when {
        body.isFictional -> resources.getString(R.string.scifi_reference_note)
        body == SpaceCompassCelestialBody.TRAPPIST_1_E -> resources.getString(R.string.celestial_spatial_host_note)
        else -> null
    }).joinToString("\n").ifEmpty { null }
    return SpaceCompassCelestialDetailSection(resources.getString(R.string.celestial_section_spatial), "coordinates", rows,
        "celestial-view-spatial-island", note)
}
