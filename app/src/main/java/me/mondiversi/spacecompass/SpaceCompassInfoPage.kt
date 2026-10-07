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
                Text(stringResource(R.string.pc_info_description), fontSize = 14.sp, lineHeight = 21.sp)
                Text(stringResource(R.string.pc_info_earth), fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        item(key = "news") {
            SpaceCompassSettingsIsland(stringResource(R.string.info_news_title, BuildConfig.DISPLAY_VERSION),
                iconKey = "whats_new") {
                listOf(R.string.info_news_camera_zoom, R.string.info_news_catalog_search, R.string.info_news_capture_metadata,
                    R.string.info_news_capture_controls, R.string.info_news_viewer, R.string.info_news_performance).forEach { change ->
                    Text("• ${stringResource(change)}", fontSize = 13.sp, lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .75f))
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
                Text(stringResource(R.string.about_copyright), fontSize = 13.sp)
                Text("GPL-3.0 · Astronomy Engine (MIT) · SGP4 · Leaflet (BSD-2-Clause)", fontSize = 13.sp)
                Text("NASA · JPL · USGS · Solar System Scope / INOVE", fontSize = 13.sp)
                Text(stringResource(R.string.pc_info_credits_detail), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}
