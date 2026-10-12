package me.mondiversi.spacecompass

/** Keep reference caveats and image credits alongside the observatory's exported measurements. */
internal fun spaceCompassObservatoryExportNotes(body: SpaceCompassCelestialBody,
    profile: SpaceCompassDataExportProfile): List<String> = buildList {
    val strings=profile.strings
    val facts=spaceCompassCelestialFacts(body)
    val temperatures=spaceCompassCelestialTemperatures(body)
    fun note(id: Int,vararg args: Any) { add(strings.getString(id,*args)) }
    if (body.isFictional) { note(R.string.celestial_lv426_model_note);note(R.string.celestial_lv426_credits) }
    else {
        note(R.string.celestial_view_facts_note,formatSpaceCompassPressure(SpaceCompassPressureUnit.BAR.pascals,
            profile.formatting.numeric,profile.units.pressure)!!)
        note(R.string.celestial_view_credits)
    }
    if (body.isComet) note(R.string.celestial_comet_note)
    if (body.isSpacecraft) note(R.string.celestial_view_craft_note)
    if (body==SpaceCompassCelestialBody.STARLINK_V3) note(R.string.celestial_starlink_note)
    if (body==SpaceCompassCelestialBody.ISS) note(R.string.celestial_view_iss_note)
    if (body==SpaceCompassCelestialBody.SUN) note(R.string.celestial_view_sun_note)
    if (body==SpaceCompassCelestialBody.SEDNA) { note(R.string.celestial_view_sedna_note);note(R.string.celestial_view_sedna_facts) }
    if (body==SpaceCompassCelestialBody.POLARIS) { note(R.string.celestial_polaris_derived);note(R.string.celestial_polaris_facts) }
    if (body.deepSkyReference!=null) note(body.deepSkyNoteResource)
    if (body.hasCatalogPhotograph) note(R.string.celestial_archival_image)
    spaceCompassCatalogImageCredit(body.viewerTexture)?.let(::add)
    if (body==SpaceCompassCelestialBody.ANDROMEDA_CORE) {
        note(R.string.celestial_andromeda_galaxy_note)
        spaceCompassCatalogImageCredit("andromeda_galaxy.webp")?.let(::add)
    }
    if (body==SpaceCompassCelestialBody.TITAN) note(R.string.celestial_titan_note)
    if (body.isJovianMoon) note(R.string.celestial_view_jovian_credits)
    if (spaceCompassAtmosphericPressure(body)!=null) {
        note(R.string.celestial_pressure_reference_note)
        if (body.isJovianMoon) add("Bagenal & Dols (2020)")
    }
    if (temperatures.isNotEmpty() && !body.isFictional) note(R.string.celestial_temperature_note)
    if (temperatures.any { it.kind in setOf(SpaceCompassCelestialTemperatureKind.SURFACE_ESTIMATE,
        SpaceCompassCelestialTemperatureKind.EQUILIBRIUM_MODEL,SpaceCompassCelestialTemperatureKind.HISTORICAL_SURFACE_RANGE,
        SpaceCompassCelestialTemperatureKind.THERMAL_MODEL) }) note(R.string.celestial_temperature_reference_note)
    spaceCompassCelestialCoreTemperature(body)?.let { core ->
        note(R.string.celestial_temperature_core_note)
        if (core.temperature.kind==SpaceCompassCelestialTemperatureKind.CORE_BOUNDARY_ESTIMATE) note(R.string.celestial_temperature_core_boundary_note)
        if (core.adiabaticReference) note(R.string.celestial_temperature_core_adiabatic_note)
        add(core.citation)
    }
    if (body==SpaceCompassCelestialBody.STEPHENSON_2_18) {
        note(R.string.celestial_mass_unavailable_note);note(R.string.celestial_stephenson_reference_note)
    }
    if (spaceCompassUsesSolarMass(facts)) note(R.string.celestial_solar_mass_note)
    else if (spaceCompassCelestialMassKilograms(facts)!=null && (facts.massModelAssumption || facts.massEstimated)) note(R.string.celestial_mass_reference_note)
    if (body==SpaceCompassCelestialBody.PSR_J0437 || spaceCompassHorizonDiameterKm(body)!=null) note(R.string.celestial_reference_estimates_note)
}
