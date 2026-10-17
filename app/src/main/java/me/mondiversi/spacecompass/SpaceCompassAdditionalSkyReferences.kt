package me.mondiversi.spacecompass

/** ICRS/J2000 catalogue/host directions. Sources, epochs and approximate distances:
 * docs/CATALOG_100.md. Extended objects have no invented resolved 3-D trajectory. */
internal fun spaceCompassAdditionalSkyReference(body: SpaceCompassCelestialBody): SpaceCompassDeepSkyReference? = when (body) {
    SpaceCompassCelestialBody.ARCTURUS -> SpaceCompassDeepSkyReference(14.261020019444445, 19.18240916666667, 36.716917449060006, -1093.39, -2000.06)
    SpaceCompassCelestialBody.CANOPUS -> SpaceCompassDeepSkyReference(6.399197188888889, -52.69566138888889, 309.15296464454974, 19.93, 23.24)
    SpaceCompassCelestialBody.CAPELLA -> SpaceCompassDeepSkyReference(5.2781551972222225, 45.997991472222225, 42.80267423884515, 75.25, -426.89)
    SpaceCompassCelestialBody.ANTARES -> SpaceCompassDeepSkyReference(16.490127694444446, -26.432002611111113, 553.7459723259764, -12.11, -23.3)
    SpaceCompassCelestialBody.ALDEBARAN -> SpaceCompassDeepSkyReference(4.598677519444444, 16.50930236111111, 66.64413111973846, 63.45, -188.94)
    SpaceCompassCelestialBody.SPICA -> SpaceCompassDeepSkyReference(13.419883158333333, -11.161319472222223, 249.73688950995407, -42.35, -30.67)
    SpaceCompassCelestialBody.FOMALHAUT -> SpaceCompassDeepSkyReference(22.960846180555556, -29.62223702777778, 25.12567427008705, 328.95, -164.67)
    SpaceCompassCelestialBody.ACHERNAR -> SpaceCompassDeepSkyReference(1.628568188888889, -57.236752805555554, 139.44265827276615, 87.0, -38.24)
    SpaceCompassCelestialBody.HADAR -> SpaceCompassDeepSkyReference(14.063723708333335, -60.37303516666667, 392.0148770432692, -33.27, -23.16)
    SpaceCompassCelestialBody.SIGMA_OCTANTIS -> SpaceCompassDeepSkyReference(21.146350991722, -88.95649953627, 293.8213393090401, 26.323, 4.721)
    SpaceCompassCelestialBody.M13 -> SpaceCompassDeepSkyReference(16.694898333333335, 36.46131944444445, 25000.0, -3.18, -2.56)
    SpaceCompassCelestialBody.OMEGA_CENTAURI -> SpaceCompassDeepSkyReference(13.446466666666668, -47.47947222222223, 17000.0, 0.0, 0.0)
    SpaceCompassCelestialBody.M57 -> SpaceCompassDeepSkyReference(18.893082434975334, 33.029134246539996, 2000.0, 0.0, 0.0)
    SpaceCompassCelestialBody.M27 -> SpaceCompassDeepSkyReference(19.993434218058002, 22.72119779432, 1360.0, 0.0, 0.0)
    SpaceCompassCelestialBody.M1 -> SpaceCompassDeepSkyReference(5.5755, 22.0175, 6500.0, 0.0, 0.0)
    SpaceCompassCelestialBody.CARINA_NEBULA -> SpaceCompassDeepSkyReference(10.750619444444444, -59.69994444444444, 7500.0, 0.0, 0.0)
    SpaceCompassCelestialBody.HORSEHEAD_NEBULA -> SpaceCompassDeepSkyReference(5.683055555555556, -2.4583333333333335, 1300.0, 0.0, 0.0)
    SpaceCompassCelestialBody.LARGE_MAGELLANIC_CLOUD -> SpaceCompassDeepSkyReference(5.392944444444445, -69.75611111111111, 160000.0, 1.91, 0.229)
    SpaceCompassCelestialBody.SMALL_MAGELLANIC_CLOUD -> SpaceCompassDeepSkyReference(0.8772222222222222, -72.80027777777778, 200000.0, 0.772, -1.117)
    SpaceCompassCelestialBody.M51 -> SpaceCompassDeepSkyReference(13.497971666666666, 47.19525833333333, 31000000.0, 0.0, 0.0)
    SpaceCompassCelestialBody.CYGNUS_X1 -> SpaceCompassDeepSkyReference(19.972687704332, 35.201606809080005, 7240.671584940001, -3.812, -6.31)
    SpaceCompassCelestialBody.V404_CYGNI -> SpaceCompassDeepSkyReference(20.4010624177065, 33.86721143190584, 7795.13742703, -5.177, -7.778)
    SpaceCompassCelestialBody.VELA_PULSAR -> SpaceCompassDeepSkyReference(8.589083333333335, -45.17658333333333, 1000.0, 0.0, 0.0)
    SpaceCompassCelestialBody.TOI_700_D -> SpaceCompassDeepSkyReference(6.473047766666666, -65.5786149, 101.5210649047905, 0.0, 0.0)
    SpaceCompassCelestialBody.KEPLER_186_F -> SpaceCompassDeepSkyReference(19.9101824, 43.9549884, 579.234157412538, 0.0, 0.0)
    // The galaxy marker points to its nucleus, not to every point of its extended disc.
    // These original catalogue references do not access the additional-registry lazy initializer.
    SpaceCompassCelestialBody.ANDROMEDA_GALAXY -> SpaceCompassCelestialBody.ANDROMEDA_CORE.deepSkyReference
    SpaceCompassCelestialBody.M87_GALAXY -> SpaceCompassCelestialBody.M87.deepSkyReference
    SpaceCompassCelestialBody.MILKY_WAY_CENTER -> SpaceCompassCelestialBody.SAGITTARIUS_A.deepSkyReference
    // PAWS (2014), Table 2: J2000 nuclear maser position, not the companion NGC 5195.
    SpaceCompassCelestialBody.M51_CORE -> SpaceCompassDeepSkyReference(
        13 + 29.0 / 60 + 52.71 / 3600, 47 + 11.0 / 60 + 42.79 / 3600, 31_000_000.0)
    else -> null
}
