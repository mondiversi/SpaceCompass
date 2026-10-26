package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternCAR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Miaplacidus", SpaceCompassDeepSkyReference(9.220041, -69.717208, 113.1700660907237, -157.66, 108.91)),
        SpaceCompassConstellationStar("Ome Car", SpaceCompassDeepSkyReference(10.228961, -70.037903, 341.8829859199386, -35.73, 7.55)),
        SpaceCompassConstellationStar("The Car", SpaceCompassDeepSkyReference(10.715949, -64.39445, 455.5256526019497, -18.87, 12.06)),
        SpaceCompassConstellationStar("HIP 52468", SpaceCompassDeepSkyReference(10.725639, -60.566619, 1140.4067530971831, -15.22, 3.12)),
        SpaceCompassConstellationStar("HIP 54463", SpaceCompassDeepSkyReference(11.143165, -58.975037, Double.NaN, -5.03, 2.09, hasMeasuredDistance = false)),
        SpaceCompassConstellationStar("HIP 53253", SpaceCompassDeepSkyReference(10.891559, -58.853171, 95.006091260233, 79.75, 38.65)),
        SpaceCompassConstellationStar("HIP 51232", SpaceCompassDeepSkyReference(10.464649, -58.739403, 1342.2072055768604, -13.25, 2.21)),
        SpaceCompassConstellationStar("HIP 50371", SpaceCompassDeepSkyReference(10.28472, -61.332304, 657.5733316159233, -24.25, 6.38)),
        SpaceCompassConstellationStar("Aspidiske", SpaceCompassDeepSkyReference(9.284838, -59.275229, 765.6253518277787, -19.03, 13.11)),
        SpaceCompassConstellationStar("HIP 42568", SpaceCompassDeepSkyReference(8.676953, -59.761003, 1443.1698907630282, -6.87, 4.92)),
        SpaceCompassConstellationStar("Avior", SpaceCompassDeepSkyReference(8.375236, -59.509483, 605.1140136702776, -25.34, 22.72)),
        SpaceCompassConstellationStar("Canopus", SpaceCompassDeepSkyReference(6.399195, -52.69566, 309.1528672613659, 19.99, 23.67)),
        SpaceCompassConstellationStar("HIP 45080", SpaceCompassDeepSkyReference(9.182804, -58.966896, 446.78955402525514, -17.67, 14.14)),
        SpaceCompassConstellationStar("Nu  Pup", SpaceCompassDeepSkyReference(6.629353, -43.195934, 371.47645869417045, -0.43, -3.99)),
        SpaceCompassConstellationStar("Naos", SpaceCompassDeepSkyReference(8.059737, -40.003148, 1083.5759612212244, -30.82, 16.77)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 9 to 10, 10 to 11, 12 to 8, 12 to 9, 11 to 13, 10 to 14)
)

internal fun spaceCompassPatternCAS() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Segin", SpaceCompassDeepSkyReference(1.906584, 63.670101, 411.81352254984023, 31.98, -18.66)),
        SpaceCompassConstellationStar("Ruchbah", SpaceCompassDeepSkyReference(1.430216, 60.235283, 99.4075715772945, 297.24, -49.49)),
        SpaceCompassConstellationStar("Cih", SpaceCompassDeepSkyReference(0.945143, 60.71674, 549.0849141707055, 25.65, -3.82)),
        SpaceCompassConstellationStar("Schedar", SpaceCompassDeepSkyReference(0.675116, 56.537331, 228.240971550683, 50.36, -32.17)),
        SpaceCompassConstellationStar("Caph", SpaceCompassDeepSkyReference(0.152887, 59.14978, 54.7427387459234, 523.39, -180.42)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4)
)

internal fun spaceCompassPatternCEN() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Rigil Kentaurus", SpaceCompassDeepSkyReference(14.660765, -60.833976, 4.3209196917696, -3678.19, 481.84)),
        SpaceCompassConstellationStar("Hadar", SpaceCompassDeepSkyReference(14.063729, -60.373039, 392.0148519543171, -33.96, -25.06)),
        SpaceCompassConstellationStar("Eps Cen", SpaceCompassDeepSkyReference(13.664796, -53.466394, 427.4657671156632, -14.6, -12.79)),
        SpaceCompassConstellationStar("Zet Cen", SpaceCompassDeepSkyReference(13.925667, -47.288375, 381.916072031592, -57.14, -44.75)),
        SpaceCompassConstellationStar("Ups1Cen", SpaceCompassDeepSkyReference(13.977987, -44.803584, 427.4657671156632, -27.97, -21.69)),
        SpaceCompassConstellationStar("Mu  Cen", SpaceCompassDeepSkyReference(13.826943, -42.473732, 505.66893410954765, -23.85, -19.22)),
        SpaceCompassConstellationStar("Nu  Cen", SpaceCompassDeepSkyReference(13.825078, -41.687709, 436.6216289504576, -26.01, -20.38)),
        SpaceCompassConstellationStar("HIP 65936", SpaceCompassDeepSkyReference(13.517405, -39.407307, 905.9900105347507, -15.65, -11.25)),
        SpaceCompassConstellationStar("Iot Cen", SpaceCompassDeepSkyReference(13.343296, -36.712295, 58.777619294450105, -340.76, -87.98)),
        SpaceCompassConstellationStar("Menkent", SpaceCompassDeepSkyReference(14.111395, -36.369954, 58.820019623551104, -519.29, -517.87)),
        SpaceCompassConstellationStar("Eta Cen", SpaceCompassDeepSkyReference(14.591786, -42.157824, 305.6760402750839, -35.31, -32.44)),
        SpaceCompassConstellationStar("Kap Cen", SpaceCompassDeepSkyReference(14.986025, -42.104194, 383.26244555873757, -17.76, -21.33)),
        SpaceCompassConstellationStar("Gam Cen", SpaceCompassDeepSkyReference(12.691971, -48.959888, 130.1500932701634, -187.28, -1.2)),
        SpaceCompassConstellationStar("Sig Cen", SpaceCompassDeepSkyReference(12.467331, -50.230635, 411.81352254984023, -32.49, -12.41)),
        SpaceCompassConstellationStar("Del Cen", SpaceCompassDeepSkyReference(12.13931, -50.722425, 414.9573438744905, -47.53, -6.42)),
        SpaceCompassConstellationStar("HIP 56480", SpaceCompassDeepSkyReference(11.579355, -54.264091, 426.348355365663, -55.55, 16.64)),
        SpaceCompassConstellationStar("Lam Cen", SpaceCompassDeepSkyReference(11.596363, -63.019841, 419.7635842562777, -33.84, -6.87)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 6 to 9, 5 to 10, 10 to 11, 3 to 12, 12 to 13, 13 to 14, 14 to 15, 15 to 16)
)

internal fun spaceCompassPatternCEP() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("21Zet Cep", SpaceCompassDeepSkyReference(22.180908, 58.201261, 836.2985465297031, 13.35, 4.49)),
        SpaceCompassConstellationStar("32Iot Cep", SpaceCompassDeepSkyReference(22.82802, 66.200408, 115.2904087021514, -66.1, -124.74)),
        SpaceCompassConstellationStar("Alfirk", SpaceCompassDeepSkyReference(21.477662, 70.560716, 685.2023645272681, 12.6, 8.73)),
        SpaceCompassConstellationStar("Alderamin", SpaceCompassDeepSkyReference(21.30963, 62.585573, 49.0460914530152, 149.91, 48.27)),
        SpaceCompassConstellationStar("Errai", SpaceCompassDeepSkyReference(23.65582, 77.632276, 45.9958770087648, -48.85, 127.18)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 1 to 4, 4 to 2)
)

internal fun spaceCompassPatternCOM() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Diadem", SpaceCompassDeepSkyReference(13.166469, 17.529431, 58.138352794158095, -445.95, 129.69)),
        SpaceCompassConstellationStar("43Bet Com", SpaceCompassDeepSkyReference(13.197904, 27.878183, 29.7751418766107, -801.94, 882.7)),
        SpaceCompassConstellationStar("15Gam Com", SpaceCompassDeepSkyReference(12.448966, 28.268423, 167.2598397684917, -83.41, -80.99)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)

internal fun spaceCompassPatternCVN() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Chara", SpaceCompassDeepSkyReference(12.562411, 41.35748, 27.5259674959915, -705.06, 292.93)),
        SpaceCompassConstellationStar("Cor Caroli", SpaceCompassDeepSkyReference(12.933807, 38.31838, 114.80345723024531, -233.43, 54.98)),
    ),
    edges = listOf(0 to 1)
)

internal fun spaceCompassPatternAUR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Mahasim", SpaceCompassDeepSkyReference(5.995351, 37.212585, 165.56154350980782, 42.09, -73.61)),
        SpaceCompassConstellationStar("Menkalinan", SpaceCompassDeepSkyReference(5.992149, 44.947433, 81.1131341957238, -56.41, -0.88)),
        SpaceCompassConstellationStar("Capella", SpaceCompassDeepSkyReference(5.27815, 45.997991, 42.802806071081804, 75.52, -427.13)),
        SpaceCompassConstellationStar("Saclateni", SpaceCompassDeepSkyReference(5.041301, 41.075837, 785.9191278046503, 8.88, -21.43)),
        SpaceCompassConstellationStar("Hassaleh", SpaceCompassDeepSkyReference(4.949894, 33.16609, 493.4286114108443, 3.63, -18.54)),
        SpaceCompassConstellationStar("Elnath", SpaceCompassDeepSkyReference(5.438198, 28.60745, 133.8901284532493, 23.28, -174.22)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 5 to 4, 5 to 0)
)
