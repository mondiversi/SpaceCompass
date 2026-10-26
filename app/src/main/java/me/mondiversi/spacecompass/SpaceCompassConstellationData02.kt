package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternSCT() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Bet Sct", SpaceCompassDeepSkyReference(18.786242, -4.747867, 916.1696772391455, -7.71, -15.89)),
        SpaceCompassConstellationStar("HIP 92202", SpaceCompassDeepSkyReference(18.791375, -5.705147, Double.NaN, -44.09, -31.36, hasMeasuredDistance = false)),
        SpaceCompassConstellationStar("HIP 92814", SpaceCompassDeepSkyReference(18.911976, -15.603036, 1399.8126188498566, -2.38, -5.64)),
        SpaceCompassConstellationStar("Gam Sct", SpaceCompassDeepSkyReference(18.486626, -14.565813, 319.44799332346645, 3.07, -3.48)),
        SpaceCompassConstellationStar("Alp Sct", SpaceCompassDeepSkyReference(18.586785, -8.244073, 199.11879474222772, -18.98, -314.63)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 0)
)

internal fun spaceCompassPatternPYX() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Bet Pyx", SpaceCompassDeepSkyReference(8.668373, -35.308352, 416.015721320127, 9.68, -21.18)),
        SpaceCompassConstellationStar("Alp Pyx", SpaceCompassDeepSkyReference(8.726539, -33.186385, 879.1277712673788, -14.28, 10.6)),
        SpaceCompassConstellationStar("Gam Pyx", SpaceCompassDeepSkyReference(8.842204, -27.709844, 207.34674168246562, -133.49, 88.16)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)

internal fun spaceCompassPatternBOO() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("30Zet Boo", SpaceCompassDeepSkyReference(14.68582, 13.7283, 175.7307732101161, 52.36, -12.71)),
        SpaceCompassConstellationStar("Arcturus", SpaceCompassDeepSkyReference(14.26103, 19.18241, 36.717054219577506, -1093.45, -1999.4)),
        SpaceCompassConstellationStar("Izar", SpaceCompassDeepSkyReference(14.749784, 27.074222, 202.58159700426862, -50.65, 20.0)),
        SpaceCompassConstellationStar("49Del Boo", SpaceCompassDeepSkyReference(15.258376, 33.314833, 121.79103146609009, 84.84, -110.57)),
        SpaceCompassConstellationStar("Nekkar", SpaceCompassDeepSkyReference(15.032436, 40.390566, 225.2462036906416, -40.2, -29.22)),
        SpaceCompassConstellationStar("Seginus", SpaceCompassDeepSkyReference(14.534636, 38.308253, 86.7898859495923, -115.55, 151.87)),
        SpaceCompassConstellationStar("25Rho Boo", SpaceCompassDeepSkyReference(14.5305, 30.371437, 160.1160366277286, -100.44, 120.22)),
        SpaceCompassConstellationStar("Muphrid", SpaceCompassDeepSkyReference(13.911411, 18.397717, 37.168780802692005, -60.95, -358.1)),
        SpaceCompassConstellationStar("5Ups Boo", SpaceCompassDeepSkyReference(13.824623, 15.797905, 263.45411871268584, -93.64, 40.76)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 1, 1 to 7, 7 to 8)
)

internal fun spaceCompassPatternCAE() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Del Cae", SpaceCompassDeepSkyReference(4.513916, -44.953749, 704.4413507786579, 2.44, -2.48)),
        SpaceCompassConstellationStar("Alp Cae", SpaceCompassDeepSkyReference(4.676039, -41.863752, 65.77073818871581, -141.18, -74.95)),
        SpaceCompassConstellationStar("Bet Cae", SpaceCompassDeepSkyReference(4.700965, -37.144297, 93.5080550174569, 46.9, 193.14)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)

internal fun spaceCompassPatternCHA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Cha", SpaceCompassDeepSkyReference(8.308703, -76.919722, 63.80205829291861, 109.84, 106.92)),
        SpaceCompassConstellationStar("Gam Cha", SpaceCompassDeepSkyReference(10.591166, -78.607786, 417.613887570857, -38.0, 11.53)),
        SpaceCompassConstellationStar("Bet Cha", SpaceCompassDeepSkyReference(12.305812, -79.31224, 298.4047099906401, -38.07, 12.0)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)

internal fun spaceCompassPatternCNC() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("48Iot Cnc", SpaceCompassDeepSkyReference(8.778284, 28.759898, 331.12308701961564, -20.69, -43.95)),
        SpaceCompassConstellationStar("Asellus Borealis", SpaceCompassDeepSkyReference(8.721431, 21.468501, 181.1981325695012, -106.94, -39.25)),
        SpaceCompassConstellationStar("18Chi Cnc", SpaceCompassDeepSkyReference(8.334406, 27.217707, 59.5936625514555, -18.38, -376.26)),
        SpaceCompassConstellationStar("Asellus Australis", SpaceCompassDeepSkyReference(8.74475, 18.154309, 130.56692112086398, -17.1, -228.46)),
        SpaceCompassConstellationStar("Tarf", SpaceCompassDeepSkyReference(8.275256, 9.185545, 303.40142569700413, -46.8, -48.65)),
        SpaceCompassConstellationStar("Acubens", SpaceCompassDeepSkyReference(8.974784, 11.857701, 188.3119293235159, 41.45, -29.22)),
    ),
    edges = listOf(0 to 1, 1 to 2, 1 to 3, 3 to 4, 3 to 5)
)

internal fun spaceCompassPatternCAP() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Algedi", SpaceCompassDeepSkyReference(20.300904, -12.544852, 105.8263290904305, 61.78, 2.85)),
        SpaceCompassConstellationStar("Dabih", SpaceCompassDeepSkyReference(20.350187, -14.781367, 326.8099950809108, 48.42, 14.0)),
        SpaceCompassConstellationStar("23The Cap", SpaceCompassDeepSkyReference(21.099118, -17.232861, 162.18615115699052, 79.64, -61.64)),
        SpaceCompassConstellationStar("32Iot Cap", SpaceCompassDeepSkyReference(21.370776, -16.834542, 196.71665302046722, 30.83, 5.26)),
        SpaceCompassConstellationStar("Nashira", SpaceCompassDeepSkyReference(21.668181, -16.662308, 157.0325542329528, 187.39, -22.33)),
        SpaceCompassConstellationStar("Deneb Algedi", SpaceCompassDeepSkyReference(21.784011, -16.127286, 38.703672716148205, 263.26, -296.23)),
        SpaceCompassConstellationStar("34Zet Cap", SpaceCompassDeepSkyReference(21.444452, -22.411332, 385.5276016018641, -2.61, 18.88)),
        SpaceCompassConstellationStar("16Psi Cap", SpaceCompassDeepSkyReference(20.76826, -25.270898, 47.8725808060506, -51.38, -156.66)),
        SpaceCompassConstellationStar("18Ome Cap", SpaceCompassDeepSkyReference(20.863692, -26.919133, 842.7812306928683, -7.97, -2.54)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 3 to 6, 6 to 2, 1 to 7, 2 to 8)
)
