package me.mondiversi.spacecompass

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SpaceCompassInfoPage(modifier: Modifier) {
    SpaceCompassIslandGrid(modifier) {
        item(key = "introduction") {
            SpaceCompassSettingsIsland {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFF161334)) {
                        Image(painterResource(R.drawable.ic_space_compass), null, Modifier.size(80.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Space Compass V. ${BuildConfig.DISPLAY_VERSION}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
                Text(stringResource(R.string.pc_info_description), fontSize = 13.sp, lineHeight = 18.sp)
                SpaceCompassSettingsDescription(stringResource(R.string.pc_info_earth))
            }
        }
        item(key = "news") {
            SpaceCompassSettingsIsland(stringResource(R.string.info_news_title, BuildConfig.DISPLAY_VERSION),
                iconKey = "whats_new") {
                listOf(R.string.info_news_star_sky, R.string.info_news_texture_downloads, R.string.info_news_weather,
                    R.string.info_news_viewer_zoom, R.string.info_news_settings, R.string.info_news_catalog).forEach { change ->
                    Text("• ${stringResource(change)}", fontSize = 13.sp, lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                }
            }
        }
        item(key = "repository") {
            SpaceCompassSettingsIsland(stringResource(R.string.github_repository_title), iconKey = "repository") {
                SpaceCompassRepositoryActions()
            }
        }
        item(key = "credits") {
            SpaceCompassSettingsIsland(stringResource(R.string.pc_info_credits), iconKey = "credits") {
                Text(stringResource(R.string.about_copyright), fontSize = 11.sp, lineHeight = 14.sp)
                Text("GPL-3.0 · Astronomy Engine (MIT) · SGP4 · Leaflet (BSD-2-Clause)", fontSize = 11.sp, lineHeight = 14.sp)
                Text("NASA · ESA · Hubble · ALMA (ESO/NAOJ/NRAO) · VTAD · JPL · USGS · Solar System Scope / INOVE", fontSize = 11.sp, lineHeight = 14.sp)
                Text("Deep Star Maps 2020 · NASA/Goddard SVS · ESA/Gaia/DPAC · Ernie Wright (USRA)", fontSize = 11.sp, lineHeight = 14.sp)
                Text(stringResource(R.string.pc_info_credits_detail), fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}
