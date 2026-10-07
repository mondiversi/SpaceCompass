package me.mondiversi.spacecompass

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

internal const val SPACE_COMPASS_PREFERENCES_NAME = "spaceCompass_preferences"
internal const val SPACE_COMPASS_NUMERIC_FORMAT_KEY = "numeric_format"
internal const val SPACE_COMPASS_NUMERIC_FORMAT_EXPANDED_KEY =
    "settings_numeric_format_expanded"

enum class SpaceCompassNumericFormat(
    val storedValue: String
) {
    SYSTEM("system"),
    INTERNATIONAL("international"),
    EUROPEAN("european"),
    AMERICAN("american");

    companion object {
        fun fromStoredValue(value: String?): SpaceCompassNumericFormat =
            entries.firstOrNull {
                it.storedValue == value
            } ?: SYSTEM
    }
}

internal val LocalSpaceCompassNumericFormat =
    staticCompositionLocalOf {
        SpaceCompassNumericFormat.SYSTEM
    }

internal fun loadSpaceCompassNumericFormat(
    context: Context
): SpaceCompassNumericFormat =
    SpaceCompassNumericFormat.fromStoredValue(
        context.getSharedPreferences(
            SPACE_COMPASS_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        ).getString(
            SPACE_COMPASS_NUMERIC_FORMAT_KEY,
            SpaceCompassNumericFormat.SYSTEM.storedValue
        )
    )

internal fun saveSpaceCompassNumericFormat(
    context: Context,
    format: SpaceCompassNumericFormat
) {
    context.getSharedPreferences(
        SPACE_COMPASS_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    ).edit {
        putString(
            SPACE_COMPASS_NUMERIC_FORMAT_KEY,
            format.storedValue
        )
    }
}

internal fun formatSpaceCompassNumber(
    value: Double,
    fractionDigits: Int,
    format: SpaceCompassNumericFormat,
    grouping: Boolean = true,
    minimumDigits: Int = fractionDigits,
    systemLocale: Locale = Locale.getDefault()
): String {
    if (!value.isFinite()) {
        return "—"
    }

    val symbols = spaceCompassNumericFormatSymbols(format, systemLocale)
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

internal fun formatSpaceCompassExportNumber(
    value: Double,
    format: SpaceCompassNumericFormat
): String {
    if (!value.isFinite()) {
        return "—"
    }

    return DecimalFormat(
        "0.#########",
        spaceCompassNumericFormatSymbols(format)
    ).apply {
        isGroupingUsed = false
        roundingMode = RoundingMode.HALF_UP
    }.format(value)
}

internal fun spaceCompassNumericFormatSymbols(
    format: SpaceCompassNumericFormat,
    systemLocale: Locale = Locale.getDefault()
): DecimalFormatSymbols =
    when (format) {
        SpaceCompassNumericFormat.SYSTEM ->
            DecimalFormatSymbols.getInstance(systemLocale)

        SpaceCompassNumericFormat.INTERNATIONAL ->
            DecimalFormatSymbols.getInstance(Locale.US).apply {
                groupingSeparator = '\u202F'
            }

        SpaceCompassNumericFormat.EUROPEAN ->
            DecimalFormatSymbols.getInstance(Locale.GERMANY)

        SpaceCompassNumericFormat.AMERICAN ->
            DecimalFormatSymbols.getInstance(Locale.US)
    }

