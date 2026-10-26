package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternCOL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Del Col", SpaceCompassDeepSkyReference(6.368564, -33.4364, 233.97153910687203, -24.19, -52.48)),
        SpaceCompassConstellationStar("Kap Col", SpaceCompassDeepSkyReference(6.275871, -35.140519, 182.5161304917869, 0.39, 87.58)),
        SpaceCompassConstellationStar("Gam Col", SpaceCompassDeepSkyReference(5.958947, -35.28328, 869.7504492521259, -2.65, 11.19)),
        SpaceCompassConstellationStar("Wazn", SpaceCompassDeepSkyReference(5.849329, -35.768309, 87.1842090102316, 55.74, 404.68)),
        SpaceCompassConstellationStar("Eta Col", SpaceCompassDeepSkyReference(5.985778, -42.815135, 472.00633436713065, 18.53, -11.45)),
        SpaceCompassConstellationStar("Phact", SpaceCompassDeepSkyReference(5.660817, -34.074108, 261.34323463621143, -0.1, -24.05)),
        SpaceCompassConstellationStar("Eps Col", SpaceCompassDeepSkyReference(5.520209, -35.470519, 263.2417909108031, 28.36, -34.11)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 3 to 5, 5 to 6)
)

internal fun spaceCompassPatternCIR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Cir", SpaceCompassDeepSkyReference(14.708492, -64.975138, 54.04411178489, -192.64, -234.07)),
        SpaceCompassConstellationStar("Gam Cir", SpaceCompassDeepSkyReference(15.389625, -59.320787, 448.63331602839327, -10.29, -36.57)),
        SpaceCompassConstellationStar("Bet Cir", SpaceCompassDeepSkyReference(15.291917, -58.801208, 99.650558078681, -98.39, -135.46)),
    ),
    edges = listOf(0 to 1, 0 to 2)
)

internal fun spaceCompassPatternCRT() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alkes", SpaceCompassDeepSkyReference(10.996244, -18.298783, 159.1783370418411, -462.39, 129.11)),
        SpaceCompassConstellationStar("11Bet Crt", SpaceCompassDeepSkyReference(11.194302, -22.825847, 340.10054131580813, 4.73, -99.06)),
        SpaceCompassConstellationStar("15Gam Crt", SpaceCompassDeepSkyReference(11.414702, -17.68401, 82.3212174187246, -97.01, 3.22)),
        SpaceCompassConstellationStar("12Del Crt", SpaceCompassDeepSkyReference(11.322347, -14.778541, 185.73822934708522, -124.47, 206.61)),
        SpaceCompassConstellationStar("14Eps Crt", SpaceCompassDeepSkyReference(11.410164, -10.859323, 376.1897445083131, -24.14, 24.46)),
        SpaceCompassConstellationStar("21The Crt", SpaceCompassDeepSkyReference(11.611365, -9.802247, 280.4439305834565, -59.32, 2.82)),
        SpaceCompassConstellationStar("30Eta Crt", SpaceCompassDeepSkyReference(11.933599, -17.150829, 251.469828770477, -49.07, -8.23)),
        SpaceCompassConstellationStar("27Zet Crt", SpaceCompassDeepSkyReference(11.746049, -18.350674, 352.9830659222027, 27.15, -24.56)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 2)
)

internal fun spaceCompassPatternCRA() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Lam CrA", SpaceCompassDeepSkyReference(18.729706, -38.323441, 205.2590147088079, 1.12, -53.81)),
        SpaceCompassConstellationStar("HIP 92989", SpaceCompassDeepSkyReference(18.944582, -37.343241, 430.28538900087966, 8.79, -23.68)),
        SpaceCompassConstellationStar("Eps CrA", SpaceCompassDeepSkyReference(18.978721, -37.107357, 98.4473672013457, -132.25, -110.45)),
        SpaceCompassConstellationStar("Gam CrA", SpaceCompassDeepSkyReference(19.106971, -37.063437, 56.438099597207994, 96.93, -279.67)),
        SpaceCompassConstellationStar("Meridiana", SpaceCompassDeepSkyReference(19.157869, -37.904474, 125.348419077664, 85.73, -96.65)),
        SpaceCompassConstellationStar("Bet CrA", SpaceCompassDeepSkyReference(19.167154, -39.340796, 474.0643811104176, 5.89, -36.78)),
        SpaceCompassConstellationStar("Del CrA", SpaceCompassDeepSkyReference(19.139156, -40.496703, 178.5200625522065, 48.05, -26.84)),
        SpaceCompassConstellationStar("Zet CrA", SpaceCompassDeepSkyReference(19.051907, -42.095105, 193.1061019193282, 55.74, -45.33)),
        SpaceCompassConstellationStar("HIP 92953", SpaceCompassDeepSkyReference(18.938044, -42.710676, 295.43146845152694, -27.03, -30.48)),
        SpaceCompassConstellationStar("HIP 90887", SpaceCompassDeepSkyReference(18.539257, -39.704001, 229.20345902127573, 31.98, -37.62)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 0 to 9)
)

internal fun spaceCompassPatternCRB() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("4The CrB", SpaceCompassDeepSkyReference(15.548829, 31.359133, 375.3237993255196, -19.62, -8.94)),
        SpaceCompassConstellationStar("Nusakan", SpaceCompassDeepSkyReference(15.463818, 29.105703, 111.81227709035859, -181.39, 86.84)),
        SpaceCompassConstellationStar("Alphecca", SpaceCompassDeepSkyReference(15.578128, 26.714693, 75.0476040396369, 120.38, -89.44)),
        SpaceCompassConstellationStar("8Gam CrB", SpaceCompassDeepSkyReference(15.712381, 26.295637, 146.0619583126356, -112.52, 50.7)),
        SpaceCompassConstellationStar("10Del CrB", SpaceCompassDeepSkyReference(15.82657, 26.068394, 170.0501075797152, -79.85, -63.78)),
        SpaceCompassConstellationStar("13Eps CrB", SpaceCompassDeepSkyReference(15.959794, 26.87788, 221.42332478761992, -76.55, -60.24)),
        SpaceCompassConstellationStar("14Iot CrB", SpaceCompassDeepSkyReference(16.024047, 29.851061, 311.8129986778871, -38.2, -6.9)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6)
)

internal fun spaceCompassPatternCRV() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("8Eta Crv", SpaceCompassDeepSkyReference(12.53451, -16.196007, 59.62627818922551, -424.37, -58.41)),
        SpaceCompassConstellationStar("Algorab", SpaceCompassDeepSkyReference(12.497739, -16.515432, 86.85935725804241, -209.97, -139.3)),
        SpaceCompassConstellationStar("Gienah", SpaceCompassDeepSkyReference(12.263437, -17.541929, 153.6300909007864, -159.58, 22.31)),
        SpaceCompassConstellationStar("2Eps Crv", SpaceCompassDeepSkyReference(12.168746, -22.619766, 317.89124893270434, -71.52, 10.55)),
        SpaceCompassConstellationStar("Alchiba", SpaceCompassDeepSkyReference(12.140225, -24.728875, 48.7163473551605, 100.18, -39.33)),
        SpaceCompassConstellationStar("Kraz", SpaceCompassDeepSkyReference(12.573121, -23.396759, 145.6705706593956, 0.86, -56.0)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 3 to 5, 5 to 1)
)

internal fun spaceCompassPatternCYG() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("1Kap Cyg", SpaceCompassDeepSkyReference(19.28504, 53.368459, 124.1553390480374, 60.22, 122.93)),
        SpaceCompassConstellationStar("10Iot2Cyg", SpaceCompassDeepSkyReference(19.495098, 51.729779, 121.3380002574648, 20.92, 128.12)),
        SpaceCompassConstellationStar("Fawaris", SpaceCompassDeepSkyReference(19.749574, 45.13081, 164.9754404990809, 43.22, 48.44)),
        SpaceCompassConstellationStar("Sadr", SpaceCompassDeepSkyReference(20.370473, 40.256679, 1832.339354478291, 2.43, -0.93)),
        SpaceCompassConstellationStar("Deneb", SpaceCompassDeepSkyReference(20.690532, 45.280338, 1411.9322636888107, 1.56, 1.55)),
        SpaceCompassConstellationStar("Aljanah", SpaceCompassDeepSkyReference(20.770178, 33.970256, 72.7054750913732, 356.16, 330.28)),
        SpaceCompassConstellationStar("64Zet Cyg", SpaceCompassDeepSkyReference(21.215607, 30.226916, 143.1138308146053, 6.87, -68.12)),
        SpaceCompassConstellationStar("78Mu 1Cyg", SpaceCompassDeepSkyReference(21.73571, 28.742632, 72.527393709149, 260.33, -242.73)),
        SpaceCompassConstellationStar("21Eta Cyg", SpaceCompassDeepSkyReference(19.938438, 35.083424, 134.94263508408721, -34.0, -27.6)),
        SpaceCompassConstellationStar("Albireo", SpaceCompassDeepSkyReference(19.512023, 27.959681, 434.29613397745663, -7.09, -5.63)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 3 to 5, 5 to 6, 6 to 7, 3 to 8, 8 to 9)
)
