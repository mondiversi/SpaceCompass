package me.mondiversi.spacecompass

/** One registry for the additional catalogue objects. Stable enum names preserve saved selections. */
internal data class SpaceCompassAdditionalCatalogEntry(val type: SpaceCompassCatalogType, val designation: String,
    val reference: SpaceCompassDeepSkyReference?, val target: SpaceCompassHorizonsTarget?)

internal val SpaceCompassCelestialBody.additionalCatalogEntry: SpaceCompassAdditionalCatalogEntry?
    get() = additionalCatalog[this] ?: largeCatalogEntry?.entry

private val additionalCatalog: Map<SpaceCompassCelestialBody, SpaceCompassAdditionalCatalogEntry> by lazy {
    val types = mapOf(
        SpaceCompassCelestialBody.GANYMEDE to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.CALLISTO to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.PHOBOS to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.DEIMOS to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.RHEA to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.IAPETUS to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.TITANIA to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.MIRANDA to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.TRITON to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.CHARON to SpaceCompassCatalogType.NATURAL_SATELLITE,
        SpaceCompassCelestialBody.ERIS to SpaceCompassCatalogType.DWARF_PLANET,
        SpaceCompassCelestialBody.HAUMEA to SpaceCompassCatalogType.DWARF_PLANET,
        SpaceCompassCelestialBody.MAKEMAKE to SpaceCompassCatalogType.DWARF_PLANET,
        SpaceCompassCelestialBody.VESTA to SpaceCompassCatalogType.ASTEROID,
        SpaceCompassCelestialBody.BENNU to SpaceCompassCatalogType.ASTEROID,
        SpaceCompassCelestialBody.RYUGU to SpaceCompassCatalogType.ASTEROID,
        SpaceCompassCelestialBody.HALE_BOPP to SpaceCompassCatalogType.COMET,
        SpaceCompassCelestialBody.ENCKE to SpaceCompassCatalogType.COMET,
        SpaceCompassCelestialBody.ARCTURUS to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.CANOPUS to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.CAPELLA to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.ANTARES to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.ALDEBARAN to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.SPICA to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.FOMALHAUT to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.ACHERNAR to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.HADAR to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.SIGMA_OCTANTIS to SpaceCompassCatalogType.STAR,
        SpaceCompassCelestialBody.M13 to SpaceCompassCatalogType.STAR_CLUSTER,
        SpaceCompassCelestialBody.OMEGA_CENTAURI to SpaceCompassCatalogType.STAR_CLUSTER,
        SpaceCompassCelestialBody.M57 to SpaceCompassCatalogType.NEBULA,
        SpaceCompassCelestialBody.M27 to SpaceCompassCatalogType.NEBULA,
        SpaceCompassCelestialBody.M1 to SpaceCompassCatalogType.NEBULA,
        SpaceCompassCelestialBody.CARINA_NEBULA to SpaceCompassCatalogType.NEBULA,
        SpaceCompassCelestialBody.HORSEHEAD_NEBULA to SpaceCompassCatalogType.NEBULA,
        SpaceCompassCelestialBody.LARGE_MAGELLANIC_CLOUD to SpaceCompassCatalogType.GALAXY,
        SpaceCompassCelestialBody.SMALL_MAGELLANIC_CLOUD to SpaceCompassCatalogType.GALAXY,
        SpaceCompassCelestialBody.M51 to SpaceCompassCatalogType.GALAXY,
        SpaceCompassCelestialBody.BEPICOLOMBO to SpaceCompassCatalogType.SPACE_PROBE,
        SpaceCompassCelestialBody.SOLAR_ORBITER to SpaceCompassCatalogType.SPACE_PROBE,
        SpaceCompassCelestialBody.LUCY to SpaceCompassCatalogType.SPACE_PROBE,
        SpaceCompassCelestialBody.PSYCHE to SpaceCompassCatalogType.SPACE_PROBE,
        SpaceCompassCelestialBody.CYGNUS_X1 to SpaceCompassCatalogType.BLACK_HOLE,
        SpaceCompassCelestialBody.V404_CYGNI to SpaceCompassCatalogType.BLACK_HOLE,
        SpaceCompassCelestialBody.VELA_PULSAR to SpaceCompassCatalogType.NEUTRON_STAR,
        SpaceCompassCelestialBody.TOI_700_D to SpaceCompassCatalogType.EXOPLANET,
        SpaceCompassCelestialBody.KEPLER_186_F to SpaceCompassCatalogType.EXOPLANET,
        SpaceCompassCelestialBody.ANDROMEDA_GALAXY to SpaceCompassCatalogType.GALAXY,
        SpaceCompassCelestialBody.M87_GALAXY to SpaceCompassCatalogType.GALAXY,
        SpaceCompassCelestialBody.MILKY_WAY_CENTER to SpaceCompassCatalogType.GALAXY,
        SpaceCompassCelestialBody.M51_CORE to SpaceCompassCatalogType.BLACK_HOLE)
    val designations = mapOf(
        SpaceCompassCelestialBody.GANYMEDE to "Ganymede (503)",
        SpaceCompassCelestialBody.CALLISTO to "Callisto (504)",
        SpaceCompassCelestialBody.PHOBOS to "Phobos (401)",
        SpaceCompassCelestialBody.DEIMOS to "Deimos (402)",
        SpaceCompassCelestialBody.RHEA to "Rhea (605)",
        SpaceCompassCelestialBody.IAPETUS to "Iapetus (608)",
        SpaceCompassCelestialBody.TITANIA to "Titania (703)",
        SpaceCompassCelestialBody.MIRANDA to "Miranda (705)",
        SpaceCompassCelestialBody.TRITON to "Triton (801)",
        SpaceCompassCelestialBody.CHARON to "Charon (901)",
        SpaceCompassCelestialBody.ERIS to "136199 Eris (2003 UB313)",
        SpaceCompassCelestialBody.HAUMEA to "136108 Haumea (2003 EL61)",
        SpaceCompassCelestialBody.MAKEMAKE to "136472 Makemake (2005 FY9)",
        SpaceCompassCelestialBody.VESTA to "4 Vesta (A807 FA)",
        SpaceCompassCelestialBody.BENNU to "101955 Bennu (1999 RQ36) (2101955)",
        SpaceCompassCelestialBody.RYUGU to "162173 Ryugu (1999 JU3)",
        SpaceCompassCelestialBody.HALE_BOPP to "Hale-Bopp (C/1995 O1)",
        SpaceCompassCelestialBody.ENCKE to "2P/Encke",
        SpaceCompassCelestialBody.ARCTURUS to "α Bootis · HD 124897",
        SpaceCompassCelestialBody.CANOPUS to "α Carinae · HD 45348",
        SpaceCompassCelestialBody.CAPELLA to "α Aurigae · HD 34029",
        SpaceCompassCelestialBody.ANTARES to "α Scorpii · HD 148478",
        SpaceCompassCelestialBody.ALDEBARAN to "α Tauri · HD 29139",
        SpaceCompassCelestialBody.SPICA to "α Virginis · HD 116658",
        SpaceCompassCelestialBody.FOMALHAUT to "α Piscis Austrini · HD 216956",
        SpaceCompassCelestialBody.ACHERNAR to "α Eridani · HD 10144",
        SpaceCompassCelestialBody.HADAR to "β Centauri · HD 122451",
        SpaceCompassCelestialBody.SIGMA_OCTANTIS to "σ Octantis · Polaris Australis",
        SpaceCompassCelestialBody.M13 to "M13 · NGC 6205",
        SpaceCompassCelestialBody.OMEGA_CENTAURI to "ω Centauri · NGC 5139",
        SpaceCompassCelestialBody.M57 to "M57 · NGC 6720",
        SpaceCompassCelestialBody.M27 to "M27 · NGC 6853",
        SpaceCompassCelestialBody.M1 to "M1 · NGC 1952",
        SpaceCompassCelestialBody.CARINA_NEBULA to "NGC 3372",
        SpaceCompassCelestialBody.HORSEHEAD_NEBULA to "Barnard 33 · B33",
        SpaceCompassCelestialBody.LARGE_MAGELLANIC_CLOUD to "LMC",
        SpaceCompassCelestialBody.SMALL_MAGELLANIC_CLOUD to "SMC",
        SpaceCompassCelestialBody.M51 to "M51 · NGC 5194",
        SpaceCompassCelestialBody.BEPICOLOMBO to "BepiColombo (Spacecraft) (-121)",
        SpaceCompassCelestialBody.SOLAR_ORBITER to "Solar Orbiter (spacecraft) (-144)",
        SpaceCompassCelestialBody.LUCY to "Lucy (spacecraft) (-49)",
        SpaceCompassCelestialBody.PSYCHE to "Psyche (spacecraft) (-255)",
        SpaceCompassCelestialBody.CYGNUS_X1 to "Cyg X-1 · HDE 226868",
        SpaceCompassCelestialBody.V404_CYGNI to "V404 Cygni · GS 2023+338",
        SpaceCompassCelestialBody.VELA_PULSAR to "PSR B0833−45 · PSR J0835−4510",
        SpaceCompassCelestialBody.TOI_700_D to "TOI-700 d",
        SpaceCompassCelestialBody.KEPLER_186_F to "Kepler-186 f",
        SpaceCompassCelestialBody.ANDROMEDA_GALAXY to "M31 · NGC 224",
        SpaceCompassCelestialBody.M87_GALAXY to "M87 · NGC 4486",
        SpaceCompassCelestialBody.MILKY_WAY_CENTER to "Galactic Centre",
        SpaceCompassCelestialBody.M51_CORE to "NGC 5194 nucleus")
    types.mapValues { (body, type) -> SpaceCompassAdditionalCatalogEntry(type,
        designations.getValue(body), spaceCompassAdditionalSkyReference(body), spaceCompassAdditionalHorizonsTarget(body)) }
}

internal fun spaceCompassAdditionalHorizonsTarget(body: SpaceCompassCelestialBody): SpaceCompassHorizonsTarget? = when (body) {
    SpaceCompassCelestialBody.GANYMEDE -> SpaceCompassHorizonsTarget("503", "Ganymede (503)", 0.0..100.0)
    SpaceCompassCelestialBody.CALLISTO -> SpaceCompassHorizonsTarget("504", "Callisto (504)", 0.0..100.0)
    SpaceCompassCelestialBody.PHOBOS -> SpaceCompassHorizonsTarget("401", "Phobos (401)", 0.0..100.0)
    SpaceCompassCelestialBody.DEIMOS -> SpaceCompassHorizonsTarget("402", "Deimos (402)", 0.0..100.0)
    SpaceCompassCelestialBody.RHEA -> SpaceCompassHorizonsTarget("605", "Rhea (605)", 0.0..100.0)
    SpaceCompassCelestialBody.IAPETUS -> SpaceCompassHorizonsTarget("608", "Iapetus (608)", 0.0..100.0)
    SpaceCompassCelestialBody.TITANIA -> SpaceCompassHorizonsTarget("703", "Titania (703)", 0.0..100.0)
    SpaceCompassCelestialBody.MIRANDA -> SpaceCompassHorizonsTarget("705", "Miranda (705)", 0.0..100.0)
    SpaceCompassCelestialBody.TRITON -> SpaceCompassHorizonsTarget("801", "Triton (801)", 0.0..100.0)
    SpaceCompassCelestialBody.CHARON -> SpaceCompassHorizonsTarget("901", "Charon (901)", 0.0..100.0)
    SpaceCompassCelestialBody.ERIS -> SpaceCompassHorizonsTarget("136199;", "136199 Eris (2003 UB313)", 0.0..1000.0)
    SpaceCompassCelestialBody.HAUMEA -> SpaceCompassHorizonsTarget("136108;", "136108 Haumea (2003 EL61)", 0.0..1000.0)
    SpaceCompassCelestialBody.MAKEMAKE -> SpaceCompassHorizonsTarget("136472;", "136472 Makemake (2005 FY9)", 0.0..1000.0)
    SpaceCompassCelestialBody.VESTA -> SpaceCompassHorizonsTarget("4;", "4 Vesta (A807 FA)", 0.0..1000.0)
    SpaceCompassCelestialBody.BENNU -> SpaceCompassHorizonsTarget("101955;", "101955 Bennu (1999 RQ36) (2101955)", 0.0..1000.0)
    SpaceCompassCelestialBody.RYUGU -> SpaceCompassHorizonsTarget("162173;", "162173 Ryugu (1999 JU3)", 0.0..1000.0)
    SpaceCompassCelestialBody.HALE_BOPP -> SpaceCompassHorizonsTarget("DES=C/1995 O1;NOFRAG;CAP;", "Hale-Bopp (C/1995 O1)", 0.0..1000.0)
    SpaceCompassCelestialBody.ENCKE -> SpaceCompassHorizonsTarget("DES=2P;NOFRAG;CAP;", "2P/Encke", 0.0..1000.0)
    SpaceCompassCelestialBody.BEPICOLOMBO -> SpaceCompassHorizonsTarget("-121", "BepiColombo (Spacecraft) (-121)", 0.0..100.0)
    SpaceCompassCelestialBody.SOLAR_ORBITER -> SpaceCompassHorizonsTarget("-144", "Solar Orbiter (spacecraft) (-144)", 0.0..100.0)
    SpaceCompassCelestialBody.LUCY -> SpaceCompassHorizonsTarget("-49", "Lucy (spacecraft) (-49)", 0.0..100.0)
    SpaceCompassCelestialBody.PSYCHE -> SpaceCompassHorizonsTarget("-255", "Psyche (spacecraft) (-255)", 0.0..100.0)
    else -> null
}

/** Insert by scientific family without reordering the previous stable catalogue. */
internal fun spaceCompassIncludeAdditionalCatalog(previous: List<SpaceCompassCelestialBody>): List<SpaceCompassCelestialBody> {
    val after = mapOf(
        SpaceCompassCelestialBody.EUROPA to listOf(SpaceCompassCelestialBody.GANYMEDE, SpaceCompassCelestialBody.CALLISTO),
        SpaceCompassCelestialBody.MARS to listOf(SpaceCompassCelestialBody.PHOBOS, SpaceCompassCelestialBody.DEIMOS),
        SpaceCompassCelestialBody.ENCELADUS to listOf(SpaceCompassCelestialBody.RHEA, SpaceCompassCelestialBody.IAPETUS),
        SpaceCompassCelestialBody.URANUS to listOf(SpaceCompassCelestialBody.TITANIA, SpaceCompassCelestialBody.MIRANDA),
        SpaceCompassCelestialBody.NEPTUNE to listOf(SpaceCompassCelestialBody.TRITON),
        SpaceCompassCelestialBody.PLUTO to listOf(SpaceCompassCelestialBody.CHARON, SpaceCompassCelestialBody.ERIS, SpaceCompassCelestialBody.HAUMEA, SpaceCompassCelestialBody.MAKEMAKE),
        SpaceCompassCelestialBody.CERES to listOf(SpaceCompassCelestialBody.VESTA, SpaceCompassCelestialBody.BENNU, SpaceCompassCelestialBody.RYUGU),
        SpaceCompassCelestialBody.COMET_67P to listOf(SpaceCompassCelestialBody.HALE_BOPP, SpaceCompassCelestialBody.ENCKE),
        SpaceCompassCelestialBody.JUICE to listOf(SpaceCompassCelestialBody.BEPICOLOMBO, SpaceCompassCelestialBody.SOLAR_ORBITER, SpaceCompassCelestialBody.LUCY, SpaceCompassCelestialBody.PSYCHE),
        SpaceCompassCelestialBody.ALTAIR to listOf(SpaceCompassCelestialBody.FOMALHAUT),
        SpaceCompassCelestialBody.VEGA to listOf(SpaceCompassCelestialBody.ARCTURUS, SpaceCompassCelestialBody.CAPELLA, SpaceCompassCelestialBody.ALDEBARAN),
        SpaceCompassCelestialBody.RIGEL to listOf(SpaceCompassCelestialBody.CANOPUS, SpaceCompassCelestialBody.ANTARES, SpaceCompassCelestialBody.SPICA, SpaceCompassCelestialBody.ACHERNAR, SpaceCompassCelestialBody.HADAR, SpaceCompassCelestialBody.SIGMA_OCTANTIS),
        SpaceCompassCelestialBody.ORION_NEBULA to listOf(SpaceCompassCelestialBody.M13, SpaceCompassCelestialBody.OMEGA_CENTAURI, SpaceCompassCelestialBody.M57, SpaceCompassCelestialBody.M27, SpaceCompassCelestialBody.M1, SpaceCompassCelestialBody.CARINA_NEBULA, SpaceCompassCelestialBody.HORSEHEAD_NEBULA, SpaceCompassCelestialBody.LARGE_MAGELLANIC_CLOUD, SpaceCompassCelestialBody.SMALL_MAGELLANIC_CLOUD, SpaceCompassCelestialBody.M51, SpaceCompassCelestialBody.M51_CORE),
        SpaceCompassCelestialBody.PSR_J0437 to listOf(SpaceCompassCelestialBody.VELA_PULSAR),
        SpaceCompassCelestialBody.SAGITTARIUS_A to listOf(SpaceCompassCelestialBody.MILKY_WAY_CENTER, SpaceCompassCelestialBody.CYGNUS_X1, SpaceCompassCelestialBody.V404_CYGNI),
        SpaceCompassCelestialBody.ANDROMEDA_CORE to listOf(SpaceCompassCelestialBody.ANDROMEDA_GALAXY),
        SpaceCompassCelestialBody.M87 to listOf(SpaceCompassCelestialBody.M87_GALAXY),
        SpaceCompassCelestialBody.TRAPPIST_1_E to listOf(SpaceCompassCelestialBody.TOI_700_D, SpaceCompassCelestialBody.KEPLER_186_F))
    return spaceCompassIncludeLargeCatalog(previous.flatMap { listOf(it) + after[it].orEmpty() })
}
