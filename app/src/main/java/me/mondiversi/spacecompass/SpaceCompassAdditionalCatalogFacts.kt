package me.mondiversi.spacecompass

/** Qualified reference values, never guessed current telemetry. Missing properties stay null.
 * JPL satellite physical table GM/volume-equivalent radii; SBDB/Horizons; NASA Exoplanet Archive. */
internal fun spaceCompassAdditionalCatalogFacts(body: SpaceCompassCelestialBody): SpaceCompassCelestialFacts? = when (body) {
    SpaceCompassCelestialBody.GANYMEDE -> moonFacts(9887.83275, 2631.2, 1.7, 1941.6, 7.155, SpaceCompassCelestialBody.JUPITER)
    SpaceCompassCelestialBody.CALLISTO -> moonFacts(7179.2834, 2410.3, 1.5, 1834.0, 16.691, SpaceCompassCelestialBody.JUPITER)
    SpaceCompassCelestialBody.PHOBOS -> moonFacts(0.0007087, 11.08, 0.04, 1872.0, 0.319, SpaceCompassCelestialBody.MARS)
    SpaceCompassCelestialBody.DEIMOS -> moonFacts(9.62e-05, 6.2, 0.24, 1471.0, 1.263, SpaceCompassCelestialBody.MARS)
    SpaceCompassCelestialBody.RHEA -> moonFacts(153.94175, 763.5, 0.6, 1237.2, 4.518, SpaceCompassCelestialBody.SATURN)
    SpaceCompassCelestialBody.IAPETUS -> moonFacts(120.51511, 734.3, 2.8, 1088.7, 79.33, SpaceCompassCelestialBody.SATURN)
    SpaceCompassCelestialBody.TITANIA -> moonFacts(226.9, 788.9, 1.8, 1653.0, 8.706, SpaceCompassCelestialBody.URANUS)
    SpaceCompassCelestialBody.MIRANDA -> moonFacts(4.3, 235.8, 0.7, 1178.0, 1.413, SpaceCompassCelestialBody.URANUS)
    SpaceCompassCelestialBody.TRITON -> moonFacts(1428.49546, 1352.6, 2.4, 2064.9, 5.876994, SpaceCompassCelestialBody.NEPTUNE)
    SpaceCompassCelestialBody.CHARON -> moonFacts(106.1, 606.0, 0.5, 1853.0, 6.387222, SpaceCompassCelestialBody.PLUTO)
    SpaceCompassCelestialBody.ERIS -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, revolutionDays = 203486.8929375, minimumParentKm = 37.92836976130601 * SPACE_COMPASS_AU_KM, maximumParentKm = 97.48306440637319 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.HAUMEA -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, rotationHours = 3.9154, revolutionDays = 104051.18910749999, minimumParentKm = 34.98943372351407 * SPACE_COMPASS_AU_KM, maximumParentKm = 51.59950431021929 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.MAKEMAKE -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, rotationHours = 22.8266, revolutionDays = 112250.61696, minimumParentKm = 38.31981212406959 * SPACE_COMPASS_AU_KM, maximumParentKm = 52.7602965019935 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.VESTA -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, diameterKm = 522.77, diameterEstimated = true, massKg = 2.5902761338267684e+20, massEstimated = true, gravity = 0.25304074945489585, rotationHours = 5.342128, revolutionDays = 1325.8465425, minimumParentKm = 2.15270823108896 * SPACE_COMPASS_AU_KM, maximumParentKm = 2.571109077494583 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.BENNU -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, diameterKm = 0.482, diameterEstimated = true, rotationHours = 4.296061, revolutionDays = 436.64907000000005, minimumParentKm = 0.8968944004459729 * SPACE_COMPASS_AU_KM, maximumParentKm = 1.355887651343651 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.RYUGU -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, diameterKm = 0.896, diameterEstimated = true, massKg = 449485339286.5169, massEstimated = true, gravity = 0.0001494738520408163, rotationHours = 7.63262, revolutionDays = 473.90457, minimumParentKm = 0.963114783617966 * SPACE_COMPASS_AU_KM, maximumParentKm = 1.416057456207773 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.HALE_BOPP -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, diameterKm = 60.0, diameterEstimated = true, revolutionDays = 863279.5034891797, minimumParentKm = 0.890537663547794 * SPACE_COMPASS_AU_KM, maximumParentKm = 353.9762301599687 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.ENCKE -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN, diameterKm = 4.8, diameterEstimated = true, revolutionDays = 1207.0470837581395, minimumParentKm = 0.3395817503585773 * SPACE_COMPASS_AU_KM, maximumParentKm = 4.097667777125062 * SPACE_COMPASS_AU_KM)
    SpaceCompassCelestialBody.ARCTURUS -> SpaceCompassCelestialFacts(spectralType = "K1.5 III", apparentMagnitude = -0.05)
    SpaceCompassCelestialBody.CANOPUS -> SpaceCompassCelestialFacts(spectralType = "A9 II", apparentMagnitude = -0.74)
    SpaceCompassCelestialBody.CAPELLA -> SpaceCompassCelestialFacts(spectralType = "G3 III", apparentMagnitude = 0.08)
    SpaceCompassCelestialBody.ANTARES -> SpaceCompassCelestialFacts(spectralType = "M1.5 Iab + B2 V", apparentMagnitude = 0.91)
    SpaceCompassCelestialBody.ALDEBARAN -> SpaceCompassCelestialFacts(spectralType = "K5+ III", apparentMagnitude = 0.86)
    SpaceCompassCelestialBody.SPICA -> SpaceCompassCelestialFacts(spectralType = "B1 V", apparentMagnitude = 0.97)
    SpaceCompassCelestialBody.FOMALHAUT -> SpaceCompassCelestialFacts(spectralType = "A4 V", apparentMagnitude = 1.16)
    SpaceCompassCelestialBody.ACHERNAR -> SpaceCompassCelestialFacts(spectralType = "B6 Vpe", apparentMagnitude = 0.46)
    SpaceCompassCelestialBody.HADAR -> SpaceCompassCelestialFacts(spectralType = "B1 III", apparentMagnitude = 0.58)
    SpaceCompassCelestialBody.SIGMA_OCTANTIS -> SpaceCompassCelestialFacts(spectralType = "F0 IV", apparentMagnitude = 5.42)
    SpaceCompassCelestialBody.M13 -> SpaceCompassCelestialFacts(apparentMagnitude = 5.8)
    SpaceCompassCelestialBody.OMEGA_CENTAURI -> SpaceCompassCelestialFacts()
    SpaceCompassCelestialBody.M57 -> SpaceCompassCelestialFacts(apparentMagnitude = 8.8)
    SpaceCompassCelestialBody.M27 -> SpaceCompassCelestialFacts(apparentMagnitude = 7.5)
    SpaceCompassCelestialBody.M1 -> SpaceCompassCelestialFacts(apparentMagnitude = 8.4)
    SpaceCompassCelestialBody.CARINA_NEBULA -> SpaceCompassCelestialFacts()
    SpaceCompassCelestialBody.HORSEHEAD_NEBULA -> SpaceCompassCelestialFacts()
    SpaceCompassCelestialBody.LARGE_MAGELLANIC_CLOUD -> SpaceCompassCelestialFacts(apparentMagnitude = 0.4)
    SpaceCompassCelestialBody.SMALL_MAGELLANIC_CLOUD -> SpaceCompassCelestialFacts(apparentMagnitude = 2.2)
    SpaceCompassCelestialBody.M51 -> SpaceCompassCelestialFacts(apparentMagnitude = 8.4)
    SpaceCompassCelestialBody.BEPICOLOMBO -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN)
    SpaceCompassCelestialBody.SOLAR_ORBITER -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN)
    SpaceCompassCelestialBody.LUCY -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN)
    SpaceCompassCelestialBody.PSYCHE -> SpaceCompassCelestialFacts(parent = SpaceCompassCelestialBody.SUN)
    SpaceCompassCelestialBody.CYGNUS_X1 -> SpaceCompassCelestialFacts(massSolar = 21.2, massSolarError = 2.2, massEstimated = true)
    SpaceCompassCelestialBody.V404_CYGNI -> SpaceCompassCelestialFacts(massSolar = 9.0, massSolarError = .6, massEstimated = true)
    SpaceCompassCelestialBody.VELA_PULSAR -> SpaceCompassCelestialFacts(rotationSeconds = .0893)
    SpaceCompassCelestialBody.TOI_700_D -> SpaceCompassCelestialFacts(diameterKm = 14589.59, diameterEstimated = true, diameterErrorMinusKm = 331.292, diameterErrorPlusKm = 407.744, revolutionDays = 37.423457, parentName = "TOI-700")
    SpaceCompassCelestialBody.KEPLER_186_F -> SpaceCompassCelestialFacts(diameterKm = 14908.14, diameterEstimated = true, diameterErrorMinusKm = 1019.36, diameterErrorPlusKm = 1019.36, revolutionDays = 129.9441, parentName = "Kepler-186")
    SpaceCompassCelestialBody.ANDROMEDA_GALAXY -> SpaceCompassCelestialFacts(apparentMagnitude = 3.1)
    SpaceCompassCelestialBody.M87_GALAXY -> SpaceCompassCelestialFacts(apparentMagnitude = 9.6)
    SpaceCompassCelestialBody.MILKY_WAY_CENTER -> SpaceCompassCelestialFacts()
    // Izumi et al. (2016), Table 1: log10(M/Msun)=6.60 +/-0.49, M-sigma estimate.
    // Rounded value; the large logarithmic uncertainty is explained in the object note.
    SpaceCompassCelestialBody.M51_CORE -> SpaceCompassCelestialFacts(massSolar = 4e6, massEstimated = true)
    else -> body.largeCatalogEntry?.facts
}

private fun moonFacts(gm: Double, radius: Double, radiusError: Double, densityKgM3: Double,
    days: Double, parent: SpaceCompassCelestialBody) = SpaceCompassCelestialFacts(
    diameterKm = 2 * radius, diameterErrorKm = 2 * radiusError, diameterEstimated = true,
    massKg = gm / 6.67430e-20, gravity = gm / (radius * radius) * 1000,
    density = densityKgM3, rotationHours = days * 24, revolutionDays = days, parent = parent)
