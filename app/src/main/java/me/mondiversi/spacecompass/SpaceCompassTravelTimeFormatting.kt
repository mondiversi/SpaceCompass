package me.mondiversi.spacecompass

import java.text.DecimalFormat
import java.util.Locale

/** Compact invariant time symbols retain the user's decimal separators and native digits. */
internal class SpaceCompassTravelTimeFormatter(numeric: SpaceCompassNumericFormat, deviceLocale: Locale) {
    private val symbols = spaceCompassNumericFormatSymbols(numeric, deviceLocale).apply { exponentSeparator = "×10^" }
    private val normal = DecimalFormat("#,##0.##", symbols)
    private val scientific = DecimalFormat("0.##E0", symbols)

    fun duration(seconds: Double?): String {
        val value = spaceCompassTravelDuration(seconds) ?: return "—"
        val format = if (value.amount >= 1e6 || value.amount > 0 && value.amount < .01) scientific else normal
        return (if (seconds == 0.0) "" else "≈ ") + format.format(value.amount) + " " + value.unit.symbol
    }
}

internal fun formatSpaceCompassTravelNumber(value: Double, numeric: SpaceCompassNumericFormat,
    deviceLocale: Locale): String = if (value >= 1e6) {
    DecimalFormat("0.##E0", spaceCompassNumericFormatSymbols(numeric, deviceLocale).apply {
        exponentSeparator = "×10^"
    }).format(value)
} else formatSpaceCompassNumber(value, 2, numeric, minimumDigits = 0, systemLocale = deviceLocale)

internal fun formatSpaceCompassTravelSpeed(speed: SpaceCompassTravelSpeed,
    numeric: SpaceCompassNumericFormat, miles: Boolean, deviceLocale: Locale): String {
    val perSecond = speed.kmPerSecond >= 1000
    val value = speed.kmPerSecond * (if (perSecond) 1 else 3600) / (if (miles) 1.609344 else 1.0)
    val unit = if (perSecond) { if (miles) "mi/s" else "km/s" } else if (miles) "mph" else "km/h"
    return "${formatSpaceCompassTravelNumber(value, numeric, deviceLocale)} $unit"
}
