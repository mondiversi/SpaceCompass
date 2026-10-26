package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternPSA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Fomalhaut", SpaceCompassDeepSkyReference(22.960838, -29.622236, 25.1257827124972, 329.22, -164.22)),
        SpaceCompassConstellationStar("18Eps PsA", SpaceCompassDeepSkyReference(22.677594, -27.043617, 486.80046150322494, 22.01, -0.88)),
        SpaceCompassConstellationStar("12Eta PsA", SpaceCompassDeepSkyReference(22.013951, -28.453736, 817.4346401126683, 14.81, 0.3)),
        SpaceCompassConstellationStar("10The PsA", SpaceCompassDeepSkyReference(21.795598, -30.898304, 321.02006706398043, -34.53, 0.64)),
        SpaceCompassConstellationStar("15Tau PsA", SpaceCompassDeepSkyReference(22.169093, -32.548407, 59.61551502876139, 428.62, 13.83)),
        SpaceCompassConstellationStar("17Bet PsA", SpaceCompassDeepSkyReference(22.52509, -32.346073, 142.8003945356356, 59.64, -18.7)),
        SpaceCompassConstellationStar("23Del PsA", SpaceCompassDeepSkyReference(22.932472, -32.539628, 154.138242537243, 10.84, 30.32)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6)
)

internal fun spaceCompassPatternVOL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Zet Vol", SpaceCompassDeepSkyReference(7.697004, -72.606098, 141.0101221784403, 33.38, 15.29)),
        SpaceCompassConstellationStar("Gam2Vol", SpaceCompassDeepSkyReference(7.145788, -70.498932, 139.80138664268412, 23.71, 108.06)),
        SpaceCompassConstellationStar("Eps Vol", SpaceCompassDeepSkyReference(8.132173, -68.617062, 562.3386047349227, -29.88, 30.58)),
        SpaceCompassConstellationStar("Del Vol", SpaceCompassDeepSkyReference(7.280508, -67.957152, 737.9102136327211, -4.11, 8.5)),
        SpaceCompassConstellationStar("Bet Vol", SpaceCompassDeepSkyReference(8.428951, -66.13689, 107.5360408223339, -35.83, -152.15)),
        SpaceCompassConstellationStar("Alp Vol", SpaceCompassDeepSkyReference(9.040777, -66.396076, 124.91626187721151, -2.01, -95.8)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 0, 2 to 3, 2 to 4, 4 to 5, 5 to 2)
)

internal fun spaceCompassPatternPUP() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Tureis", SpaceCompassDeepSkyReference(8.125737, -24.304324, 63.5411331907586, -83.29, 46.38)),
        SpaceCompassConstellationStar("HIP 38146", SpaceCompassDeepSkyReference(7.817132, -24.912208, 281.41196271247014, -34.68, 14.71)),
        SpaceCompassConstellationStar("Pi  Pup", SpaceCompassDeepSkyReference(7.285711, -37.09747, 807.3179215891696, -10.57, 7.0)),
        SpaceCompassConstellationStar("Nu  Pup", SpaceCompassDeepSkyReference(6.629353, -43.195934, 371.47645869417045, -0.43, -3.99)),
        SpaceCompassConstellationStar("Tau Pup", SpaceCompassDeepSkyReference(6.832266, -50.61456, 182.0070003861972, 34.23, -65.85)),
        SpaceCompassConstellationStar("Sig Pup", SpaceCompassDeepSkyReference(7.487179, -43.301432, 193.6794848313248, -59.86, 188.73)),
        SpaceCompassConstellationStar("Naos", SpaceCompassDeepSkyReference(8.059737, -40.003148, 1083.5759612212244, -30.82, 16.77)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 0)
)

internal fun spaceCompassPatternRET() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Ret", SpaceCompassDeepSkyReference(4.240404, -62.473858, 161.623531405458, 41.64, 49.72)),
        SpaceCompassConstellationStar("Eps Ret", SpaceCompassDeepSkyReference(4.274738, -59.302156, 59.4850524776814, -47.99, -167.81)),
        SpaceCompassConstellationStar("Del Ret", SpaceCompassDeepSkyReference(3.979095, -61.400185, 526.0586000614632, 9.79, -14.39)),
        SpaceCompassConstellationStar("Bet Ret", SpaceCompassDeepSkyReference(3.736593, -64.806903, 97.3893159120869, 308.39, 78.72)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
)

internal fun spaceCompassPatternSGR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Kaus Media", SpaceCompassDeepSkyReference(18.3499, -29.828103, 347.71466195321466, 29.96, -26.38)),
        SpaceCompassConstellationStar("Kaus Borealis", SpaceCompassDeepSkyReference(18.466179, -25.4217, 78.1774006400461, -44.81, -186.29)),
        SpaceCompassConstellationStar("Eta Sgr", SpaceCompassDeepSkyReference(18.293793, -36.761686, 145.9311696051779, -129.27, -166.61)),
        SpaceCompassConstellationStar("Kaus Australis", SpaceCompassDeepSkyReference(18.402868, -34.384616, 143.3023492009159, -39.61, -124.05)),
        SpaceCompassConstellationStar("Alnasl", SpaceCompassDeepSkyReference(18.096803, -30.424091, 96.8684441769, -55.75, -181.53)),
        SpaceCompassConstellationStar("3    Sgr", SpaceCompassDeepSkyReference(17.792674, -27.830788, 985.3666881755996, -3.62, -10.67)),
        SpaceCompassConstellationStar("Ascella", SpaceCompassDeepSkyReference(19.043532, -29.880105, 88.1979030321232, -14.1, 3.66)),
        SpaceCompassConstellationStar("27Phi Sgr", SpaceCompassDeepSkyReference(18.76094, -26.990778, 239.2931065654252, 51.15, 0.45)),
        SpaceCompassConstellationStar("Polis", SpaceCompassDeepSkyReference(18.229392, -21.058834, Double.NaN, 1.72, -1.39, hasMeasuredDistance = false)),
        SpaceCompassConstellationStar("40Tau Sgr", SpaceCompassDeepSkyReference(19.11567, -27.670423, 121.60936236371121, -50.79, -250.51)),
        SpaceCompassConstellationStar("Nunki", SpaceCompassDeepSkyReference(18.92109, -26.296722, 227.76282630097484, 13.87, -52.65)),
        SpaceCompassConstellationStar("37Xi 2Sgr", SpaceCompassDeepSkyReference(18.962167, -21.106654, 365.2367610323917, 34.91, -12.33)),
        SpaceCompassConstellationStar("39Omi Sgr", SpaceCompassDeepSkyReference(19.07805, -21.741496, 142.054148743458, 76.26, -58.08)),
        SpaceCompassConstellationStar("43    Sgr", SpaceCompassDeepSkyReference(19.293911, -18.952908, 481.7672162825585, -10.05, -10.64)),
        SpaceCompassConstellationStar("44Rho1Sgr", SpaceCompassDeepSkyReference(19.361211, -17.847197, 126.9586531143689, -26.95, 22.24)),
        SpaceCompassConstellationStar("51    Sgr", SpaceCompassDeepSkyReference(19.600459, -24.719081, 294.36493709644793, 6.08, -26.37)),
        SpaceCompassConstellationStar("62    Sgr", SpaceCompassDeepSkyReference(20.044299, -27.709845, 448.63331602839327, 32.28, 14.35)),
        SpaceCompassConstellationStar("The1Sgr", SpaceCompassDeepSkyReference(19.995605, -35.276305, 518.5315631769025, 6.02, -25.15)),
        SpaceCompassConstellationStar("Iot Sgr", SpaceCompassDeepSkyReference(19.921026, -41.868288, 181.8041311192678, 23.37, 51.6)),
        SpaceCompassConstellationStar("Rukbat", SpaceCompassDeepSkyReference(19.398103, -40.61594, 181.8041311192678, 32.67, -120.81)),
        SpaceCompassConstellationStar("Arkab Posterior", SpaceCompassDeepSkyReference(19.386976, -44.799778, 134.1654044360281, 92.78, -53.73)),
    ),
    edges = listOf(0 to 1, 2 to 3, 3 to 4, 4 to 5, 4 to 0, 0 to 3, 3 to 6, 6 to 7, 7 to 0, 7 to 1, 1 to 8, 6 to 9, 9 to 10, 10 to 7, 10 to 11, 11 to 12, 12 to 13, 13 to 14, 9 to 15, 15 to 16, 16 to 17, 17 to 18, 18 to 19, 18 to 20)
)

internal fun spaceCompassPatternSCO() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Shaula", SpaceCompassDeepSkyReference(17.560145, -37.103821, 571.2019042989201, -8.9, -29.95)),
        SpaceCompassConstellationStar("Kap Sco", SpaceCompassDeepSkyReference(17.708132, -39.029983, 483.1944765913737, -6.49, -25.55)),
        SpaceCompassConstellationStar("Iot1Sco", SpaceCompassDeepSkyReference(17.793078, -40.126997, 1929.919471871332, 0.44, -6.4)),
        SpaceCompassConstellationStar("Sargas", SpaceCompassDeepSkyReference(17.62198, -42.997824, 300.32805414993703, 6.06, -0.95)),
        SpaceCompassConstellationStar("Eta Sco", SpaceCompassDeepSkyReference(17.202552, -43.239189, 73.4752041427452, 22.01, -287.41)),
        SpaceCompassConstellationStar("Zet1Sco", SpaceCompassDeepSkyReference(16.899924, -42.362025, 2568.1605365118435, -0.48, -2.17)),
        SpaceCompassConstellationStar("Xamidimura", SpaceCompassDeepSkyReference(16.864509, -38.04738, 501.00815947221463, -8.84, -21.6)),
        SpaceCompassConstellationStar("Larawag", SpaceCompassDeepSkyReference(16.83608, -34.293232, 63.7149745400727, -611.83, -255.87)),
        SpaceCompassConstellationStar("Paikauhale", SpaceCompassDeepSkyReference(16.598043, -28.216016, 474.0643811104176, -8.59, -22.5)),
        SpaceCompassConstellationStar("Antares", SpaceCompassDeepSkyReference(16.490128, -26.432002, 553.7460149644162, -10.16, -23.21)),
        SpaceCompassConstellationStar("Dschubba", SpaceCompassDeepSkyReference(16.005557, -22.62171, 491.19933256926475, -8.67, -36.9)),
        SpaceCompassConstellationStar("Fang", SpaceCompassDeepSkyReference(15.980865, -26.114105, 585.5589818888964, -12.0, -25.71)),
        SpaceCompassConstellationStar("Acrab", SpaceCompassDeepSkyReference(16.09062, -19.805453, 404.1589585215989, -6.75, -24.89)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 9 to 11, 9 to 12)
)

internal fun spaceCompassPatternSER() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("32Mu  Ser", SpaceCompassDeepSkyReference(15.827002, -3.430208, 169.6081656879317, -98.07, -27.41)),
        SpaceCompassConstellationStar("37Eps Ser", SpaceCompassDeepSkyReference(15.846935, 4.47773, 70.4442329247791, 127.98, 61.87)),
        SpaceCompassConstellationStar("Unukalhai", SpaceCompassDeepSkyReference(15.737798, 6.425627, 73.9582417381189, 134.66, 44.14)),
        SpaceCompassConstellationStar("13Del Ser", SpaceCompassDeepSkyReference(15.580041, 10.538867, 228.0814810819877, -72.63, 3.17)),
        SpaceCompassConstellationStar("28Bet Ser", SpaceCompassDeepSkyReference(15.769793, 15.421826, 155.0909453165047, 68.54, -41.31)),
        SpaceCompassConstellationStar("41Gam Ser", SpaceCompassDeepSkyReference(15.940882, 15.661617, 36.7046602772249, 311.2, -1282.17)),
        SpaceCompassConstellationStar("Gudja", SpaceCompassDeepSkyReference(15.812327, 18.141564, 381.916072031592, -51.78, -88.72)),
        SpaceCompassConstellationStar("Alya", SpaceCompassDeepSkyReference(18.936995, 4.203595, 154.6496557374766, 37.69, 26.98)),
        SpaceCompassConstellationStar("58Eta Ser", SpaceCompassDeepSkyReference(18.355167, -2.898825, 60.477872491400205, -547.57, -700.72)),
        SpaceCompassConstellationStar("56Omi Ser", SpaceCompassDeepSkyReference(17.690243, -12.875307, 173.21088903600588, -72.16, -55.22)),
        SpaceCompassConstellationStar("55Xi  Ser", SpaceCompassDeepSkyReference(17.626445, -15.398557, 105.2796910014053, -39.3, -61.27)),
        SpaceCompassConstellationStar("53Nu  Ser", SpaceCompassDeepSkyReference(17.347128, -12.846875, 203.21270959511813, 42.75, 2.72)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 4, 7 to 8, 8 to 9, 9 to 10, 10 to 11)
)
