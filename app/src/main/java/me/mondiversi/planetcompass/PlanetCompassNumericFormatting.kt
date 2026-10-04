package me.mondiversi.planetcompass

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal const val PLANET_COMPASS_PREFERENCES_NAME = "planetCompass_preferences"
internal const val PLANET_COMPASS_NUMERIC_FORMAT_KEY = "numeric_format"
internal const val PLANET_COMPASS_NUMERIC_FORMAT_EXPANDED_KEY =
    "settings_numeric_format_expanded"

enum class PlanetCompassNumericFormat(
    val storedValue: String
) {
    SYSTEM("system"),
    INTERNATIONAL("international"),
    EUROPEAN("european"),
    AMERICAN("american");

    companion object {
        fun fromStoredValue(value: String?): PlanetCompassNumericFormat =
            entries.firstOrNull {
                it.storedValue == value
            } ?: SYSTEM
    }
}

internal val LocalPlanetCompassNumericFormat =
    staticCompositionLocalOf {
        PlanetCompassNumericFormat.SYSTEM
    }

internal fun loadPlanetCompassNumericFormat(
    context: Context
): PlanetCompassNumericFormat =
    PlanetCompassNumericFormat.fromStoredValue(
        context.getSharedPreferences(
            PLANET_COMPASS_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).getString(
            PLANET_COMPASS_NUMERIC_FORMAT_KEY,
            PlanetCompassNumericFormat.SYSTEM.storedValue
        )
    )

internal fun savePlanetCompassNumericFormat(
    context: Context,
    format: PlanetCompassNumericFormat
) {
    context.getSharedPreferences(
        PLANET_COMPASS_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    ).edit {
        putString(
            PLANET_COMPASS_NUMERIC_FORMAT_KEY,
            format.storedValue
        )
    }
}

internal fun formatPlanetCompassNumber(
    value: Double,
    fractionDigits: Int,
    format: PlanetCompassNumericFormat,
    grouping: Boolean = true,
    minimumDigits: Int = fractionDigits
): String {
    if (!value.isFinite()) {
        return value.toString()
    }

    val symbols = numericFormatSymbols(format)
    val pattern =
        buildString {
            append(if (grouping) "#,##0" else "0")
            if (fractionDigits > 0) {
                append('.')
                repeat(fractionDigits) {
                    append('0')
                }
            }
        }

    return DecimalFormat(
        pattern,
        symbols
    ).apply {
        isGroupingUsed = grouping
        minimumFractionDigits = minimumDigits
        maximumFractionDigits = fractionDigits
        roundingMode = RoundingMode.HALF_UP
    }.format(value)
}

internal fun formatPlanetCompassExportNumber(
    value: Double,
    format: PlanetCompassNumericFormat
): String {
    if (!value.isFinite()) {
        return value.toString()
    }

    return DecimalFormat(
        "0.#########",
        numericFormatSymbols(format)
    ).apply {
        isGroupingUsed = false
        roundingMode = RoundingMode.HALF_UP
    }.format(value)
}

private fun numericFormatSymbols(
    format: PlanetCompassNumericFormat
): DecimalFormatSymbols =
    when (format) {
        PlanetCompassNumericFormat.SYSTEM ->
            DecimalFormatSymbols.getInstance(Locale.getDefault())

        PlanetCompassNumericFormat.INTERNATIONAL ->
            DecimalFormatSymbols.getInstance(Locale.US).apply {
                groupingSeparator = '\u202F'
            }

        PlanetCompassNumericFormat.EUROPEAN ->
            DecimalFormatSymbols.getInstance(Locale.GERMANY)

        PlanetCompassNumericFormat.AMERICAN ->
            DecimalFormatSymbols.getInstance(Locale.US)
    }

