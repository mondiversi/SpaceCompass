package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpaceCompassRepositoryActions() {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val state by SpaceCompassUpdates.state.collectAsState()
    val text = MaterialTheme.colorScheme.onSurface
    Text("github.com/mondiversi/SpaceCompass", fontSize = 12.sp, color = text.copy(alpha = .65f))
    CompositionLocalProvider(LocalSpaceCompassSettingsActionButtons provides true) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { uri.openUri(SpaceCompassUpdatePolicy.REPOSITORY_URL) },
                modifier = Modifier.fillMaxWidth().testTag("open-github-repository"),
                colors = spaceCompassOutlinedActionColors(text), border = spaceCompassOutlinedActionBorder(true, text)) {
                Icon(painterResource(R.drawable.ic_repository), null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open_github_repository))
            }
            val enabled = !state.checking && !state.busy
            OutlinedButton(onClick = { SpaceCompassUpdates.check(context, manual = true) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("check-app-updates"),
                colors = spaceCompassPrimaryOutlinedButtonColors(), border = spaceCompassPrimaryOutlinedButtonBorder(enabled, text)) {
                if (state.checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(painterResource(R.drawable.ic_update), null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (state.checking) R.string.update_checking else R.string.check_for_updates))
            }
        }
    }
}
