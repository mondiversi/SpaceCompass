package me.mondiversi.spacecompass

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources

internal fun interface SpaceCompassTextResources { fun text(id: Int, arguments: Array<out Any>): String }
internal fun SpaceCompassTextResources.getString(id: Int, vararg arguments: Any): String = text(id,arguments)
internal fun Resources.spaceCompassTextResources() = SpaceCompassTextResources { id,args ->
    if (args.isEmpty()) getString(id) else getString(id,*args)
}

internal data class SpaceCompassDataExportProfile(val formatting: SpaceCompassPanoramaFormatting,
    val units: SpaceCompassUnits, val strings: SpaceCompassTextResources)
internal data class SpaceCompassDataExportProfiles(val selected: SpaceCompassDataExportProfile,
    val international: SpaceCompassDataExportProfile) {
    fun forMode(mode: SpaceCompassPanoramaExportMode) =
        if (mode == SpaceCompassPanoramaExportMode.SELECTED) selected else international
}

@Composable
internal fun rememberSpaceCompassDataExportProfiles(): SpaceCompassDataExportProfiles {
    val context=LocalContext.current
    val configuration=LocalConfiguration.current
    val resources=LocalResources.current
    val international=remember(context,configuration) { context.createConfigurationContext(Configuration(configuration).apply {
        setLocale(spaceCompassPanoramaExportLocale);setLayoutDirection(spaceCompassPanoramaExportLocale)
    }).resources.spaceCompassTextResources() }
    val units=LocalSpaceCompassUnits.current
    val selected=SpaceCompassDataExportProfile(SpaceCompassPanoramaFormatting(configuration.locales[0],
        LocalSpaceCompassNumericFormat.current,LocalSpaceCompassDateFormat.current,LocalSpaceCompassTimeFormat.current,
        units.feet,units.dms,LocalSpaceCompassDeviceLocale.current),units,resources.spaceCompassTextResources())
    return SpaceCompassDataExportProfiles(selected,SpaceCompassDataExportProfile(spaceCompassPanoramaInternationalFormatting,
        SpaceCompassUnits(pressure=SpaceCompassPressureUnit.PASCAL),international))
}

internal fun spaceCompassOrbitExportVocabulary(resources: SpaceCompassTextResources) = SpaceCompassOrbitExportVocabulary(
    point=resources.getString(R.string.celestial_point_number),time=resources.getString(R.string.format_time_heading),
    azimuth=resources.getString(R.string.capture_table_azimuth),elevation=resources.getString(R.string.capture_table_elevation),
    events=resources.getString(R.string.capture_table_events),selected=resources.getString(R.string.capture_table_selected),
    capturedAt=resources.getString(R.string.capture_table_captured),observedAt=resources.getString(R.string.capture_table_observed),
    context=resources.getString(R.string.capture_table_context),legend=resources.getString(R.string.capture_table_legend),
    illuminated=resources.getString(R.string.celestial_view_illuminated),pass=resources.getString(R.string.celestial_pass),
    fictionNote=resources.getString(R.string.scifi_reference_note),referenceLabel=resources.getString(R.string.scifi_reference))
