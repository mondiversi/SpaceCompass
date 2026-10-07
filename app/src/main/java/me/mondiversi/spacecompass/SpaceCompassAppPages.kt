package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal val LocalSpaceCompassMainVisible = staticCompositionLocalOf { true }

internal val LocalSpaceCompassNavigate = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Keep the live compass composition and its data owners through full-screen navigation. */
@Composable
internal fun SpaceCompassAppPages(content: @Composable () -> Unit) {
    var route by rememberSaveable { mutableStateOf<String?>(null) }
    val holder = rememberSaveableStateHolder()
    val observer = rememberSpaceCompassObserverState()
    CompositionLocalProvider(LocalSpaceCompassNavigate provides { route = it },
        LocalSpaceCompassObserver provides observer, LocalSpaceCompassMainVisible provides (route == null)) {
        Box(Modifier.fillMaxSize()) {
            // Keep remember/effects/caches alive. An unplaced layer has no drawing,
            // touch targets or accessibility nodes behind the foreground page.
            Box(Modifier.fillMaxSize().layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    if (route == null) placeable.placeRelative(0, 0)
                }
            }) { holder.SaveableStateProvider("compass") { content() } }
            route?.let { SpaceCompassAppPage(it, onBack = { route = null }) }
        }
    }
}

@Composable
internal fun spaceCompassPageBackground() = if (isSystemInDarkTheme()) Color(0xFF101418) else Color(0xFFF5F7F8)

@Composable
internal fun spaceCompassSettingsCardColor() = if (isSystemInDarkTheme()) Color(0xFF282D33) else Color(0xFFE6E9EB)

@Composable
internal fun SpaceCompassSettingsButton() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val navigate = LocalSpaceCompassNavigate.current
    val card = spaceCompassSettingsCardColor()
    val foreground = MaterialTheme.colorScheme.onSurface
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("app-settings"),
            colors = IconButtonDefaults.iconButtonColors(contentColor = foreground)) {
            Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.pc_settings))
        }
        SpaceCompassAdaptiveDropdownMenu(expanded, { expanded = false }, containerColor = card,
            modifier = Modifier.testTag("app-settings-menu"), shape = RoundedCornerShape(16.dp)) {
            listOf("appearance" to R.string.settings_section_appearance, "language" to R.string.settings_section_language,
                "units" to R.string.settings_section_units, "observer" to R.string.observer_title, "info" to R.string.settings_section_info).forEachIndexed { index, (route, title) ->
                if (index > 0) HorizontalDivider(color = foreground.copy(alpha = .10f))
                DropdownMenuItem(text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpaceCompassSettingsMenuIcon(route, foreground)
                        Text(stringResource(title), color = foreground)
                    }
                },
                    modifier = Modifier.testTag("open-$route"),
                    onClick = { expanded = false; navigate(route) })
            }

        }
    }
}

@Composable
internal fun SpaceCompassAppPage(route: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val title = when (route) {
        "appearance" -> R.string.settings_section_appearance
        "language" -> R.string.settings_section_language
        "units" -> R.string.settings_section_units
        "observer" -> R.string.observer_title
        else -> R.string.pc_info
    }
    Surface(color = spaceCompassPageBackground(), contentColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxSize().testTag("page-$route")) {
        Column(Modifier.fillMaxSize()) {
            SpaceCompassPageToolbar(stringResource(title), onBack)
            val resourceLanguageTag = stringResource(R.string.settings_resource_language_tag)
            when (route) {
                "observer" -> SpaceCompassObserverPage(Modifier.weight(1f))
                "info" -> SpaceCompassInfoPage(Modifier.weight(1f))
                "language" -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(spaceCompassPageContentPadding)) {
                    SpaceCompassSettingsChoices(SpaceCompassSettingSpec(R.string.settings_section_language,
                        "language", "system", listOf("system" to stringResource(R.string.language_system)) + spaceCompassLanguages,
                        description = stringResource(R.string.language_description), examples = mapOf("system" to
                            (spaceCompassLanguages.firstOrNull { it.first == resourceLanguageTag } ?: spaceCompassLanguages.first()).second)), languageCodes = true)
                }
                else -> {
                    val settings = if (route == "appearance") spaceCompassAppearanceSettings() else spaceCompassUnitSettings()
                    SpaceCompassIslandGrid(Modifier.fillMaxWidth().weight(1f)) {
                        items(settings, key = { it.key }) { spec -> SpaceCompassSettingsChoices(spec) }
                    }
                }
            }
        }
    }
}
