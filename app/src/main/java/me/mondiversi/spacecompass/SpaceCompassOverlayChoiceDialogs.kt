package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

internal val SpaceCompassPanoramaPosition.labelResource: Int get() = when (this) {
    SpaceCompassPanoramaPosition.COMPLETE -> R.string.panorama_position_complete
    SpaceCompassPanoramaPosition.AREA -> R.string.panorama_position_area
    SpaceCompassPanoramaPosition.HIDDEN -> R.string.panorama_position_hidden
}

internal val SpaceCompassPanoramaPosition.detailResource: Int get() = when (this) {
    SpaceCompassPanoramaPosition.COMPLETE -> R.string.panorama_position_complete_detail
    SpaceCompassPanoramaPosition.AREA -> R.string.panorama_position_area_detail
    SpaceCompassPanoramaPosition.HIDDEN -> R.string.panorama_position_hidden_detail
}


private data class SpaceCompassOverlayChoice(val key: String,val label: Int)

/** Shared radio choices use the standard modal, including its scrolling and close action. */
@Composable
private fun SpaceCompassOverlayChoiceDialog(visible: Boolean,title: Int,selectedKey: String,
    choices: List<SpaceCompassOverlayChoice>,tag: String,onChoose: (String)->Unit,onDismiss: ()->Unit) {
    if(!visible) return
    SpaceCompassAlertDialog(onDismissRequest=onDismiss,confirmButton=null,
        title={Text(stringResource(title))},text={
            Column(Modifier.fillMaxWidth().selectableGroup().testTag(tag)) {
                choices.forEach { choice ->
                    SpaceCompassSettingsRadioRow(choice.key==selectedKey,{onChoose(choice.key)},
                        Modifier.fillMaxWidth().testTag("$tag-${choice.key}")) {
                        Text(stringResource(choice.label),Modifier.weight(1f))
                    }
                }
            }
        })
}

@Composable
internal fun SpaceCompassPanoramaLabelsDialog(visible: Boolean,selected: SpaceCompassPanoramaLabelMode,
    onChoose: (SpaceCompassPanoramaLabelMode)->Unit,onDismiss: ()->Unit,
    modes: List<SpaceCompassPanoramaLabelMode> = SpaceCompassPanoramaLabelMode.entries) =
    SpaceCompassOverlayChoiceDialog(visible,R.string.panorama_point_labels,selected.key,
        modes.map { SpaceCompassOverlayChoice(it.key,it.labelResource) },
        "panorama-labels-dialog",{ key -> onChoose(SpaceCompassPanoramaLabelMode.entries.first { it.key==key }) },onDismiss)

@Composable
internal fun SpaceCompassPanoramaPositionDialog(visible: Boolean,selected: SpaceCompassPanoramaPosition,
    onChoose: (SpaceCompassPanoramaPosition)->Unit,onDismiss: ()->Unit) =
    SpaceCompassOverlayChoiceDialog(visible,R.string.panorama_position,selected.key,
        SpaceCompassPanoramaPosition.entries.map { SpaceCompassOverlayChoice(it.key,it.labelResource) },
        "panorama-position-dialog",{ key -> onChoose(SpaceCompassPanoramaPosition.entries.first { it.key==key }) },onDismiss)

@Composable
internal fun SpaceCompassPanoramaCenterDialog(visible: Boolean,selected: SpaceCompassPanoramaCenter,
    onChoose: (SpaceCompassPanoramaCenter)->Unit,onDismiss: ()->Unit) =
    SpaceCompassOverlayChoiceDialog(visible,R.string.panorama_center,selected.key,
        SpaceCompassPanoramaCenter.entries.map { SpaceCompassOverlayChoice(it.key,it.labelResource) },
        "panorama-center-dialog",{ key -> onChoose(SpaceCompassPanoramaCenter.entries.first { it.key==key }) },onDismiss)
