package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternHYA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("7Eta Hya", SpaceCompassDeepSkyReference(8.72041, 3.398662, 586.6121408324897, -19.14, -1.29)),
        SpaceCompassConstellationStar("Minchir", SpaceCompassDeepSkyReference(8.645955, 3.341435, 372.75009934908894, -19.03, -16.29)),
        SpaceCompassConstellationStar("4Del Hya", SpaceCompassDeepSkyReference(8.627602, 5.703782, 160.35217384518342, -70.27, -6.99)),
        SpaceCompassConstellationStar("Ashlesha", SpaceCompassDeepSkyReference(8.779587, 6.418809, 129.2733849269058, -231.04, -40.17)),
        SpaceCompassConstellationStar("13Rho Hya", SpaceCompassDeepSkyReference(8.807214, 5.837812, 354.13276715359524, -17.51, -29.79)),
        SpaceCompassConstellationStar("16Zet Hya", SpaceCompassDeepSkyReference(8.92323, 5.945563, 167.17406064115661, -99.76, 14.65)),
        SpaceCompassConstellationStar("22The Hya", SpaceCompassDeepSkyReference(9.239405, 2.31428, 113.4851331515819, 112.57, -306.07)),
        SpaceCompassConstellationStar("32Tau2Hya", SpaceCompassDeepSkyReference(9.533036, -1.184664, 517.7086706359654, -11.42, -3.87)),
        SpaceCompassConstellationStar("31Tau1Hya", SpaceCompassDeepSkyReference(9.485805, -2.768964, 56.535946510518, 100.93, -3.15)),
        SpaceCompassConstellationStar("Alphard", SpaceCompassDeepSkyReference(9.45979, -8.658603, 180.2966363415384, -14.49, 33.25)),
        SpaceCompassConstellationStar("Zhang", SpaceCompassDeepSkyReference(9.85797, -14.846603, 263.8804050983397, 18.68, -21.88)),
        SpaceCompassConstellationStar("41Lam Hya", SpaceCompassDeepSkyReference(10.176467, -12.354083, 112.5454766274282, -200.33, -100.28)),
        SpaceCompassConstellationStar("42Mu  Hya", SpaceCompassDeepSkyReference(10.434842, -16.83629, 234.1395096413875, -128.51, -80.06)),
        SpaceCompassConstellationStar("Nu  Hya", SpaceCompassDeepSkyReference(10.827079, -16.193648, 143.7446172490771, 92.77, 199.02)),
        SpaceCompassConstellationStar("Xi  Hya", SpaceCompassDeepSkyReference(11.550038, -31.857625, 129.6328092551312, -209.09, -41.6)),
        SpaceCompassConstellationStar("Bet Hya", SpaceCompassDeepSkyReference(11.881813, -33.908124, 309.7402748976036, -58.02, 2.4)),
        SpaceCompassConstellationStar("45Psi Hya", SpaceCompassDeepSkyReference(13.150909, -23.11807, 231.48068285037712, -19.97, -39.9)),
        SpaceCompassConstellationStar("46Gam Hya", SpaceCompassDeepSkyReference(13.315359, -23.171512, 133.8353341817957, 68.41, -41.09)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 10 to 11, 11 to 12, 12 to 13, 13 to 14, 14 to 15, 15 to 16, 16 to 17)
)

internal fun spaceCompassPatternHYI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Bet Hyi", SpaceCompassDeepSkyReference(0.427916, -77.254247, 24.327351899887603, 2220.12, 324.37)),
        SpaceCompassConstellationStar("Gam Hyi", SpaceCompassDeepSkyReference(3.787295, -74.238962, 214.0133780426536, 51.07, 115.27)),
        SpaceCompassConstellationStar("Eps Hyi", SpaceCompassDeepSkyReference(2.659799, -68.266946, 151.84177548185733, 87.4, 0.56)),
        SpaceCompassConstellationStar("Del Hyi", SpaceCompassDeepSkyReference(2.362498, -68.659418, 139.6816872520682, -50.07, 2.38)),
        SpaceCompassConstellationStar("Alp Hyi", SpaceCompassDeepSkyReference(1.979451, -61.569859, 71.7932157029463, 262.54, 26.88)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4)
)

internal fun spaceCompassPatternIND() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("The Ind", SpaceCompassDeepSkyReference(21.331096, -53.449427, 98.7754805173119, 107.37, -67.12)),
        SpaceCompassConstellationStar("Alp Ind", SpaceCompassDeepSkyReference(20.626116, -47.291502, 98.32864627986291, 49.2, 66.07)),
        SpaceCompassConstellationStar("Bet Ind", SpaceCompassDeepSkyReference(20.913498, -58.454155, 611.9257896185421, 21.06, -24.75)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0)
)

internal fun spaceCompassPatternLAC() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("1    Lac", SpaceCompassDeepSkyReference(22.26616, 37.748737, 621.2502743006074, 8.74, 0.76)),
        SpaceCompassConstellationStar("6    Lac", SpaceCompassDeepSkyReference(22.508128, 43.123376, 1716.6125485427765, -2.05, -5.76)),
        SpaceCompassConstellationStar("5    Lac", SpaceCompassDeepSkyReference(22.492173, 47.706887, 1647.2544163557386, -0.6, -3.37)),
        SpaceCompassConstellationStar("4    Lac", SpaceCompassDeepSkyReference(22.408609, 49.476392, 2249.3544189396907, -5.12, -3.37)),
        SpaceCompassConstellationStar("3Bet Lac", SpaceCompassDeepSkyReference(22.392675, 52.229046, 169.9617192013585, -13.56, -186.37)),
        SpaceCompassConstellationStar("7Alp Lac", SpaceCompassDeepSkyReference(22.521515, 50.282491, 102.5970547948228, 137.22, 17.15)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 2)
)

internal fun spaceCompassPatternMON() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("5Gam Mon", SpaceCompassDeepSkyReference(6.247592, -6.274776, 497.94881264938857, -6.18, -20.06)),
        SpaceCompassConstellationStar("11Bet Mon", SpaceCompassDeepSkyReference(6.480297, -7.033062, 676.6730490940353, -7.0, -4.97)),
        SpaceCompassConstellationStar("22Del Mon", SpaceCompassDeepSkyReference(7.197739, -0.492764, 384.1652464122112, -0.22, 6.68)),
        SpaceCompassConstellationStar("8Eps Mon", SpaceCompassDeepSkyReference(6.396135, 4.592865, 122.29331228774811, -20.88, 10.75)),
        SpaceCompassConstellationStar("HIP 29151", SpaceCompassDeepSkyReference(6.149418, 2.499692, 654.9324434256864, -1.75, -14.17)),
        SpaceCompassConstellationStar("29Zet Mon", SpaceCompassDeepSkyReference(8.143236, -2.983786, 1058.9491977666082, -16.79, -3.89)),
        SpaceCompassConstellationStar("26Alp Mon", SpaceCompassDeepSkyReference(7.687454, -9.551131, 147.7827593613808, -74.8, -19.64)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 2 to 5, 5 to 6)
)

internal fun spaceCompassPatternLEP() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("18The Lep", SpaceCompassDeepSkyReference(6.10259, -14.935254, 172.7523131689597, -16.87, 13.16)),
        SpaceCompassConstellationStar("16Eta Lep", SpaceCompassDeepSkyReference(5.940082, -14.1677, 48.5278289688499, -42.23, 139.02)),
        SpaceCompassConstellationStar("14Zet Lep", SpaceCompassDeepSkyReference(5.782595, -14.82195, 70.47456546790521, -14.84, -1.18)),
        SpaceCompassConstellationStar("Arneb", SpaceCompassDeepSkyReference(5.545504, -17.822289, 2218.750839863722, 3.27, 1.54)),
        SpaceCompassConstellationStar("5Mu  Lep", SpaceCompassDeepSkyReference(5.215528, -16.205468, 185.9499048362125, 45.66, -16.13)),
        SpaceCompassConstellationStar("15Del Lep", SpaceCompassDeepSkyReference(5.855357, -20.879089, 113.7225749945475, 228.96, -647.96)),
        SpaceCompassConstellationStar("13Gam Lep", SpaceCompassDeepSkyReference(5.741057, -22.448382, 29.115979837279, -292.42, -368.45)),
        SpaceCompassConstellationStar("Nihal", SpaceCompassDeepSkyReference(5.470756, -20.759441, 160.35217384518342, -5.03, -85.92)),
        SpaceCompassConstellationStar("2Eps Lep", SpaceCompassDeepSkyReference(5.091018, -22.371032, 213.31344645610938, 19.27, -72.35)),
        SpaceCompassConstellationStar("6Lam Lep", SpaceCompassDeepSkyReference(5.326257, -13.176788, 851.5832128578583, -2.49, -4.74)),
        SpaceCompassConstellationStar("4Kap Lep", SpaceCompassDeepSkyReference(5.220522, -12.941291, 728.0276753884111, -12.22, -1.32)),
        SpaceCompassConstellationStar("3Iot Lep", SpaceCompassDeepSkyReference(5.204973, -11.869219, 231.8097746354764, 24.95, -31.22)),
        SpaceCompassConstellationStar("7Nu  Lep", SpaceCompassDeepSkyReference(5.333062, -12.315585, 333.4932654163615, -12.12, 7.97)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 3 to 5, 5 to 6, 6 to 7, 7 to 8, 3 to 7, 4 to 9, 4 to 10, 8 to 4, 10 to 11, 9 to 12)
)

internal fun spaceCompassPatternLEO() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Denebola", SpaceCompassDeepSkyReference(11.817663, 14.57206, 35.8768753906223, -499.02, -113.78)),
        SpaceCompassConstellationStar("Chertan", SpaceCompassDeepSkyReference(11.237335, 15.42957, 165.0589365317721, -59.01, -79.37)),
        SpaceCompassConstellationStar("Regulus", SpaceCompassDeepSkyReference(10.139532, 11.967207, 79.2990524229564, -249.4, 4.91)),
        SpaceCompassConstellationStar("30Eta Leo", SpaceCompassDeepSkyReference(10.122209, 16.762664, 1269.0910996059627, -1.94, -0.53)),
        SpaceCompassConstellationStar("Algieba", SpaceCompassDeepSkyReference(10.332873, 19.841489, 130.0982344061091, 310.77, -152.88)),
        SpaceCompassConstellationStar("Zosma", SpaceCompassDeepSkyReference(11.235138, 20.523717, 58.4299365958219, 143.31, -130.43)),
        SpaceCompassConstellationStar("Adhafera", SpaceCompassDeepSkyReference(10.278171, 23.417311, 274.0809458109072, 19.84, -7.3)),
        SpaceCompassConstellationStar("Rasalas", SpaceCompassDeepSkyReference(9.879398, 26.006951, 124.1083725296486, -216.29, -54.92)),
        SpaceCompassConstellationStar("Ras Elased Australis", SpaceCompassDeepSkyReference(9.764188, 23.774255, 246.714468783611, -46.09, -9.57)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 0, 4 to 6, 6 to 7, 7 to 8, 5 to 1)
)
