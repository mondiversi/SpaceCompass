package me.mondiversi.spacecompass

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpaceCompassInfoPage(modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(spaceCompassPageContentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SpaceCompassSettingsIsland {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = Color(0xFF161334)) {
                    Image(painterResource(R.drawable.ic_space_compass), null, Modifier.size(80.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Space Compass V. ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Text(stringResource(R.string.pc_info_description), fontSize = 14.sp, lineHeight = 21.sp)
            Text(stringResource(R.string.pc_info_earth), fontSize = 14.sp, lineHeight = 21.sp)
        }
        SpaceCompassSettingsIsland(stringResource(R.string.github_repository_title)) {
            Text(
                "github.com/mondiversi/SpaceCompass",
                modifier = Modifier.clickable(role = Role.Button) {
                    uriHandler.openUri("https://github.com/mondiversi/SpaceCompass")
                },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            Text(BuildConfig.APPLICATION_ID, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SpaceCompassSettingsIsland(stringResource(R.string.pc_info_credits)) {
            Text(stringResource(R.string.about_copyright), fontSize = 13.sp)
            Text("GPL-3.0 · Astronomy Engine (MIT) · SGP4", fontSize = 13.sp)
            Text("NASA · JPL · USGS · Solar System Scope / INOVE", fontSize = 13.sp)
            Text(stringResource(R.string.pc_info_credits_detail), fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
