package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternAQL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alshain", SpaceCompassDeepSkyReference(19.921887, 6.406763, 44.678857555612204, 46.35, -481.32)),
        SpaceCompassConstellationStar("Altair", SpaceCompassDeepSkyReference(19.846388, 8.868322, 16.7301913941215, 536.82, 385.54)),
        SpaceCompassConstellationStar("Tarazed", SpaceCompassDeepSkyReference(19.770994, 10.613261, 394.8625232880158, 15.72, -3.08)),
        SpaceCompassConstellationStar("30Del Aql", SpaceCompassDeepSkyReference(19.424972, 3.114775, 50.6374084198135, 253.06, 80.67)),
        SpaceCompassConstellationStar("55Eta Aql", SpaceCompassDeepSkyReference(19.874547, 1.005661, 1382.0185053516775, 6.94, -7.3)),
        SpaceCompassConstellationStar("65The Aql", SpaceCompassDeepSkyReference(20.188413, -0.821461, 286.3532318346251, 35.48, 6.05)),
        SpaceCompassConstellationStar("Okab", SpaceCompassDeepSkyReference(19.090169, 13.863478, 83.03354294762141, -7.04, -95.31)),
        SpaceCompassConstellationStar("13Eps Aql", SpaceCompassDeepSkyReference(18.993711, 15.068298, 154.94352263378428, -52.68, -73.81)),
        SpaceCompassConstellationStar("16Lam Aql", SpaceCompassDeepSkyReference(19.10415, -4.882554, 123.6846953950163, -19.68, -90.37)),
    ),
    edges = listOf(0 to 1, 1 to 2, 1 to 3, 3 to 4, 5 to 4, 3 to 6, 6 to 7, 3 to 8)
)

internal fun spaceCompassPatternAND() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alpheratz", SpaceCompassDeepSkyReference(0.139791, 29.090432, 97.0126052958434, 135.68, -162.95)),
        SpaceCompassConstellationStar("31Del And", SpaceCompassDeepSkyReference(0.655462, 30.861024, 105.518111313504, 115.38, -83.05)),
        SpaceCompassConstellationStar("Mirach", SpaceCompassDeepSkyReference(1.162194, 35.620558, 197.4312616440079, 175.59, -112.23)),
        SpaceCompassConstellationStar("Almach", SpaceCompassDeepSkyReference(2.064984, 42.329725, 392.9594008241363, 43.08, -50.85)),
        SpaceCompassConstellationStar("37Mu  And", SpaceCompassDeepSkyReference(0.945885, 38.499345, 129.7358746704844, 152.78, 36.82)),
        SpaceCompassConstellationStar("35Nu  And", SpaceCompassDeepSkyReference(0.830234, 41.078911, 617.7202838247604, 22.68, -18.05)),
    ),
    edges = listOf(0 to 1, 1 to 2, 3 to 2, 2 to 4, 4 to 5)
)

internal fun spaceCompassPatternSCL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Bet Scl", SpaceCompassDeepSkyReference(23.549512, -37.818268, 174.0429139555186, 96.39, 37.58)),
        SpaceCompassConstellationStar("Alp Scl", SpaceCompassDeepSkyReference(0.976766, -29.357449, 776.5626797975705, 22.44, 6.3)),
        SpaceCompassConstellationStar("Gam Scl", SpaceCompassDeepSkyReference(23.313733, -32.532027, 182.2101958095043, 19.65, -78.57)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0)
)

internal fun spaceCompassPatternARA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("The Ara", SpaceCompassDeepSkyReference(18.11052, -50.091477, 813.3576853914182, -8.43, -9.26)),
        SpaceCompassConstellationStar("Alp Ara", SpaceCompassDeepSkyReference(17.530695, -49.876145, 267.3412504221144, -31.27, -67.15)),
        SpaceCompassConstellationStar("Zet Ara", SpaceCompassDeepSkyReference(16.977006, -55.990141, 486.0750897192201, -18.31, -35.29)),
        SpaceCompassConstellationStar("Eta Ara", SpaceCompassDeepSkyReference(16.82976, -59.041378, 299.2259717496887, 39.59, -25.28)),
        SpaceCompassConstellationStar("Del Ara", SpaceCompassDeepSkyReference(17.518318, -60.683848, 197.9103853628492, -53.65, -99.37)),
        SpaceCompassConstellationStar("Gam Ara", SpaceCompassDeepSkyReference(17.423239, -56.377727, 1113.1616062423914, -0.77, -15.85)),
        SpaceCompassConstellationStar("Bet Ara", SpaceCompassDeepSkyReference(17.421665, -55.529884, 645.8542068087846, -8.23, -24.71)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 0)
)

internal fun spaceCompassPatternLIB() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("46The Lib", SpaceCompassDeepSkyReference(15.897093, -16.729293, 168.46922761700333, 100.12, 135.27)),
        SpaceCompassConstellationStar("Zubenelhakrabi", SpaceCompassDeepSkyReference(15.592105, -14.789537, 163.15972794442501, 65.67, 6.93)),
        SpaceCompassConstellationStar("Zubeneschamali", SpaceCompassDeepSkyReference(15.283449, -9.382917, 185.1058121307249, -96.39, -20.76)),
        SpaceCompassConstellationStar("Zubenelgenubi", SpaceCompassDeepSkyReference(14.847977, -16.041778, 75.7974375519692, -105.69, -69.0)),
        SpaceCompassConstellationStar("Brachium", SpaceCompassDeepSkyReference(15.067839, -25.281965, 288.3786629401421, -71.85, -44.69)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 1)
)

internal fun spaceCompassPatternCET() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("65Xi 1Cet", SpaceCompassDeepSkyReference(2.216665, 8.846717, 383.26244555873757, -26.23, -14.4)),
        SpaceCompassConstellationStar("73Xi 2Cet", SpaceCompassDeepSkyReference(2.469317, 8.460054, 193.1061019193282, 41.72, -14.46)),
        SpaceCompassConstellationStar("52Tau Cet", SpaceCompassDeepSkyReference(1.734479, -15.93748, 11.9053600988054, -1721.82, 854.07)),
        SpaceCompassConstellationStar("Diphda", SpaceCompassDeepSkyReference(0.72649, -17.986605, 96.3250676516518, 232.79, 32.71)),
        SpaceCompassConstellationStar("8Iot Cet", SpaceCompassDeepSkyReference(0.323799, -8.823921, 274.54245708535274, -14.37, -37.84)),
        SpaceCompassConstellationStar("31Eta Cet", SpaceCompassDeepSkyReference(1.143164, -10.182264, 123.91952798696029, 215.81, -138.33)),
        SpaceCompassConstellationStar("45The Cet", SpaceCompassDeepSkyReference(1.40039, -8.183257, 113.80183099432861, -78.38, -206.88)),
        SpaceCompassConstellationStar("Baten Kaitos", SpaceCompassDeepSkyReference(1.857676, -10.335038, 234.98295003411968, 38.78, -38.04)),
        SpaceCompassConstellationStar("72Rho Cet", SpaceCompassDeepSkyReference(2.432501, -12.29048, 456.16263600759765, -11.71, -11.37)),
        SpaceCompassConstellationStar("83Eps Cet", SpaceCompassDeepSkyReference(2.659398, -11.872156, 70.06589152664709, 172.01, -236.31)),
        SpaceCompassConstellationStar("89Pi  Cet", SpaceCompassDeepSkyReference(2.735375, -13.858696, 392.9594008241363, -7.58, -8.4)),
        SpaceCompassConstellationStar("76Sig Cet", SpaceCompassDeepSkyReference(2.534786, -15.244677, 87.06777118339271, -80.92, -146.84)),
        SpaceCompassConstellationStar("Mira", SpaceCompassDeepSkyReference(2.322442, -2.977643, 298.951674236043, 10.33, -239.48)),
        SpaceCompassConstellationStar("82Del Cet", SpaceCompassDeepSkyReference(2.658044, 0.328511, 649.7139413824865, 14.44, -2.53)),
        SpaceCompassConstellationStar("Kaffaljidhma", SpaceCompassDeepSkyReference(2.721678, 3.235818, 79.6085748253937, -146.43, -145.27)),
        SpaceCompassConstellationStar("Menkar", SpaceCompassDeepSkyReference(3.037992, 4.089734, 249.1645554928934, -11.81, -78.76)),
        SpaceCompassConstellationStar("91Lam Cet", SpaceCompassDeepSkyReference(2.99525, 8.907365, 576.2478696183168, 9.22, -14.92)),
        SpaceCompassConstellationStar("87Mu  Cet", SpaceCompassDeepSkyReference(2.749039, 10.114146, 84.0609355373764, 285.17, -30.4)),
        SpaceCompassConstellationStar("78Nu  Cet", SpaceCompassDeepSkyReference(2.597909, 5.593246, 340.10054131580813, -28.89, -22.76)),
    ),
    edges = listOf(0 to 1, 2 to 3, 3 to 4, 3 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 10 to 11, 11 to 2, 12 to 9, 12 to 13, 13 to 14, 14 to 15, 15 to 16, 16 to 17, 17 to 1, 1 to 18, 18 to 14)
)

internal fun spaceCompassPatternARI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Bharani", SpaceCompassDeepSkyReference(2.833063, 27.260507, 165.6456918552544, 65.47, -116.59)),
        SpaceCompassConstellationStar("Hamal", SpaceCompassDeepSkyReference(2.119555, 23.462423, 65.81052926679521, 190.73, -145.77)),
        SpaceCompassConstellationStar("Sheratan", SpaceCompassDeepSkyReference(1.910668, 20.808035, 58.66118146761121, 96.32, -108.8)),
        SpaceCompassConstellationStar("Mesarthim", SpaceCompassDeepSkyReference(1.89217, 19.293852, 164.0625287978986, 79.43, -99.1)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3)
)
