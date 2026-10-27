package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Exploratory state is isolated from the rotating model and never changes the scenario clock. */
@Composable
internal fun SpaceCompassTravelTimeIsland(body: SpaceCompassCelestialBody, distanceKm: Double?,
    primaryText: Color, secondaryText: Color) {
    val preferences = LocalSpaceCompassPreferences.current
    var index by remember(preferences) { mutableFloatStateOf(spaceCompassRestoreTravelSpeed(
        preferences?.getString(SPACE_COMPASS_TRAVEL_SPEED_KEY, null)).toFloat()) }
    val speed = spaceCompassTravelSpeeds[index.roundToInt().coerceIn(spaceCompassTravelSpeeds.indices)]
    val numeric = LocalSpaceCompassNumericFormat.current
    val units = LocalSpaceCompassUnits.current
    val deviceLocale = LocalSpaceCompassDeviceLocale.current
    val formatter = remember(numeric, deviceLocale) { SpaceCompassTravelTimeFormatter(numeric, deviceLocale) }
    val distance = spaceCompassTravelDistanceKm(body, distanceKm, distanceKm.takeIf { body.isFictional })
    val duration = formatter.duration(spaceCompassTravelSeconds(distance, speed))
    val name = when {
        speed.nameResource != null -> stringResource(speed.nameResource)
        speed.warp != null -> stringResource(R.string.travel_warp,
            formatSpaceCompassNumber(speed.warp, 3, numeric, grouping = false, minimumDigits = 0, systemLocale = deviceLocale))
        else -> "${formatSpaceCompassTravelNumber(speed.lightFraction!!, numeric, deviceLocale)} c"
    }
    val speedText = formatSpaceCompassTravelSpeed(speed, numeric, units.miles, deviceLocale)
    val title = stringResource(R.string.travel_title)
    val speedLabel = stringResource(R.string.travel_speed)
    val durationLabel = stringResource(R.string.travel_duration)
    Column(Modifier.fillMaxWidth().testTag("observatory-travel-island").clip(RoundedCornerShape(18.dp))
        .background(spaceCompassSettingsCardColor()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpaceCompassSettingsGroupIcon("speed", primaryText)
            Text(title, color = primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)
        }
        SpaceCompassSettingsDescription(stringResource(R.string.travel_description))
        val lightEquivalent = if (speed.warp != null) "≈ ${formatSpaceCompassTravelNumber(
            speed.kmPerSecond / SPACE_COMPASS_TRAVEL_LIGHT_KM_S, numeric, deviceLocale)} c" else null
        SpaceCompassTravelSpeedReadout(name, speedText, lightEquivalent, primaryText, secondaryText)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Slider(index, onValueChange = { value ->
                val next = value.roundToInt().coerceIn(spaceCompassTravelSpeeds.indices)
                if (index != next.toFloat()) {
                    index = next.toFloat()
                    // Persist each discrete step, including a drag interrupted by navigation or backgrounding.
                    preferences?.edit()?.putString(SPACE_COMPASS_TRAVEL_SPEED_KEY, spaceCompassTravelSpeeds[next].key)?.apply()
                }
            },
                valueRange = 0f..spaceCompassTravelSpeeds.lastIndex.toFloat(), steps = spaceCompassTravelSpeeds.size - 2,
                colors = spaceCompassSliderColors(), modifier = Modifier.fillMaxWidth().testTag("travel-speed-slider")
                    .semantics { contentDescription = speedLabel; stateDescription = "$name · $speedText · $durationLabel: $duration" })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(if (body.isFictional) R.string.scifi_estimated_distance else R.string.celestial_view_observer_distance), Modifier.weight(.52f),
                color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
            Text(formatSpaceCompassSelectedDistance(body, distance, numeric, units.distance, units.feet) ?: "—",
                Modifier.weight(.48f).testTag("travel-distance"), color = primaryText, fontSize = 12.sp, lineHeight = 16.sp,
                style = TextStyle(textDirection = TextDirection.ContentOrLtr))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(durationLabel, Modifier.weight(.52f), color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
            Text(duration, Modifier.weight(.48f).testTag("travel-duration"), color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp,
                style = TextStyle(textDirection = TextDirection.ContentOrLtr))
        }
        if (distance == null) SpaceCompassSettingsDescription(stringResource(R.string.travel_distance_unavailable))
        else body.fictionalReference?.let { SpaceCompassSettingsDescription(
            stringResource(R.string.scifi_distance_reference_note, it.referenceName)) }
        if (speed.warp != null) SpaceCompassSettingsDescription(stringResource(R.string.travel_warp_note))
    }
}
