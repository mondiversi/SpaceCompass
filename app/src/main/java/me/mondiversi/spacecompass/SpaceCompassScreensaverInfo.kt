package me.mondiversi.spacecompass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import java.util.TimeZone
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay

internal data class SpaceCompassScreensaverInfo(val date: String, val time: String,
    val batteryPercent: Int?, val batteryText: String, val charging: Boolean, val batteryDescription: String, val rtl: Boolean)

internal fun spaceCompassScreensaverBatteryPercent(level: Int, scale: Int): Int? =
    if (scale > 0 && level in 0..scale) kotlin.math.round(level.toDouble() / scale * 100).toInt() else null

/** Use the same real-world time zone and app locale as the date, including midnight boundaries. */
internal fun formatSpaceCompassScreensaverWeekday(timestamp: Long, locale: Locale, zone: TimeZone): String =
    SimpleDateFormat("EEE", locale).apply { timeZone = zone }.format(Date(timestamp))
        .trimEnd('.').replaceFirstChar { it.titlecase(locale) }

private data class ScreensaverBattery(val percent: Int? = null, val charging: Boolean = false)

/** Register only while the saver is visible. Battery broadcasts require no additional permission. */
@Composable
internal fun rememberSpaceCompassScreensaverInfo(): SpaceCompassScreensaverInfo {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - value.mod(60_000L))
        }
    }
    val battery by produceState(ScreensaverBattery(), context) {
        fun update(intent: Intent?) {
            if (intent == null) return
            value = ScreensaverBattery(spaceCompassScreensaverBatteryPercent(
                intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)),
                intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = update(intent)
        }
        update(ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED))
        awaitDispose { context.unregisterReceiver(receiver) }
    }
    val locale = LocalConfiguration.current.locales[0]
    val numeric = LocalSpaceCompassNumericFormat.current
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val zone = TimeZone.getDefault()
    val percent = battery.percent?.let {
        formatSpaceCompassNumber(it.toDouble(), 0, numeric, systemLocale = deviceLocale) + "%"
    } ?: "—"
    return SpaceCompassScreensaverInfo(
        formatSpaceCompassScreensaverWeekday(now, locale, zone) + " " +
            formatSpaceCompassDateOnly(now, LocalSpaceCompassDateFormat.current, locale, zone, numeric, deviceLocale),
        formatSpaceCompassTimeOnly(now, resolveSpaceCompassTimeFormat(context, LocalSpaceCompassTimeFormat.current),
            locale, zone, numeric, deviceLocale, includeSeconds = false),
        battery.percent, percent, battery.charging,
        stringResource(if (battery.charging) R.string.screensaver_battery_charging else R.string.screensaver_battery, percent),
        LocalLayoutDirection.current == LayoutDirection.Rtl)
}
