package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The same scrollable page and action footer for every export source. */
@Composable
internal fun SpaceCompassDataExportPage(onBack: () -> Unit, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    val foreground = MaterialTheme.colorScheme.onSurface
    val secondary = foreground.copy(alpha = .72f)
    val scroll = rememberScrollState()
    Surface(modifier.fillMaxSize().testTag("page-export"), color = spaceCompassPageBackground(), contentColor = foreground) {
        Column(Modifier.fillMaxSize()) {
            SpaceCompassPageToolbar(stringResource(R.string.panorama_export_title), onBack)
            Column(Modifier.fillMaxWidth().weight(1f).scrollbarOverlay(scroll, secondary.copy(alpha = .46f))
                .verticalScroll(scroll).padding(spaceCompassPageContentPadding)) {
                SpaceCompassSettingsIsland {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SpaceCompassSettingsDescription(stringResource(R.string.panorama_export_description), pageDescription = true)
                        content()
                    }
                }
            }
        }
    }
}

@Composable
internal fun SpaceCompassDataExportActions(save: () -> Unit, share: () -> Unit, enabled: Boolean,
    saveModifier: Modifier = Modifier, shareModifier: Modifier = Modifier, status: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SpaceCompassSettingsActionGap)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .16f))
        Spacer(Modifier.height(2.dp))
        status()
        SpaceCompassSettingsActionButton(stringResource(R.string.panorama_save_phone), save, saveModifier, enabled) {
            SpaceCompassPanoramaActionIcon(false)
        }
        SpaceCompassSettingsActionButton(stringResource(R.string.panorama_share), share, shareModifier, enabled) {
            SpaceCompassPanoramaActionIcon(true)
        }
    }
}

@Composable
internal fun SpaceCompassPanoramaExportPage(state: SpaceCompassCaptureExportState) {
    val secondary = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)
    SpaceCompassDataExportPage(state.close) {
        Text(stringResource(R.string.capture_export_files_heading),fontSize=15.sp,fontWeight=FontWeight.Bold)
        Column(Modifier.fillMaxWidth().testTag("capture-export-types"),
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            state.options.forEach { type ->
                SpaceCompassExportFileRow(type,type in state.types,
                    { state.changeType(type,it) },Modifier.testTag("capture-export-type-${type.key}"),
                    enabled = !state.preparing && !state.saving)
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(Modifier.testTag("capture-export-profile-divider"), color=secondary.copy(alpha=.22f))
        Spacer(Modifier.height(2.dp))
        state.position?.let { position ->
            SpaceCompassExportPositionChoices(position,state.changePosition,!state.preparing && !state.saving)
            HorizontalDivider(color=secondary.copy(alpha=.22f))
            Spacer(Modifier.height(2.dp))
        }
        SpaceCompassExportProfileChoices(state.mode, state.changeMode,
            enabled = !state.preparing && !state.saving)
        SpaceCompassDataExportActions(state.save, state.share, state.enabled,
            Modifier.testTag("panorama-export-document"), Modifier.testTag("panorama-export-share")) {
            if (state.failed) {
                SpaceCompassSettingsDescription(stringResource(R.string.capture_export_error))
                TextButton(state.retry,Modifier.testTag("panorama-export-retry")) {
                    Text(stringResource(R.string.sun_finder_retry))
                }
            } else if (state.preparing) Row(Modifier.fillMaxWidth().padding(vertical=6.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)
                SpaceCompassSettingsDescription(stringResource(R.string.capture_export_preparing))
            }
        }
    }
}
