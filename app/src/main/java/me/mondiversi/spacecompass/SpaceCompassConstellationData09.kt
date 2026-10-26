package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternAPS() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Aps", SpaceCompassDeepSkyReference(14.797701, -79.044751, 446.78955402525514, -5.67, -15.75)),
        SpaceCompassConstellationStar("Gam Aps", SpaceCompassDeepSkyReference(16.557609, -78.897148, 156.28011146959892, -125.68, -77.59)),
        SpaceCompassConstellationStar("Bet Aps", SpaceCompassDeepSkyReference(16.718126, -77.517435, 156.9568859533264, -282.83, -355.03)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)

internal fun spaceCompassPatternOPH() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Rasalhague", SpaceCompassDeepSkyReference(17.582241, 12.560035, 48.5858848040805, 110.08, -222.61)),
        SpaceCompassConstellationStar("Cebalrai", SpaceCompassDeepSkyReference(17.724543, 4.567303, 81.8460075764157, -40.67, 158.8)),
        SpaceCompassConstellationStar("Sabik", SpaceCompassDeepSkyReference(17.172968, -15.72491, 88.3652212538833, 41.16, 97.65)),
        SpaceCompassConstellationStar("27Kap Oph", SpaceCompassDeepSkyReference(16.961139, 9.375033, 91.4627283729002, -293.03, -9.7)),
        SpaceCompassConstellationStar("Yed Posterior", SpaceCompassDeepSkyReference(16.305358, -4.692511, 106.44798314632669, 82.39, 40.08)),
        SpaceCompassConstellationStar("13Zet Oph", SpaceCompassDeepSkyReference(16.619316, -10.56709, 366.0563920095518, 13.07, 25.44)),
        SpaceCompassConstellationStar("51    Oph", SpaceCompassDeepSkyReference(17.523598, -23.962643, 405.66710561208373, 4.91, -25.9)),
    ),
    edges = listOf(0 to 1, 2 to 1, 0 to 3, 3 to 4, 4 to 5, 5 to 2, 2 to 6)
)

internal fun spaceCompassPatternORI() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alnitak", SpaceCompassDeepSkyReference(5.679313, -1.942572, 736.2445330118072, 3.99, 2.54)),
        SpaceCompassConstellationStar("Alnilam", SpaceCompassDeepSkyReference(5.603559, -1.20192, 1976.7052996268862, 1.49, -1.06)),
        SpaceCompassConstellationStar("Mintaka", SpaceCompassDeepSkyReference(5.533445, -0.299092, 692.4763040627334, 1.67, 0.56)),
        SpaceCompassConstellationStar("64    Ori", SpaceCompassDeepSkyReference(6.057602, 19.690561, 718.4060622462611, 9.81, -20.29)),
        SpaceCompassConstellationStar("70Xi  Ori", SpaceCompassDeepSkyReference(6.198999, 14.208765, 607.3674280838069, -1.59, -20.23)),
        SpaceCompassConstellationStar("67Nu  Ori", SpaceCompassDeepSkyReference(6.126201, 14.768472, 516.0700609944006, 4.95, -21.18)),
        SpaceCompassConstellationStar("54Chi1Ori", SpaceCompassDeepSkyReference(5.906386, 20.276174, 28.255905469284098, -163.17, -98.92)),
        SpaceCompassConstellationStar("61Mu  Ori", SpaceCompassDeepSkyReference(6.039722, 9.647276, 154.94352263378428, 14.19, -37.44)),
        SpaceCompassConstellationStar("Betelgeuse", SpaceCompassDeepSkyReference(5.919529, 7.407063, 497.94881264938857, 27.33, 10.86)),
        SpaceCompassConstellationStar("Saiph", SpaceCompassDeepSkyReference(5.795941, -9.669605, 647.1356752167679, 1.55, -1.2)),
        SpaceCompassConstellationStar("Rigel", SpaceCompassDeepSkyReference(5.242298, -8.20164, 862.8476756744831, 1.87, -0.56)),
        SpaceCompassConstellationStar("Bellatrix", SpaceCompassDeepSkyReference(5.418851, 6.349702, 252.44307940153382, -8.75, -13.28)),
        SpaceCompassConstellationStar("Meissa", SpaceCompassDeepSkyReference(5.585633, 9.934158, 1098.1695021850333, -1.03, -1.86)),
        SpaceCompassConstellationStar("Tabit", SpaceCompassDeepSkyReference(4.830669, 6.961276, 26.315601178346803, 463.44, 11.62)),
        SpaceCompassConstellationStar("3Pi 4Ori", SpaceCompassDeepSkyReference(4.853434, 5.605104, 1052.1172001229263, -3.62, 1.03)),
        SpaceCompassConstellationStar("5    Ori", SpaceCompassDeepSkyReference(4.889659, 2.508225, 590.8629369030539, 29.67, -16.4)),
        SpaceCompassConstellationStar("10Pi 6Ori", SpaceCompassDeepSkyReference(4.975806, 1.714016, 945.3808947387128, -1.59, -7.93)),
        SpaceCompassConstellationStar("2Pi 2Ori", SpaceCompassDeepSkyReference(4.843534, 8.900176, 224.4709299808487, -0.87, -31.57)),
        SpaceCompassConstellationStar("7Pi 1Ori", SpaceCompassDeepSkyReference(4.914924, 10.150833, 116.3181274482841, 40.08, -128.37)),
    ),
    edges = listOf(0 to 1, 1 to 2, 3 to 4, 4 to 5, 5 to 6, 4 to 7, 7 to 8, 8 to 0, 0 to 9, 9 to 10, 10 to 2, 2 to 11, 11 to 12, 12 to 8, 11 to 13, 13 to 14, 14 to 15, 15 to 16, 13 to 17, 17 to 18, 5 to 7)
)

internal fun spaceCompassPatternPAV() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Peacock", SpaceCompassDeepSkyReference(20.427459, -56.73509, 178.8139294485142, 7.71, -86.15)),
        SpaceCompassConstellationStar("Gam Pav", SpaceCompassDeepSkyReference(21.440705, -65.366198, 30.207951389818597, 81.08, 800.68)),
        SpaceCompassConstellationStar("Bet Pav", SpaceCompassDeepSkyReference(20.749314, -66.203212, 135.110279462225, -42.4, 10.57)),
        SpaceCompassConstellationStar("Del Pav", SpaceCompassDeepSkyReference(20.145157, -66.182068, 19.9229361754268, 1210.29, -1130.34)),
        SpaceCompassConstellationStar("Eps Pav", SpaceCompassDeepSkyReference(20.009845, -72.910504, 105.07616942172052, 80.54, -131.34)),
        SpaceCompassConstellationStar("Zet Pav", SpaceCompassDeepSkyReference(18.71726, -71.428113, 218.45693253243843, 1.25, -158.29)),
        SpaceCompassConstellationStar("Kap Pav", SpaceCompassDeepSkyReference(18.949177, -67.233495, 500.2397350463534, -7.83, 16.65)),
        SpaceCompassConstellationStar("Lam Pav", SpaceCompassDeepSkyReference(18.870288, -62.187593, 1430.5104571189804, -1.33, -13.53)),
        SpaceCompassConstellationStar("Xi  Pav", SpaceCompassDeepSkyReference(18.387117, -61.493901, 468.6156126645614, 0.53, 1.7)),
        SpaceCompassConstellationStar("Pi  Pav", SpaceCompassDeepSkyReference(18.143, -63.668553, 129.9945166780005, 17.45, -207.64)),
        SpaceCompassConstellationStar("Eta Pav", SpaceCompassDeepSkyReference(17.762221, -64.723871, 352.2208384675178, -11.08, -56.37)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 3 to 4, 4 to 5, 5 to 6, 6 to 3, 6 to 7, 7 to 8, 8 to 9, 9 to 7, 9 to 10)
)

internal fun spaceCompassPatternPEG() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Algenib", SpaceCompassDeepSkyReference(0.220598, 15.183596, 391.544208301296, 4.7, -8.24)),
        SpaceCompassConstellationStar("Markab", SpaceCompassDeepSkyReference(23.079348, 15.205264, 133.3428380514687, 61.1, -42.56)),
        SpaceCompassConstellationStar("Scheat", SpaceCompassDeepSkyReference(23.062901, 28.082789, 196.0075890553474, 187.76, 137.61)),
        SpaceCompassConstellationStar("Matar", SpaceCompassDeepSkyReference(22.716704, 30.221245, 214.29452484023102, 13.11, -26.11)),
        SpaceCompassConstellationStar("27Pi 1Peg", SpaceCompassDeepSkyReference(22.153789, 33.172337, 288.6340433838812, -60.0, -65.86)),
        SpaceCompassConstellationStar("Sadalbari", SpaceCompassDeepSkyReference(22.833385, 24.601579, 106.10160507320931, 144.11, -43.44)),
        SpaceCompassConstellationStar("47Lam Peg", SpaceCompassDeepSkyReference(22.775521, 23.565654, 365.2367610323917, 56.99, -10.46)),
        SpaceCompassConstellationStar("24Iot Peg", SpaceCompassDeepSkyReference(22.116847, 25.345112, 38.2454230054797, 296.73, 26.93)),
        SpaceCompassConstellationStar("10Kap Peg", SpaceCompassDeepSkyReference(21.744092, 25.645036, 111.62082329664871, 46.66, 13.47)),
        SpaceCompassConstellationStar("46Xi  Peg", SpaceCompassDeepSkyReference(22.778216, 12.172888, 53.1546833429021, 233.06, -492.04)),
        SpaceCompassConstellationStar("Homam", SpaceCompassDeepSkyReference(22.691033, 10.831364, 204.3584969499782, 77.38, -10.98)),
        SpaceCompassConstellationStar("Biham", SpaceCompassDeepSkyReference(22.169996, 6.197865, 92.29083941588051, 281.96, 31.23)),
        SpaceCompassConstellationStar("Enif", SpaceCompassDeepSkyReference(21.736433, 9.875011, 689.5483982601206, 30.02, 1.38)),
        SpaceCompassConstellationStar("Alpheratz", SpaceCompassDeepSkyReference(0.139791, 29.090432, 97.0126052958434, 135.68, -162.95)),
    ),
    edges = listOf(0 to 1, 2 to 3, 3 to 4, 2 to 5, 5 to 6, 6 to 7, 7 to 8, 1 to 9, 9 to 10, 10 to 11, 11 to 12, 13 to 2, 13 to 0, 2 to 1)
)

internal fun spaceCompassPatternPIC() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Alp Pic", SpaceCompassDeepSkyReference(6.803194, -61.941391, 96.5530509596641, -68.39, 242.03)),
        SpaceCompassConstellationStar("Gam Pic", SpaceCompassDeepSkyReference(5.830451, -56.166663, 176.7783874952885, 81.61, -71.77)),
        SpaceCompassConstellationStar("Bet Pic", SpaceCompassDeepSkyReference(5.788079, -51.066514, 63.405125981257704, 4.65, 81.96)),
    ),
    edges = listOf(0 to 1, 1 to 2)
)

internal fun spaceCompassPatternPER() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Atik", SpaceCompassDeepSkyReference(3.738648, 32.288248, 1120.8122563941004, 8.11, -10.32)),
        SpaceCompassConstellationStar("44Zet Per", SpaceCompassDeepSkyReference(3.9022, 31.883635, 751.512239208322, 4.41, -9.15)),
        SpaceCompassConstellationStar("Menkib", SpaceCompassDeepSkyReference(3.98275, 35.791033, 1244.8717054670938, 1.92, 2.3)),
        SpaceCompassConstellationStar("45Eps Per", SpaceCompassDeepSkyReference(3.96423, 40.010215, 638.2707448708819, 12.61, -24.06)),
        SpaceCompassConstellationStar("39Del Per", SpaceCompassDeepSkyReference(3.715416, 47.787551, 516.0700609944006, 23.83, -41.93)),
        SpaceCompassConstellationStar("Mirfak", SpaceCompassDeepSkyReference(3.405378, 49.86118, 506.4539925106716, 24.11, -26.01)),
        SpaceCompassConstellationStar("23Gam Per", SpaceCompassDeepSkyReference(3.079942, 53.50644, 243.21872472742243, 0.5, -4.19)),
        SpaceCompassConstellationStar("Miram", SpaceCompassDeepSkyReference(2.844945, 55.895496, 879.1277712673788, 16.64, -13.76)),
        SpaceCompassConstellationStar("Algol", SpaceCompassDeepSkyReference(3.136148, 40.955648, 89.924574895667, 2.39, -1.44)),
        SpaceCompassConstellationStar("25Rho Per", SpaceCompassDeepSkyReference(3.08627, 38.840274, 307.6946220966692, 128.69, -106.61)),
        SpaceCompassConstellationStar("16    Per", SpaceCompassDeepSkyReference(2.843063, 38.318644, 120.7538541850041, 195.68, -108.92)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 5 to 8, 8 to 9, 9 to 10)
)
