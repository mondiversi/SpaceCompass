package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternLUP() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("5Chi Lup", SpaceCompassDeepSkyReference(15.849316, -33.627165, 195.1863272962988, -5.92, -24.91)),
        SpaceCompassConstellationStar("HIP 78970", SpaceCompassDeepSkyReference(16.121168, -36.755676, 178.7157563788265, 52.54, -56.45)),
        SpaceCompassConstellationStar("Eta Lup", SpaceCompassDeepSkyReference(16.002036, -38.396706, 441.94645797278775, -16.58, -27.06)),
        SpaceCompassConstellationStar("Gam Lup", SpaceCompassDeepSkyReference(15.585681, -41.166757, 420.8470757429971, -16.05, -25.52)),
        SpaceCompassConstellationStar("Del Lup", SpaceCompassDeepSkyReference(15.356201, -40.647518, 883.8925897891979, -19.11, -24.05)),
        SpaceCompassConstellationStar("Phi1Lup", SpaceCompassDeepSkyReference(15.36344, -36.261376, 275.005272985309, -91.85, -86.03)),
        SpaceCompassConstellationStar("Bet Lup", SpaceCompassDeepSkyReference(14.975537, -43.13396, 382.81267591388934, -34.06, -38.3)),
        SpaceCompassConstellationStar("Ome Lup", SpaceCompassDeepSkyReference(15.634232, -42.567348, 218.60337674602573, -147.6, 56.35)),
        SpaceCompassConstellationStar("Zet Lup", SpaceCompassDeepSkyReference(15.20476, -52.099247, 117.32236293522242, -113.94, -71.0)),
        SpaceCompassConstellationStar("Alp Lup", SpaceCompassDeepSkyReference(14.698823, -47.3882, 464.6100861900277, -21.15, -24.22)),
        SpaceCompassConstellationStar("Rho Lup", SpaceCompassDeepSkyReference(14.631454, -49.425828, 316.04292074027836, -27.78, -29.05)),
        SpaceCompassConstellationStar("Tau2Lup", SpaceCompassDeepSkyReference(14.436336, -45.379276, 319.1355355136298, 13.55, -6.35)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0, 2 to 3, 3 to 4, 4 to 5, 4 to 6, 3 to 7, 7 to 8, 8 to 9, 8 to 10, 9 to 11, 9 to 6)
)

internal fun spaceCompassPatternLYN() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("40Alp Lyn", SpaceCompassDeepSkyReference(9.350925, 34.392562, 203.0861609205705, -222.8, 14.78)),
        SpaceCompassConstellationStar("38    Lyn", SpaceCompassDeepSkyReference(9.314069, 36.802597, 124.82069805854542, -32.61, -123.78)),
        SpaceCompassConstellationStar("HIP 44700", SpaceCompassDeepSkyReference(9.108825, 38.452215, 817.4346401126683, -28.37, -14.34)),
        SpaceCompassConstellationStar("HIP 44248", SpaceCompassDeepSkyReference(9.010685, 41.782911, 52.411372958123806, -487.67, -219.29)),
        SpaceCompassConstellationStar("Alsciaukat", SpaceCompassDeepSkyReference(8.380588, 43.188131, 382.36388473817414, -25.62, -99.44)),
        SpaceCompassConstellationStar("21    Lyn", SpaceCompassDeepSkyReference(7.445237, 49.211527, 273.6210653183502, -10.79, -48.57)),
        SpaceCompassConstellationStar("15    Lyn", SpaceCompassDeepSkyReference(6.954612, 58.422759, 178.3250210383419, 6.58, -123.53)),
        SpaceCompassConstellationStar("2    Lyn", SpaceCompassDeepSkyReference(6.327052, 59.010964, 156.5801753370829, -3.51, 24.43)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7)
)

internal fun spaceCompassPatternLYR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Vega", SpaceCompassDeepSkyReference(18.61564, 38.783692, 25.0445697744499, 201.02, 287.46)),
        SpaceCompassConstellationStar("6Zet1Lyr", SpaceCompassDeepSkyReference(18.746209, 37.605115, 156.1304056922346, 29.26, 26.57)),
        SpaceCompassConstellationStar("Sheliak", SpaceCompassDeepSkyReference(18.834665, 33.362667, 962.1133692274781, 1.1, -4.46)),
        SpaceCompassConstellationStar("Sulafat", SpaceCompassDeepSkyReference(18.982395, 32.689557, 620.0692620569557, -2.76, 1.77)),
        SpaceCompassConstellationStar("12Del2Lyr", SpaceCompassDeepSkyReference(18.908412, 36.898613, 736.2445330118072, -6.73, 3.26)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 1)
)

internal fun spaceCompassPatternANT() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Ant", SpaceCompassDeepSkyReference(10.45253, -31.067779, 366.0563920095518, -80.42, 9.63)),
        SpaceCompassConstellationStar("Eta Ant", SpaceCompassDeepSkyReference(9.981191, -35.890973, 108.64627713202471, -89.59, -16.27)),
    ),
    edges = listOf(0 to 1)
)

internal fun spaceCompassPatternMIC() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Eps Mic", SpaceCompassDeepSkyReference(21.298966, -32.172539, 182.2101958095043, 55.46, -22.12)),
        SpaceCompassConstellationStar("Gam Mic", SpaceCompassDeepSkyReference(21.021517, -32.257767, 229.04233777069192, -2.09, -0.19)),
        SpaceCompassConstellationStar("Alp Mic", SpaceCompassDeepSkyReference(20.8328, -33.779722, 378.3717306751261, 3.71, -20.19)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)

internal fun spaceCompassPatternMUS() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Bet Mus", SpaceCompassDeepSkyReference(12.771346, -68.108119, 341.52486621722403, -40.4, -10.32)),
        SpaceCompassConstellationStar("Lam Mus", SpaceCompassDeepSkyReference(11.760141, -66.728763, 127.15663003563282, -100.42, 33.21)),
        SpaceCompassConstellationStar("Gam Mus", SpaceCompassDeepSkyReference(12.541134, -72.132988, 324.85697069124325, -50.59, -5.16)),
        SpaceCompassConstellationStar("Alp Mus", SpaceCompassDeepSkyReference(12.619739, -69.135564, 315.4317036884686, -39.87, -12.44)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
)

internal fun spaceCompassPatternOCT() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Nu  Oct", SpaceCompassDeepSkyReference(21.691253, -77.390046, 69.1448259160223, 64.83, -240.38)),
        SpaceCompassConstellationStar("Bet Oct", SpaceCompassDeepSkyReference(22.767692, -81.381615, 149.2706847564482, -55.35, 0.88)),
        SpaceCompassConstellationStar("Del Oct", SpaceCompassDeepSkyReference(14.448801, -83.667884, 298.951674236043, -95.79, -12.91)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0)
)
