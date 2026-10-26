package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternDEL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Aldulfin", SpaceCompassDeepSkyReference(20.553547, 11.303263, 330.4521833506867, 10.75, -28.54)),
        SpaceCompassConstellationStar("Rotanev", SpaceCompassDeepSkyReference(20.625816, 14.595087, 100.883429186387, 118.28, -47.65)),
        SpaceCompassConstellationStar("Sualocin", SpaceCompassDeepSkyReference(20.660635, 15.912072, 253.818154689917, 54.14, 7.91)),
        SpaceCompassConstellationStar("12Gam2Del", SpaceCompassDeepSkyReference(20.77764, 16.124296, 126.3193866140769, -25.88, -196.27)),
        SpaceCompassConstellationStar("11Del Del", SpaceCompassDeepSkyReference(20.724315, 15.074581, 223.24197274967509, -19.61, -41.74)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 1)
)

internal fun spaceCompassPatternDOR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Del Dor", SpaceCompassDeepSkyReference(5.746223, -65.735526, 149.6131489530332, -28.38, 6.12)),
        SpaceCompassConstellationStar("HIP 27890", SpaceCompassDeepSkyReference(5.901658, -63.089627, 85.6052859857859, 120.11, 573.43)),
        SpaceCompassConstellationStar("Bet Dor", SpaceCompassDeepSkyReference(5.560421, -62.489825, 1006.655567260834, 1.06, 12.56)),
        SpaceCompassConstellationStar("Alp Dor", SpaceCompassDeepSkyReference(4.566598, -55.044975, 168.6433951226951, 58.06, 12.73)),
        SpaceCompassConstellationStar("Gam Dor", SpaceCompassDeepSkyReference(4.267097, -51.486648, 66.73974878686249, 100.57, 184.23)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0, 2 to 3, 3 to 4)
)

internal fun spaceCompassPatternDRA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Grumium", SpaceCompassDeepSkyReference(17.892134, 56.872643, 112.5454766274282, 93.65, 78.44)),
        SpaceCompassConstellationStar("Eltanin", SpaceCompassDeepSkyReference(17.943437, 51.488895, 154.2840344380749, -8.52, -23.05)),
        SpaceCompassConstellationStar("Rastaban", SpaceCompassDeepSkyReference(17.507213, 52.301387, 380.1355843657277, -15.59, 11.57)),
        SpaceCompassConstellationStar("25Nu 2Dra", SpaceCompassDeepSkyReference(17.537767, 55.172958, 99.4379041204206, 143.71, 62.46)),
        SpaceCompassConstellationStar("Altais", SpaceCompassDeepSkyReference(19.209225, 67.661541, 97.41834382970221, 94.49, 92.3)),
        SpaceCompassConstellationStar("63Eps Dra", SpaceCompassDeepSkyReference(19.80285, 70.26793, 147.98399784642172, 80.18, 39.02)),
        SpaceCompassConstellationStar("60Tau Dra", SpaceCompassDeepSkyReference(19.259229, 73.355468, 146.3897454722241, -116.6, 104.25)),
        SpaceCompassConstellationStar("44Chi Dra", SpaceCompassDeepSkyReference(18.350736, 72.732843, 26.2797239767998, 531.08, -351.59)),
        SpaceCompassConstellationStar("Aldhibah", SpaceCompassDeepSkyReference(17.146448, 65.714683, 328.4554540064073, -20.76, 19.15)),
        SpaceCompassConstellationStar("Athebyne", SpaceCompassDeepSkyReference(16.39986, 61.514213, 92.08242549053021, -16.98, 56.68)),
        SpaceCompassConstellationStar("13The Dra", SpaceCompassDeepSkyReference(16.031532, 58.565251, 68.6066678928173, -320.07, 334.96)),
        SpaceCompassConstellationStar("Edasich", SpaceCompassDeepSkyReference(15.415494, 58.966065, 101.19653930897901, -8.27, 17.3)),
        SpaceCompassConstellationStar("Thuban", SpaceCompassDeepSkyReference(14.073165, 64.37585, 303.1193004302936, -56.52, 17.19)),
        SpaceCompassConstellationStar("5Kap Dra", SpaceCompassDeepSkyReference(12.558058, 69.788238, 490.4605883737743, -58.14, 11.42)),
        SpaceCompassConstellationStar("Giausar", SpaceCompassDeepSkyReference(11.523407, 69.331076, 333.152432001665, -41.14, -18.79)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 0 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 10 to 11, 11 to 12, 12 to 13, 13 to 14)
)

internal fun spaceCompassPatternNOR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Kap Nor", SpaceCompassDeepSkyReference(16.224648, -54.630467, 428.0267560853072, -5.33, -22.48)),
        SpaceCompassConstellationStar("Gam2Nor", SpaceCompassDeepSkyReference(16.330687, -50.155508, 128.7629501958053, -159.36, -52.84)),
        SpaceCompassConstellationStar("Eps Nor", SpaceCompassDeepSkyReference(16.453067, -47.554786, 530.3354886422431, -13.72, -20.63)),
        SpaceCompassConstellationStar("Eta Nor", SpaceCompassDeepSkyReference(16.053579, -49.229695, 219.4862820604596, 41.18, 10.39)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 1, 3 to 0)
)

internal fun spaceCompassPatternERI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Achernar", SpaceCompassDeepSkyReference(1.628556, -57.236757, 139.4426146272141, 88.02, -40.08)),
        SpaceCompassConstellationStar("Chi Eri", SpaceCompassDeepSkyReference(1.932564, -51.608896, 58.2215226704716, 681.24, 284.23)),
        SpaceCompassConstellationStar("Phi Eri", SpaceCompassDeepSkyReference(2.275154, -51.512165, 153.7024976166358, 90.75, -21.9)),
        SpaceCompassConstellationStar("Kap Eri", SpaceCompassDeepSkyReference(2.449755, -47.70384, 508.03161090960646, 19.97, -5.44)),
        SpaceCompassConstellationStar("HIP 12413", SpaceCompassDeepSkyReference(2.663326, -42.89167, 116.4012973245976, 92.11, -15.07)),
        SpaceCompassConstellationStar("Iot Eri", SpaceCompassDeepSkyReference(2.677781, -39.855375, 150.6496739213638, 135.17, -27.34)),
        SpaceCompassConstellationStar("Acamar", SpaceCompassDeepSkyReference(2.971023, -40.304672, 161.2239898427755, -53.53, 25.71)),
        SpaceCompassConstellationStar("82 G. Eri", SpaceCompassDeepSkyReference(3.331944, -43.069784, 19.7109345299218, 3038.08, 726.34)),
        SpaceCompassConstellationStar("HIP 17797", SpaceCompassDeepSkyReference(3.809963, -37.620155, 165.4777213207389, 75.83, -10.69)),
        SpaceCompassConstellationStar("HIP 17874", SpaceCompassDeepSkyReference(3.824237, -36.20025, 209.8819552063277, -49.42, -56.63)),
        SpaceCompassConstellationStar("41Ups4Eri", SpaceCompassDeepSkyReference(4.298237, -33.798348, 177.9359164797458, 62.31, -6.75)),
        SpaceCompassConstellationStar("Beemim", SpaceCompassDeepSkyReference(4.400613, -34.016846, 296.2364223916905, 73.77, 57.57)),
        SpaceCompassConstellationStar("Theemin", SpaceCompassDeepSkyReference(4.592512, -30.562341, 213.87313080024262, -48.92, -12.75)),
        SpaceCompassConstellationStar("27Tau6Eri", SpaceCompassDeepSkyReference(3.780804, -23.249723, 57.492889322689805, -159.86, -528.54)),
        SpaceCompassConstellationStar("19Tau5Eri", SpaceCompassDeepSkyReference(3.563132, -21.632883, 293.3062334944337, 44.91, -27.47)),
        SpaceCompassConstellationStar("16Tau4Eri", SpaceCompassDeepSkyReference(3.325278, -21.757864, 304.5344929531339, 51.35, 32.2)),
        SpaceCompassConstellationStar("11Tau3Eri", SpaceCompassDeepSkyReference(3.039863, -23.624472, 88.6294079198203, -145.96, -55.76)),
        SpaceCompassConstellationStar("1Tau1Eri", SpaceCompassDeepSkyReference(2.751715, -18.572563, 46.3817200035839, 330.93, 36.28)),
        SpaceCompassConstellationStar("Azha", SpaceCompassDeepSkyReference(2.940458, -8.898144, 136.5241673595545, 77.73, -219.99)),
        SpaceCompassConstellationStar("Zibal", SpaceCompassDeepSkyReference(3.263896, -8.81973, 109.7431410302298, -3.82, 45.52)),
        SpaceCompassConstellationStar("Ran", SpaceCompassDeepSkyReference(3.548848, -9.458262, 10.4895152632097, -976.44, 17.97)),
        SpaceCompassConstellationStar("Rana", SpaceCompassDeepSkyReference(3.720806, -9.763395, 29.487145795101604, -91.71, 742.23)),
        SpaceCompassConstellationStar("48Nu  Eri", SpaceCompassDeepSkyReference(4.605317, -3.352459, 675.2718812954361, 1.73, -4.54)),
        SpaceCompassConstellationStar("57Mu  Eri", SpaceCompassDeepSkyReference(4.758375, -3.254657, 521.85020432, 17.27, -13.51)),
        SpaceCompassConstellationStar("61Ome Eri", SpaceCompassDeepSkyReference(4.881575, -5.452695, 234.98295003411968, -18.01, 25.03)),
        SpaceCompassConstellationStar("Cursa", SpaceCompassDeepSkyReference(5.130829, -5.086446, 89.3580412676021, -83.39, -75.44)),
        SpaceCompassConstellationStar("69Lam Eri", SpaceCompassDeepSkyReference(5.15244, -8.754081, 811.3342112241675, 0.0, -2.01)),
        SpaceCompassConstellationStar("Sceptrum", SpaceCompassDeepSkyReference(4.63634, -14.30402, 109.8540341986478, -77.86, -178.06)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 10 to 11, 11 to 12, 12 to 13, 13 to 14, 14 to 15, 15 to 16, 16 to 17, 17 to 18, 18 to 19, 19 to 20, 20 to 21, 21 to 22, 22 to 23, 23 to 24, 24 to 25, 25 to 26, 26 to 27)
)

internal fun spaceCompassPatternSGE() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("6Bet Sge", SpaceCompassDeepSkyReference(19.68415, 17.476041, 439.5638856336894, 9.06, -33.9)),
        SpaceCompassConstellationStar("7Del Sge", SpaceCompassDeepSkyReference(19.789795, 18.534286, 594.0918850422838, -4.58, 11.1)),
        SpaceCompassConstellationStar("Sham", SpaceCompassDeepSkyReference(19.668275, 18.01389, 425.2364882740837, 15.09, -19.72)),
        SpaceCompassConstellationStar("12Gam Sge", SpaceCompassDeepSkyReference(19.979285, 19.492148, 258.44403059483614, 64.37, 22.58)),
        SpaceCompassConstellationStar("16Eta Sge", SpaceCompassDeepSkyReference(20.08597, 19.991071, 160.35217384518342, 30.61, 79.65)),
    ),
    edges = listOf(0 to 1, 1 to 2, 1 to 3, 3 to 4)
)

internal fun spaceCompassPatternFOR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Bet For", SpaceCompassDeepSkyReference(2.818169, -32.405898, 172.6609893832037, 86.96, 158.96)),
        SpaceCompassConstellationStar("Dalim", SpaceCompassDeepSkyReference(3.201249, -28.987618, 46.434557336771306, 371.49, 612.26)),
    ),
    edges = listOf(0 to 1)
)
