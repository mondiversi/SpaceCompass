package me.mondiversi.spacecompass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Object-specific, sky-readable colours stay identical for one or many selected objects. */
internal fun spaceCompassCelestialPathTint(body: SpaceCompassCelestialBody): Color = when (body) {
    SpaceCompassCelestialBody.CERES -> Color(0xFFC1B5A0)
    SpaceCompassCelestialBody.ENCELADUS -> Color(0xFFB6E5EF)
    SpaceCompassCelestialBody.VEGA -> Color(0xFFBEDFFF)
    SpaceCompassCelestialBody.DENEB -> Color(0xFFE0E9FF)
    SpaceCompassCelestialBody.ALTAIR -> Color(0xFFF4E4B6)
    SpaceCompassCelestialBody.SOUTHERN_CROSS -> Color(0xFF96DFC9)
    SpaceCompassCelestialBody.NEW_HORIZONS -> Color(0xFFB7F0A1)
    SpaceCompassCelestialBody.JWST -> Color(0xFFF4D76A)
    SpaceCompassCelestialBody.PARKER_SOLAR_PROBE -> Color(0xFFFF9C59)
    SpaceCompassCelestialBody.APOPHIS -> Color(0xFFC0C1DA)
    SpaceCompassCelestialBody.ATLAS_3I -> Color(0xFF83E0BC)
    SpaceCompassCelestialBody.EUROPA_CLIPPER -> Color(0xFF9DB7FF)
    SpaceCompassCelestialBody.JUICE -> Color(0xFFCE9DEC)
    SpaceCompassCelestialBody.HUBBLE -> Color(0xFF69DADF)
    SpaceCompassCelestialBody.TIANGONG -> Color(0xFFFFA5B3)
    SpaceCompassCelestialBody.M87 -> Color(0xFFFFB56C)
    SpaceCompassCelestialBody.PROXIMA_B -> Color(0xFF93DAC0)
    SpaceCompassCelestialBody.TESLA_ROADSTER -> Color(0xFFFF6F86)
    SpaceCompassCelestialBody.SIRIUS -> Color(0xFFB9DEFF)
    SpaceCompassCelestialBody.BETELGEUSE -> Color(0xFFFFB08C)
    SpaceCompassCelestialBody.TITAN -> Color(0xFFE9B765)
    SpaceCompassCelestialBody.ORION_NEBULA -> Color(0xFFFFA3D5)
    SpaceCompassCelestialBody.PLEIADES -> Color(0xFF8ADBE5)
    SpaceCompassCelestialBody.LV_426 -> Color(0xFFB5CF8D)
    SpaceCompassCelestialBody.TRAPPIST_1_E -> Color(0xFF8ED6BE)
    SpaceCompassCelestialBody.EARTH_CENTER -> Color(0xFF52C8E8)
    SpaceCompassCelestialBody.SUN -> Color(0xFFFFD447)
    SpaceCompassCelestialBody.MOON -> Color(0xFFDCE3F0)
    SpaceCompassCelestialBody.MERCURY -> Color(0xFFBDAEA5)
    SpaceCompassCelestialBody.VENUS -> Color(0xFFFFE5B4)
    SpaceCompassCelestialBody.MARS -> Color(0xFFFF806D)
    SpaceCompassCelestialBody.JUPITER -> Color(0xFFFFB27B)
    SpaceCompassCelestialBody.IO -> Color(0xFFF2EB88)
    SpaceCompassCelestialBody.EUROPA -> Color(0xFF9EDAC9)
    SpaceCompassCelestialBody.SATURN -> Color(0xFFDCC895)
    SpaceCompassCelestialBody.URANUS -> Color(0xFF78DFE8)
    SpaceCompassCelestialBody.NEPTUNE -> Color(0xFF849AFF)
    SpaceCompassCelestialBody.PLUTO -> Color(0xFFCAA1C5)
    SpaceCompassCelestialBody.HALLEY -> Color(0xFF79F1D6)
    SpaceCompassCelestialBody.COMET_67P -> Color(0xFFB1D7FF)
    SpaceCompassCelestialBody.SEDNA -> Color(0xFFE991A4)
    SpaceCompassCelestialBody.ISS -> Color(0xFFA3EDA7)
    SpaceCompassCelestialBody.STARLINK_V3 -> Color(0xFF55C6FF)
    SpaceCompassCelestialBody.VOYAGER_1 -> Color(0xFFDBA5FF)
    SpaceCompassCelestialBody.VOYAGER_2 -> Color(0xFFC1CFFF)
    SpaceCompassCelestialBody.POLARIS -> Color(0xFFFFFFFF)
    SpaceCompassCelestialBody.ALPHA_CENTAURI -> Color(0xFFFFE4A3)
    SpaceCompassCelestialBody.SAGITTARIUS_A -> Color(0xFFFFA24C)
    SpaceCompassCelestialBody.STEPHENSON_2_18 -> Color(0xFFFF694D)
    SpaceCompassCelestialBody.RX_J1856 -> Color(0xFF75E6FF)
    SpaceCompassCelestialBody.PSR_J0437 -> Color(0xFFBD9AFF)
    SpaceCompassCelestialBody.PROXIMA_CENTAURI -> Color(0xFFFF9570)
    SpaceCompassCelestialBody.RIGEL -> Color(0xFF91CFFF)
    SpaceCompassCelestialBody.TON_618 -> Color(0xFFFF70BF)
    SpaceCompassCelestialBody.ANDROMEDA_CORE -> Color(0xFFA9ACFF)
    // Stable high-contrast hue spacing for the additional catalogue; old colours never change.
    else -> spaceCompassLargeCatalogTint(body) ?: Color.hsv(((body.ordinal - SpaceCompassCelestialBody.GANYMEDE.ordinal) * 137.508f) % 360f, .38f, 1f)
}

/** The orbit and off-screen pointer share the same treatment below the horizon. */
internal fun spaceCompassCelestialPathVisibilityTint(tint: Color, belowHorizon: Boolean): Color =
    if (belowHorizon) lerp(tint, Color(0xFF8290A5), 0.40f) else tint

/** Reuse the rendered orbit stroke colour, including opacity, for its off-screen pointer. */
internal fun spaceCompassCelestialPathLineTint(tint: Color, belowHorizon: Boolean): Color =
    spaceCompassCelestialPathVisibilityTint(tint, belowHorizon).copy(alpha = 0.85f)
