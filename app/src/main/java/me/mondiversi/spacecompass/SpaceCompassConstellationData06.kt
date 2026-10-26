package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternGEM() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alhena", SpaceCompassDeepSkyReference(6.628528, 16.399252, 109.3018514512017, -2.04, -66.92)),
        SpaceCompassConstellationStar("Mekbuda", SpaceCompassDeepSkyReference(7.068481, 20.570297, 1376.1871554747793, -5.72, -0.96)),
        SpaceCompassConstellationStar("Wasat", SpaceCompassDeepSkyReference(7.335383, 21.98232, 60.466457018180705, -18.72, -7.76)),
        SpaceCompassConstellationStar("54Lam Gem", SpaceCompassDeepSkyReference(7.30155, 16.540383, 100.883429186387, -46.09, -37.9)),
        SpaceCompassConstellationStar("Alzirr", SpaceCompassDeepSkyReference(6.754824, 12.895591, 58.7035817967122, -115.16, -190.91)),
        SpaceCompassConstellationStar("69Ups Gem", SpaceCompassDeepSkyReference(7.598708, 26.895741, 270.8940718444005, -39.58, -108.08)),
        SpaceCompassConstellationStar("77Kap Gem", SpaceCompassDeepSkyReference(7.740793, 24.397993, 141.3767219469751, -22.45, -56.24)),
        SpaceCompassConstellationStar("Pollux", SpaceCompassDeepSkyReference(7.755277, 28.026199, 33.7845822276768, -625.69, -45.95)),
        SpaceCompassConstellationStar("60Iot Gem", SpaceCompassDeepSkyReference(7.428779, 27.79808, 120.3530079968108, -121.28, -84.43)),
        SpaceCompassConstellationStar("46Tau Gem", SpaceCompassDeepSkyReference(7.185659, 30.245163, 321.02006706398043, -31.0, -48.4)),
        SpaceCompassConstellationStar("Castor", SpaceCompassDeepSkyReference(7.576634, 31.888276, 50.8666963533366, -206.33, -148.18)),
        SpaceCompassConstellationStar("34The Gem", SpaceCompassDeepSkyReference(6.879816, 33.961254, 189.076113716467, -2.66, -47.67)),
        SpaceCompassConstellationStar("Mebsuta", SpaceCompassDeepSkyReference(6.732202, 25.131124, 844.9648476415699, -5.93, -12.81)),
        SpaceCompassConstellationStar("18Nu  Gem", SpaceCompassDeepSkyReference(6.482719, 20.212133, 544.5014385948873, -5.98, -14.08)),
        SpaceCompassConstellationStar("Tejat", SpaceCompassDeepSkyReference(6.382673, 22.513586, 231.6450656647379, 56.84, -108.79)),
        SpaceCompassConstellationStar("Propus", SpaceCompassDeepSkyReference(6.247961, 22.506799, 384.6182776208365, -62.53, -10.24)),
        SpaceCompassConstellationStar("1    Gem", SpaceCompassDeepSkyReference(6.068671, 23.263341, 155.0909453165047, -2.6, -119.72)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 2 to 5, 5 to 6, 5 to 7, 5 to 8, 8 to 9, 9 to 10, 9 to 11, 9 to 12, 12 to 13, 12 to 14, 14 to 15, 15 to 16)
)

internal fun spaceCompassPatternCAM() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("HIP 16228", SpaceCompassDeepSkyReference(3.484482, 59.94033, 1941.407025650304, -0.82, -1.85)),
        SpaceCompassConstellationStar("HIP 18505", SpaceCompassDeepSkyReference(3.957067, 63.072264, 360.79255422985153, 7.58, 6.01)),
        SpaceCompassConstellationStar("9Alp Cam", SpaceCompassDeepSkyReference(4.900836, 66.342678, Double.NaN, 0.49, 7.31, hasMeasuredDistance = false)),
        SpaceCompassConstellationStar("Gam Cam", SpaceCompassDeepSkyReference(3.839302, 71.332266, 358.807892671547, 18.27, -42.0)),
        SpaceCompassConstellationStar("HIP 25110", SpaceCompassDeepSkyReference(5.376036, 79.231149, 68.11939026453351, -78.6, 161.13)),
    ),
    edges = listOf(0 to 1, 1 to 2, 0 to 3, 3 to 2, 3 to 4)
)

internal fun spaceCompassPatternCMA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("14The CMa", SpaceCompassDeepSkyReference(6.903167, -12.038628, 260.7166882346497, -139.04, -14.47)),
        SpaceCompassConstellationStar("Muliphein", SpaceCompassDeepSkyReference(7.062637, -15.633286, 441.94645797278775, -0.81, -11.2)),
        SpaceCompassConstellationStar("20Iot CMa", SpaceCompassDeepSkyReference(6.935618, -17.054242, 2508.8953134327317, -2.46, 1.91)),
        SpaceCompassConstellationStar("Sirius", SpaceCompassDeepSkyReference(6.752481, -16.716116, 8.601069836326701, -546.01, -1223.08)),
        SpaceCompassConstellationStar("24Omi2CMa", SpaceCompassDeepSkyReference(7.050409, -23.833291, 2764.037010703355, -1.56, 4.28)),
        SpaceCompassConstellationStar("Wezen", SpaceCompassDeepSkyReference(7.139857, -26.3932, 1606.6815414389916, -2.75, 3.33)),
        SpaceCompassConstellationStar("28Ome CMa", SpaceCompassDeepSkyReference(7.246849, -26.772667, 911.0513052038993, -11.5, 7.67)),
        SpaceCompassConstellationStar("Aludra", SpaceCompassDeepSkyReference(7.401584, -29.303104, 1988.7584085647895, -3.76, 6.66)),
        SpaceCompassConstellationStar("Adhara", SpaceCompassDeepSkyReference(6.977097, -28.972084, 405.16319400853723, 2.63, 2.29)),
        SpaceCompassConstellationStar("Nganurganity", SpaceCompassDeepSkyReference(7.028652, -27.93483, 1120.8122563941004, -6.01, 4.64)),
        SpaceCompassConstellationStar("16Omi1CMa", SpaceCompassDeepSkyReference(6.902209, -24.184211, Double.NaN, -3.46, 5.02, hasMeasuredDistance = false)),
        SpaceCompassConstellationStar("7Nu 2CMa", SpaceCompassDeepSkyReference(6.611399, -19.255879, 64.4194723159047, 62.32, -69.91)),
        SpaceCompassConstellationStar("5Xi 2CMa", SpaceCompassDeepSkyReference(6.584274, -22.964793, 441.34828717608605, 13.11, 16.52)),
        SpaceCompassConstellationStar("Mirzam", SpaceCompassDeepSkyReference(6.378329, -17.955918, 492.6833440877998, -3.45, -0.47)),
        SpaceCompassConstellationStar("Furud", SpaceCompassDeepSkyReference(6.338553, -30.063367, 362.39593898262467, 8.0, 3.81)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 8 to 9, 9 to 5, 9 to 10, 10 to 11, 11 to 12, 3 to 13, 11 to 3, 14 to 8, 2 to 0)
)

internal fun spaceCompassPatternUMA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alkaid", SpaceCompassDeepSkyReference(13.792354, 49.313265, 103.9375575071698, -121.23, -15.56)),
        SpaceCompassConstellationStar("Mizar", SpaceCompassDeepSkyReference(13.398747, 54.925362, 85.80815525271531, 121.23, -22.01)),
        SpaceCompassConstellationStar("Alioth", SpaceCompassDeepSkyReference(12.900472, 55.959821, 82.55017919587, 111.74, -8.99)),
        SpaceCompassConstellationStar("Megrez", SpaceCompassDeepSkyReference(12.257086, 57.032617, 80.51268030437811, 103.56, 7.81)),
        SpaceCompassConstellationStar("Dubhe", SpaceCompassDeepSkyReference(11.062155, 61.751033, 122.892461553583, -136.46, -35.25)),
        SpaceCompassConstellationStar("Merak", SpaceCompassDeepSkyReference(11.030677, 56.382427, 79.7449081912723, 81.66, 33.74)),
        SpaceCompassConstellationStar("Phecda", SpaceCompassDeepSkyReference(11.897168, 53.69476, 83.1819440994749, 107.76, 11.16)),
        SpaceCompassConstellationStar("Taiyangshou", SpaceCompassDeepSkyReference(11.767515, 47.779406, 183.6465884968951, -138.38, 28.37)),
        SpaceCompassConstellationStar("52Psi UMa", SpaceCompassDeepSkyReference(11.161062, 44.498487, 144.5088016420282, -62.35, -27.38)),
        SpaceCompassConstellationStar("Tania Borealis", SpaceCompassDeepSkyReference(10.284952, 42.914365, 137.5026364926545, -170.11, -42.64)),
        SpaceCompassConstellationStar("Tania Australis", SpaceCompassDeepSkyReference(10.372155, 41.499516, 230.3365262774055, -80.47, 34.1)),
        SpaceCompassConstellationStar("30Phi UMa", SpaceCompassDeepSkyReference(9.868433, 54.064332, 508.82417090741745, -4.21, 19.18)),
        SpaceCompassConstellationStar("25The UMa", SpaceCompassDeepSkyReference(9.547715, 51.6773, 43.9622919938053, -947.06, -535.6)),
        SpaceCompassConstellationStar("Alkaphrah", SpaceCompassDeepSkyReference(9.060427, 47.156525, 358.4135696109077, -37.37, -55.39)),
        SpaceCompassConstellationStar("Talitha", SpaceCompassDeepSkyReference(8.986828, 48.041826, 47.3239857787592, -441.12, -215.21)),
        SpaceCompassConstellationStar("29Ups UMa", SpaceCompassDeepSkyReference(9.849867, 59.038735, 116.2352837283483, -294.45, -151.75)),
        SpaceCompassConstellationStar("Muscida", SpaceCompassDeepSkyReference(8.504431, 60.718169, 179.10844865757733, -134.31, -107.73)),
        SpaceCompassConstellationStar("23    UMa", SpaceCompassDeepSkyReference(9.525453, 63.061861, 77.6747936620104, 107.55, 26.86)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 3, 6 to 7, 7 to 8, 8 to 9, 8 to 10, 5 to 11, 11 to 12, 12 to 13, 12 to 14, 11 to 15, 15 to 16, 16 to 17, 17 to 4)
)

internal fun spaceCompassPatternGRU() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("The Gru", SpaceCompassDeepSkyReference(23.11465, -43.520358, 131.8868759814159, -48.35, -13.98)),
        SpaceCompassConstellationStar("Del1Gru", SpaceCompassDeepSkyReference(22.487825, -43.495565, 309.44640800129594, 25.62, -4.23)),
        SpaceCompassConstellationStar("Alnair", SpaceCompassDeepSkyReference(22.137209, -46.960975, 101.0083470790461, 127.6, -147.91)),
        SpaceCompassConstellationStar("Tiaki", SpaceCompassDeepSkyReference(22.711115, -46.884577, 176.97049360175382, 135.68, -4.51)),
        SpaceCompassConstellationStar("Iot Gru", SpaceCompassDeepSkyReference(23.17264, -45.246711, 183.2340006791046, 132.42, -26.27)),
        SpaceCompassConstellationStar("Zet Gru", SpaceCompassDeepSkyReference(23.014677, -52.754137, 108.86382343595061, -65.09, -12.91)),
        SpaceCompassConstellationStar("Eps Gru", SpaceCompassDeepSkyReference(22.809239, -51.316864, 128.9155913805689, 108.47, -65.93)),
        SpaceCompassConstellationStar("Lam Gru", SpaceCompassDeepSkyReference(22.101914, -39.543353, 242.13523324070303, -24.16, -125.17)),
        SpaceCompassConstellationStar("Aldhanab", SpaceCompassDeepSkyReference(21.898808, -37.364852, 211.10438930994732, 95.88, -12.1)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 0, 3 to 5, 3 to 6, 2 to 7, 7 to 8)
)

internal fun spaceCompassPatternHER() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("85Iot Her", SpaceCompassDeepSkyReference(17.657747, 46.006332, 454.89029997819, -7.17, 3.97)),
        SpaceCompassConstellationStar("91The Her", SpaceCompassDeepSkyReference(17.93755, 37.250539, 753.2480434504413, 2.74, 7.24)),
        SpaceCompassConstellationStar("75Rho Her", SpaceCompassDeepSkyReference(17.394708, 37.145946, 393.43363219731214, -38.58, 9.19)),
        SpaceCompassConstellationStar("69    Her", SpaceCompassDeepSkyReference(17.294517, 37.291498, 175.44734331789482, -42.74, 63.76)),
        SpaceCompassConstellationStar("67Pi  Her", SpaceCompassDeepSkyReference(17.250788, 36.809162, 376.6238586470318, -27.35, 2.7)),
        SpaceCompassConstellationStar("44Eta Her", SpaceCompassDeepSkyReference(16.714933, 38.922254, 108.64627713202471, 35.58, -84.98)),
        SpaceCompassConstellationStar("35Sig Her", SpaceCompassDeepSkyReference(16.568384, 42.437041, 314.8227697313027, -9.03, 59.8)),
        SpaceCompassConstellationStar("22Tau Her", SpaceCompassDeepSkyReference(16.329011, 46.313366, 307.40466907689387, -13.15, 39.31)),
        SpaceCompassConstellationStar("1Chi Her", SpaceCompassDeepSkyReference(15.877903, 42.451518, 51.836685420616405, 438.97, 629.52)),
        SpaceCompassConstellationStar("40Zet Her", SpaceCompassDeepSkyReference(16.688113, 31.602726, 34.9502651215766, -462.58, 345.05)),
        SpaceCompassConstellationStar("Kornephoros", SpaceCompassDeepSkyReference(16.503668, 21.489613, 139.1451600107517, -98.43, -14.49)),
        SpaceCompassConstellationStar("20Gam Her", SpaceCompassDeepSkyReference(16.365338, 19.15313, 192.6498091469259, -47.44, 44.61)),
        SpaceCompassConstellationStar("58Eps Her", SpaceCompassDeepSkyReference(17.004827, 30.926405, 155.0172339751445, -47.68, 26.89)),
        SpaceCompassConstellationStar("Sarin", SpaceCompassDeepSkyReference(17.250531, 24.839204, 75.1340354797274, -21.14, -157.68)),
        SpaceCompassConstellationStar("Maasym", SpaceCompassDeepSkyReference(17.512308, 26.110645, 369.3730762143831, 18.39, 16.78)),
        SpaceCompassConstellationStar("86Mu  Her", SpaceCompassDeepSkyReference(17.774319, 27.720676, 27.105225768758498, -291.42, -750.0)),
        SpaceCompassConstellationStar("92Xi  Her", SpaceCompassDeepSkyReference(17.962744, 29.24788, 136.8105326591751, 82.33, -18.73)),
        SpaceCompassConstellationStar("103Omi Her", SpaceCompassDeepSkyReference(18.125708, 28.762488, 337.9857433628013, -0.17, 7.51)),
        SpaceCompassConstellationStar("Rasalgethi", SpaceCompassDeepSkyReference(17.244127, 14.390333, 359.5991480438472, -6.71, 32.78)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 5 to 9, 9 to 10, 10 to 11, 9 to 12, 12 to 13, 13 to 14, 14 to 15, 15 to 16, 16 to 17, 12 to 4, 10 to 18)
)

internal fun spaceCompassPatternHOR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Hor", SpaceCompassDeepSkyReference(4.233363, -42.294368, 115.0056741844193, 41.89, -203.65)),
        SpaceCompassConstellationStar("Zet Hor", SpaceCompassDeepSkyReference(2.677666, -54.549911, 160.1160366277286, 32.76, 5.24)),
        SpaceCompassConstellationStar("Mu  Hor", SpaceCompassDeepSkyReference(3.060239, -59.737775, 141.5610003003756, -72.81, -63.89)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)
