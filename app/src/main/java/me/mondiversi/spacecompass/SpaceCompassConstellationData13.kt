package me.mondiversi.spacecompass

/** Stellarium western line patterns, HYG ICRS/J2000 star references. CC BY-SA 4.0. */
internal fun spaceCompassPatternAQR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Sadalsuud", SpaceCompassDeepSkyReference(21.525982, -5.571172, 537.3250198163541, 22.79, -6.7)),
        SpaceCompassConstellationStar("Sadalmelik", SpaceCompassDeepSkyReference(22.096399, -0.319851, 523.5253434758672, 17.9, -9.93)),
        SpaceCompassConstellationStar("Sadachbia", SpaceCompassDeepSkyReference(22.360938, -1.387331, 163.7331108564216, 129.24, 8.9)),
        SpaceCompassConstellationStar("55Zet1Aqr", SpaceCompassDeepSkyReference(22.480531, -0.019972, 91.874990034313, 191.32, 37.47)),
        SpaceCompassConstellationStar("62Eta Aqr", SpaceCompassDeepSkyReference(22.589272, -0.117498, 167.8622505981036, 88.56, -56.1)),
        SpaceCompassConstellationStar("73Lam Aqr", SpaceCompassDeepSkyReference(22.87691, -7.579599, 385.0726134549726, 19.51, 32.71)),
        SpaceCompassConstellationStar("91Psi1Aqr", SpaceCompassDeepSkyReference(23.264859, -9.087737, 149.8192797837396, 368.56, -17.02)),
        SpaceCompassConstellationStar("98    Aqr", SpaceCompassDeepSkyReference(23.382842, -20.10058, 163.4049975404554, -120.27, -96.7)),
        SpaceCompassConstellationStar("Ancha", SpaceCompassDeepSkyReference(22.280565, -7.78329, 187.4463102971001, 118.95, -21.91)),
        SpaceCompassConstellationStar("33Iot Aqr", SpaceCompassDeepSkyReference(22.107286, -13.869679, 175.1645657384289, 40.45, -57.16)),
        SpaceCompassConstellationStar("57Sig Aqr", SpaceCompassDeepSkyReference(22.510782, -10.67795, 289.6591528789923, 1.79, -26.26)),
        SpaceCompassConstellationStar("71Tau2Aqr", SpaceCompassDeepSkyReference(22.826528, -13.592632, 317.581726530267, -12.58, -38.8)),
        SpaceCompassConstellationStar("Skat", SpaceCompassDeepSkyReference(22.910837, -15.82082, 160.5889633753936, -44.08, -24.81)),
        SpaceCompassConstellationStar("88    Aqr", SpaceCompassDeepSkyReference(23.157443, -21.17241, 270.6693501001652, 55.99, 31.25)),
        SpaceCompassConstellationStar("Albali", SpaceCompassDeepSkyReference(20.794598, -9.495776, 207.7430216813711, 31.89, -35.32)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 1 to 8, 8 to 9, 8 to 10, 10 to 11, 11 to 12, 12 to 13, 14 to 0)
)

internal fun spaceCompassPatternVIR() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("3Nu  Vir", SpaceCompassDeepSkyReference(11.764322, 6.529376, 293.8346068263077, -19.65, -180.02)),
        SpaceCompassConstellationStar("13    Vir", SpaceCompassDeepSkyReference(12.311199, -0.787184, 373.6039767459075, 33.51, -16.17)),
        SpaceCompassConstellationStar("Porrima", SpaceCompassDeepSkyReference(12.694345, -1.449375, 38.111372734245, -616.66, 60.66)),
        SpaceCompassConstellationStar("Spica", SpaceCompassDeepSkyReference(13.419883, -11.161322, 249.7369599357569, -42.5, -31.73)),
        SpaceCompassConstellationStar("Kang", SpaceCompassDeepSkyReference(14.214929, -10.273702, 254.809670078125, 8.04, 140.79)),
        SpaceCompassConstellationStar("Syrma", SpaceCompassDeepSkyReference(14.266908, -6.000547, 72.527393709149, -25.84, -419.84)),
        SpaceCompassConstellationStar("107Mu  Vir", SpaceCompassDeepSkyReference(14.717673, -5.658207, 59.5936625514555, 104.23, -319.9)),
        SpaceCompassConstellationStar("Heze", SpaceCompassDeepSkyReference(13.57822, -0.59582, 74.0759841904686, -278.89, 48.56)),
        SpaceCompassConstellationStar("93Tau Vir", SpaceCompassDeepSkyReference(14.027443, 1.544532, 224.93537666269353, 17.41, -21.2)),
        SpaceCompassConstellationStar("109    Vir", SpaceCompassDeepSkyReference(14.770812, 1.892885, 134.4974316285267, -116.04, -21.75)),
        SpaceCompassConstellationStar("Minelauva", SpaceCompassDeepSkyReference(12.926725, 3.39747, 198.3921183327121, -471.44, -52.81)),
        SpaceCompassConstellationStar("Vindemiatrix", SpaceCompassDeepSkyReference(13.036278, 10.95915, 109.59571834750942, -275.05, 19.96)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 3 to 7, 7 to 8, 8 to 9, 7 to 10, 10 to 11, 10 to 2)
)

internal fun spaceCompassPatternVEL() = SpaceCompassConstellationPattern(
    stars = listOf(
        SpaceCompassConstellationStar("Gam2Vel", SpaceCompassDeepSkyReference(8.158876, -47.336588, 1116.9740481413266, -5.93, 9.9)),
        SpaceCompassConstellationStar("HIP 42536", SpaceCompassDeepSkyReference(8.671554, -52.921887, 493.4286114108443, -24.62, 35.09)),
        SpaceCompassConstellationStar("Alsephina", SpaceCompassDeepSkyReference(8.745059, -54.708821, 80.55247138245751, 28.78, -104.14)),
        SpaceCompassConstellationStar("Markeb", SpaceCompassDeepSkyReference(9.368562, -55.010668, 572.2041828475923, -10.72, 11.24)),
        SpaceCompassConstellationStar("Phi Vel", SpaceCompassDeepSkyReference(9.947708, -54.56779, 1591.0067920831073, -13.13, 2.83)),
        SpaceCompassConstellationStar("Mu  Vel", SpaceCompassDeepSkyReference(10.779488, -49.420255, 117.1537400879515, 62.55, -53.57)),
        SpaceCompassConstellationStar("HIP 51986", SpaceCompassDeepSkyReference(10.621717, -48.22562, 87.5351532726368, -131.48, -1.58)),
        SpaceCompassConstellationStar("HIP 50191", SpaceCompassDeepSkyReference(10.245607, -42.121942, 101.3537466830304, -150.68, 49.84)),
        SpaceCompassConstellationStar("Psi Vel", SpaceCompassDeepSkyReference(9.511674, -40.466769, 61.3653439951219, -147.14, 48.65)),
        SpaceCompassConstellationStar("Suhail", SpaceCompassDeepSkyReference(9.133268, -43.432589, 544.5014385948873, -23.21, 14.28)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9, 9 to 0)
)
