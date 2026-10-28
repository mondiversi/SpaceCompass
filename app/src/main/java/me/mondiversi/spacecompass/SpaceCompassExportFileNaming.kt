package me.mondiversi.spacecompass

import java.util.Locale
import java.util.TimeZone

/** Preserve Unicode names without splitting a surrogate pair or exceeding common filesystem byte limits. */
internal fun spaceCompassLimitFileText(value: String, length: Int, bytes: Int = 220, trimDelimiters: Boolean = true): String {
    var end = value.length.coerceAtMost(length)
    if (end > 0 && Character.isHighSurrogate(value[end-1])) end--
    while (end > 0 && value.substring(0,end).toByteArray(Charsets.UTF_8).size > bytes) {
        end -= Character.charCount(value.codePointBefore(end))
    }
    return value.substring(0,end).let { if (trimDelimiters) it.trimEnd('-', '_', ' ', '.') else it }
}

/** Hyphens join words inside a field; underscores are reserved for field boundaries. */
internal fun spaceCompassExportFilePart(value: String, limit: Int = 40, bytes: Int = 120): String {
    val clean = value.replace(Regex("[\\p{Cf}\\p{Cc}]"), " ")
        .replace(Regex("[\\\\/:*?\"<>|_\\s\u00a0\u202f]+"), "-")
        .replace(Regex("-+"), "-").trim('-', ' ', '.')
    return spaceCompassLimitFileText(clean,limit,bytes)
}

/** Capture time and its frozen zone, never the scenario's simulated instant or the later export time. */
internal fun spaceCompassExportFileStem(capturedAt: Long, formatting: SpaceCompassPanoramaFormatting,
    zoneId: String, fields: List<String>): String {
    val zone = TimeZone.getTimeZone(zoneId)
    val dateLocale = if (formatting.date==SpaceCompassDateFormat.SYSTEM) formatting.deviceLocale else formatting.locale
    val date = formatSpaceCompassDateOnly(capturedAt,formatting.date,dateLocale,zone,formatting.numeric,
        formatting.deviceLocale,allowHistorical=true)
    val time = formatSpaceCompassTimeOnly(capturedAt,formatting.time,formatting.locale,zone,formatting.numeric,
        formatting.deviceLocale,includeSeconds=true,allowHistorical=true)
    val fraction = formatSpaceCompassNumber(Math.floorMod(capturedAt,1000).toDouble()/1000,3,formatting.numeric,
        grouping=false,systemLocale=formatting.deviceLocale).drop(1)
    val offset = zone.getOffset(capturedAt)/60_000
    val offsetText = String.format(Locale.ROOT,"UTC%s%02d.%02d",if (offset<0) "-" else "+",
        kotlin.math.abs(offset)/60,kotlin.math.abs(offset)%60)
    val parts = listOf("SpaceCompass",date,time+fraction,offsetText)+fields
    return spaceCompassLimitFileText(parts.map { spaceCompassExportFilePart(it) }
        .filter(String::isNotBlank).joinToString("_"),180,220)
}
