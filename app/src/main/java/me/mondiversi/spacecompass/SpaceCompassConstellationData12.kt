package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternSEX() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("30Bet Sex", SpaceCompassDeepSkyReference(10.504855, -0.637026, 404.6605870305015, -40.12, -22.17)),
        SpaceCompassConstellationStar("15Alp Sex", SpaceCompassDeepSkyReference(10.1323, -0.371637, 283.36792250953704, -26.17, -3.65)),
    ),
    edges = listOf(0 to 1)
)

internal fun spaceCompassPatternMEN() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Gam Men", SpaceCompassDeepSkyReference(5.531318, -76.340964, 102.27546460641061, 143.2, 287.75)),
        SpaceCompassConstellationStar("Mu  Men", SpaceCompassDeepSkyReference(4.717764, -70.931027, 491.19933256926475, 9.3, 34.77)),
    ),
    edges = listOf(0 to 1)
)

internal fun spaceCompassPatternTAU() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Elnath", SpaceCompassDeepSkyReference(5.438198, 28.60745, 133.8901284532493, 23.28, -174.22)),
        SpaceCompassConstellationStar("94Tau Tau", SpaceCompassDeepSkyReference(4.704084, 22.956926, 398.2372633280777, -2.84, -20.33)),
        SpaceCompassConstellationStar("Ain", SpaceCompassDeepSkyReference(4.476943, 19.180431, 146.652953669028, 107.23, -36.77)),
        SpaceCompassConstellationStar("Aldebaran", SpaceCompassDeepSkyReference(4.598677, 16.509301, 66.6441849681964, 62.78, -189.36)),
        SpaceCompassConstellationStar("Tianguan", SpaceCompassDeepSkyReference(5.627413, 21.142549, 444.9607952154912, 2.39, -18.04)),
        SpaceCompassConstellationStar("Prima Hyadum", SpaceCompassDeepSkyReference(4.329889, 15.627642, 161.5436230929215, 115.29, -23.86)),
        SpaceCompassConstellationStar("Secunda Hyadum", SpaceCompassDeepSkyReference(4.382247, 17.542514, 155.6088816442923, 107.75, -28.84)),
        SpaceCompassConstellationStar("35Lam Tau", SpaceCompassDeepSkyReference(4.011338, 12.490347, 483.911694465936, -8.15, -11.98)),
        SpaceCompassConstellationStar("1Omi Tau", SpaceCompassDeepSkyReference(3.413554, 9.02887, 290.95138444743975, -74.25, -80.31)),
        SpaceCompassConstellationStar("Chamukuy", SpaceCompassDeepSkyReference(4.477705, 15.870883, 150.3717886875634, 108.66, -26.39)),
        SpaceCompassConstellationStar("68Del3Tau", SpaceCompassDeepSkyReference(4.424828, 17.92791, 148.5228081823821, 108.26, -32.47)),
        SpaceCompassConstellationStar("Atlas", SpaceCompassDeepSkyReference(3.819373, 24.053415, 382.36388473817414, 17.77, -44.7)),
    ),
    edges = listOf(0 to 1, 1 to 2, 3 to 4, 5 to 6, 5 to 7, 7 to 8, 3 to 2, 3 to 9, 9 to 5, 2 to 10, 10 to 6, 6 to 11)
)

internal fun spaceCompassPatternTEL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Zet Tel", SpaceCompassDeepSkyReference(18.480505, -49.070588, 126.22153970076691, 137.9, -229.14)),
        SpaceCompassConstellationStar("Alp Tel", SpaceCompassDeepSkyReference(18.449561, -45.968459, 277.8164148047053, -16.68, -53.33)),
    ),
    edges = listOf(0 to 1)
)

internal fun spaceCompassPatternTUC() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Tuc", SpaceCompassDeepSkyReference(22.308371, -60.259587, 199.72838101214901, -71.48, -38.15)),
        SpaceCompassConstellationStar("Gam Tuc", SpaceCompassDeepSkyReference(23.290498, -58.235734, 75.20318063179981, -34.93, 79.59)),
        SpaceCompassConstellationStar("Zet Tuc", SpaceCompassDeepSkyReference(0.334142, -64.874791, 28.0057435275882, 1707.56, 1165.36)),
        SpaceCompassConstellationStar("Bet1Tuc", SpaceCompassDeepSkyReference(0.525725, -62.958218, 135.0545067216383, 82.48, -54.37)),
    ),
    edges = listOf(0 to 1, 1 to 2, 1 to 3)
)

internal fun spaceCompassPatternTRI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("9Gam Tri", SpaceCompassDeepSkyReference(2.288573, 33.847194, 112.3129271301281, 44.91, -52.42)),
        SpaceCompassConstellationStar("4Bet Tri", SpaceCompassDeepSkyReference(2.159058, 34.987297, 126.8598277319258, 148.78, -39.13)),
        SpaceCompassConstellationStar("Mothallah", SpaceCompassDeepSkyReference(1.884696, 29.578829, 63.3314146398975, 12.02, -233.69)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0)
)

internal fun spaceCompassPatternTRA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Atria", SpaceCompassDeepSkyReference(16.811077, -69.027715, 390.6065087154085, 17.85, -32.92)),
        SpaceCompassConstellationStar("Gam TrA", SpaceCompassDeepSkyReference(15.315181, -68.679545, 183.8536977967346, -66.48, -32.0)),
        SpaceCompassConstellationStar("Bet TrA", SpaceCompassDeepSkyReference(15.919083, -63.430727, 40.3709841189506, -188.45, -401.92)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0)
)
