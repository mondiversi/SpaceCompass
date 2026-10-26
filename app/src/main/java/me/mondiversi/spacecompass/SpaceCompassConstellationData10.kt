package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternEQU() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("5Gam Equ", SpaceCompassDeepSkyReference(21.172361, 10.131579, 118.38693735203522, 49.07, -151.85)),
        SpaceCompassConstellationStar("7Del Equ", SpaceCompassDeepSkyReference(21.241337, 10.006981, 60.298812640042904, 42.32, -303.43)),
        SpaceCompassConstellationStar("10Bet Equ", SpaceCompassDeepSkyReference(21.381559, 6.81114, 330.7874721069623, 52.66, 11.15)),
        SpaceCompassConstellationStar("Kitalpha", SpaceCompassDeepSkyReference(21.263731, 5.247845, 190.2897415978887, 59.63, -94.33)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
)

internal fun spaceCompassPatternCMI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Procyon", SpaceCompassDeepSkyReference(7.655033, 5.224993, 11.461787425133402, -716.57, -1034.58)),
        SpaceCompassConstellationStar("Gomeisa", SpaceCompassDeepSkyReference(7.452512, 8.289315, 161.7037658743722, -50.28, -38.45)),
    ),
    edges = listOf(0 to 1)
)

internal fun spaceCompassPatternLMI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Praecipua", SpaceCompassDeepSkyReference(10.888526, 34.214871, 94.8681271124659, 92.47, -286.06)),
        SpaceCompassConstellationStar("31Bet LMi", SpaceCompassDeepSkyReference(10.464727, 36.707212, 153.9200439205617, -127.52, -109.62)),
        SpaceCompassConstellationStar("21    LMi", SpaceCompassDeepSkyReference(10.123821, 35.244693, 92.1085180007462, 50.97, 0.64)),
        SpaceCompassConstellationStar("10    LMi", SpaceCompassDeepSkyReference(9.570384, 36.397558, 185.0007897771055, 6.82, -22.89)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 2 to 0)
)

internal fun spaceCompassPatternVUL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Anser", SpaceCompassDeepSkyReference(19.478427, 24.664905, 297.3166523146329, -126.45, -106.99)),
        SpaceCompassConstellationStar("15    Vul", SpaceCompassDeepSkyReference(20.018346, 27.753573, 236.34530522377264, 57.99, 3.76)),
    ),
    edges = listOf(0 to 1)
)

internal fun spaceCompassPatternUMI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Polaris", SpaceCompassDeepSkyReference(2.52975, 89.264109, 432.56815748840205, 44.22, -11.74)),
        SpaceCompassConstellationStar("Yildun", SpaceCompassDeepSkyReference(17.536918, 86.58646, 172.1140251378008, 10.54, 53.97)),
        SpaceCompassConstellationStar("22Eps UMi", SpaceCompassDeepSkyReference(16.766159, 82.037262, 303.9666546995582, 19.54, 4.67)),
        SpaceCompassConstellationStar("16Zet UMi", SpaceCompassDeepSkyReference(15.734299, 77.794493, 368.9552698945494, 20.07, -2.5)),
        SpaceCompassConstellationStar("21Eta UMi", SpaceCompassDeepSkyReference(16.291791, 75.75533, 96.9839035346058, -89.94, 257.8)),
        SpaceCompassConstellationStar("Pherkad", SpaceCompassDeepSkyReference(15.345483, 71.834016, 486.80046150322494, -18.03, 17.68)),
        SpaceCompassConstellationStar("Kochab", SpaceCompassDeepSkyReference(14.845105, 74.155505, 130.9338470457765, -32.29, 11.91)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 3)
)

internal fun spaceCompassPatternPHE() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Wurren", SpaceCompassDeepSkyReference(1.139742, -55.24576, 298.67802903515275, 21.17, 29.7)),
        SpaceCompassConstellationStar("Bet Phe", SpaceCompassDeepSkyReference(1.101407, -46.718414, Double.NaN, -70.16, 31.6, hasMeasuredDistance = false)),
        SpaceCompassConstellationStar("Kap Phe", SpaceCompassDeepSkyReference(0.436716, -43.679829, 77.6562027484815, 106.14, 32.83)),
        SpaceCompassConstellationStar("Del Phe", SpaceCompassDeepSkyReference(1.520851, -49.072702, 142.116118455221, 138.22, 154.2)),
        SpaceCompassConstellationStar("Psi Phe", SpaceCompassDeepSkyReference(1.894102, -46.302669, 341.8829859199386, -92.38, -91.46)),
        SpaceCompassConstellationStar("Gam Phe", SpaceCompassDeepSkyReference(1.472759, -43.318234, 233.6362503505964, -18.21, -207.71)),
        SpaceCompassConstellationStar("Ankaa", SpaceCompassDeepSkyReference(0.438056, -42.305981, 84.71585754379801, 232.76, -353.64)),
        SpaceCompassConstellationStar("Eps Phe", SpaceCompassDeepSkyReference(0.156836, -45.747426, 144.18949454825992, 122.15, -180.13)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0, 1 to 3, 3 to 4, 4 to 1, 1 to 5, 5 to 2, 2 to 6, 6 to 7, 7 to 2)
)

internal fun spaceCompassPatternPSC() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("69Sig Psc", SpaceCompassDeepSkyReference(1.046971, 31.804263, 433.71851103254994, 15.53, -30.38)),
        SpaceCompassConstellationStar("85Phi Psc", SpaceCompassDeepSkyReference(1.229152, 24.583713, 446.1783369734453, 18.58, -21.26)),
        SpaceCompassConstellationStar("90Ups Psc", SpaceCompassDeepSkyReference(1.324443, 27.264059, 307.98522742919994, 26.21, -11.62)),
        SpaceCompassConstellationStar("Alpherg", SpaceCompassDeepSkyReference(1.524725, 15.345823, 349.5779933390147, 25.73, -3.29)),
        SpaceCompassConstellationStar("Torcular", SpaceCompassDeepSkyReference(1.756564, 9.157736, 279.4827477383746, 70.82, 38.99)),
        SpaceCompassConstellationStar("Alrescha", SpaceCompassDeepSkyReference(2.034117, 2.763759, 150.58020261291372, 33.29, -0.42)),
        SpaceCompassConstellationStar("111Xi  Psc", SpaceCompassDeepSkyReference(1.892597, 3.187536, 179.10844865757733, 23.69, 23.9)),
        SpaceCompassConstellationStar("106Nu  Psc", SpaceCompassDeepSkyReference(1.690526, 5.487613, 363.2031760174322, -22.73, 3.61)),
        SpaceCompassConstellationStar("98Mu  Psc", SpaceCompassDeepSkyReference(1.503087, 6.14382, 303.9666546995582, 293.23, -46.36)),
        SpaceCompassConstellationStar("71Eps Psc", SpaceCompassDeepSkyReference(1.049058, 7.890135, 181.8041311192678, -80.57, 25.88)),
        SpaceCompassConstellationStar("63Del Psc", SpaceCompassDeepSkyReference(0.811373, 7.585079, 311.2177632885846, 83.14, -50.48)),
        SpaceCompassConstellationStar("41    Psc", SpaceCompassDeepSkyReference(0.343295, 8.190271, 407.69547212500004, -4.5, 9.59)),
        SpaceCompassConstellationStar("28Ome Psc", SpaceCompassDeepSkyReference(23.988525, 6.863321, 104.3367729134746, 148.49, -112.16)),
        SpaceCompassConstellationStar("17Iot Psc", SpaceCompassDeepSkyReference(23.665844, 5.626292, 44.7281071686449, 376.32, -437.0)),
        SpaceCompassConstellationStar("18Lam Psc", SpaceCompassDeepSkyReference(23.700779, 1.780041, 106.6218244956408, -129.55, -154.87)),
        SpaceCompassConstellationStar("8Kap Psc", SpaceCompassDeepSkyReference(23.448876, 1.255608, 153.4852774690876, 85.6, -94.43)),
        SpaceCompassConstellationStar("6Gam Psc", SpaceCompassDeepSkyReference(23.286094, 3.282289, 137.9680616436324, 760.35, 17.96)),
        SpaceCompassConstellationStar("10The Psc", SpaceCompassDeepSkyReference(23.466138, 6.378992, 148.5228081823821, -123.83, -43.26)),
    ),
    edges = listOf(0 to 1, 0 to 2, 2 to 1, 1 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 10 to 11, 11 to 12, 12 to 13, 13 to 14, 14 to 15, 15 to 16, 16 to 17, 17 to 13)
)
