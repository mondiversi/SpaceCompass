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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight

internal val LocalSpaceCompassMainVisible = staticCompositionLocalOf { true }

internal val LocalSpaceCompassNavigate = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Keep the live compass composition and its data owners through full-screen navigation. */
@Composable
internal fun SpaceCompassAppPages(content: @Composable () -> Unit) {
    val screensaverActive = LocalSpaceCompassScreensaverActive.current
    var route by rememberSaveable { mutableStateOf<String?>(null) }
    val holder = rememberSaveableStateHolder()
    val observer = rememberSpaceCompassObserverState()
    val objectScrollSession = rememberSpaceCompassObjectPageScrollSession()
    val orbitSelection = rememberSpaceCompassSunDailyPathUiState()
    val comparisons = rememberSpaceCompassTemporalComparisons(orbitSelection)
    val sceneTransfer = rememberSpaceCompassSceneTransfer(observer, comparisons, orbitSelection)
    val sceneExportVisible = sceneTransfer.exportPage != null
    val settingsConfirmation = rememberSpaceCompassSettingsExitNotice(route)
    val tutorial = LocalSpaceCompassTutorial.current
    LaunchedEffect(tutorial?.active) { if (tutorial?.active == true) route = null }
    CompositionLocalProvider(LocalSpaceCompassNavigate provides {
        when (it) {
            "tutorial" -> { route = null; tutorial?.start() }
            "scene-export" -> { route = null; sceneTransfer.openExport() }
            "scene-import" -> { route = null; sceneTransfer.openImport() }
            else -> route = it
        }
    },
        LocalSpaceCompassSettingsExitFailure provides settingsConfirmation::rejectExit,
        LocalSpaceCompassObserver provides observer, LocalSpaceCompassObjectPageScrollSession provides objectScrollSession,
        LocalSpaceCompassOrbitSelection provides orbitSelection, LocalSpaceCompassComparisons provides comparisons,
        LocalSpaceCompassMainVisible provides (route == null && !screensaverActive && !sceneExportVisible)) {
        SpaceCompassDropdownDimmingHost {
            // Keep remember/effects/caches alive. An unplaced layer has no drawing,
            // touch targets or accessibility nodes behind the foreground page.
            Box(Modifier.fillMaxSize().layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    if (route == null && !sceneExportVisible) placeable.placeRelative(0, 0)
                }
            }) { holder.SaveableStateProvider("compass") { content() } }
            route?.let { SpaceCompassAppPage(it, onBack = { route = null }) }
            sceneTransfer.exportPage?.invoke()
        }
    }
}

/** The full-face logo is a direct entry to the app's information and credits. */
@Composable
internal fun SpaceCompassMainInfoButton(color: Color, background: Color) {
    val navigate = LocalSpaceCompassNavigate.current
    SpaceCompassFloatingActionButton(stringResource(R.string.pc_info), { navigate("info") },
        color, background, Modifier.testTag("main-info").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.INFO),
        border = BorderStroke(2.dp, Color.White.copy(alpha = .35f))) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.ic_space_compass_button), contentDescription = null,
                modifier = Modifier.fillMaxSize())
            Text(BuildConfig.DISPLAY_VERSION,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
                    .testTag("main-logo-version"),
                color = Color.White, fontSize = 8.sp, lineHeight = 9.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
        }
    }
}

/** The familiar menu glyph shares the sky controls' circle, fitted ink and original stroke weight. */
@Composable
internal fun SpaceCompassSettingsButton(color: Color, background: Color) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val navigate = LocalSpaceCompassNavigate.current
    val tutorial = LocalSpaceCompassTutorial.current
    val tutorialActive = tutorial?.active == true
    LaunchedEffect(tutorialActive) { if (tutorialActive) expanded = false }
    val card = spaceCompassSettingsCardColor()
    val foreground = MaterialTheme.colorScheme.onSurface
    Box {
        SpaceCompassFloatingActionButton(stringResource(if (tutorialActive) R.string.close else R.string.pc_settings),
            { if (tutorialActive) tutorial?.close() else expanded = true },
            color, background, Modifier.testTag("app-settings").spaceCompassTutorialTarget(SpaceCompassTutorialFeature.MENU),
            glyphInk = if (tutorialActive) spaceCompassFloatingInk(26f, .24f, .24f, .76f, .76f)
                else spaceCompassFloatingInk(26f, 4f / 24f, .25f, 20f / 24f, .75f)) {
            val stroke = SpaceCompassFloatingIconStrokeWidth * LocalSpaceCompassFloatingStrokeScale.current
            SpaceCompassIconCanvas(color, Modifier.size(26.dp)) { ink ->
                if (tutorialActive) {
                    drawLine(ink, Offset(size.width * .24f, size.height * .24f),
                        Offset(size.width * .76f, size.height * .76f), stroke.toPx(), StrokeCap.Round)
                    drawLine(ink, Offset(size.width * .76f, size.height * .24f),
                        Offset(size.width * .24f, size.height * .76f), stroke.toPx(), StrokeCap.Round)
                } else {
                    drawSpaceCompassMenuGlyph(ink, stroke.toPx())
                }
            }
        }
        SpaceCompassAdaptiveDropdownMenu(expanded && !tutorialActive, { expanded = false }, containerColor = card,
            modifier = Modifier.testTag("app-settings-menu"), shape = RoundedCornerShape(16.dp)) {
            listOf("appearance" to R.string.settings_section_appearance, "language" to R.string.settings_section_language,
                "units" to R.string.settings_section_units, "scene-export" to R.string.scene_export,
                "scene-import" to R.string.scene_import, "tutorial" to R.string.tutorial_title, "info" to R.string.pc_info).forEachIndexed { index, (route, title) ->
                if (index > 0) HorizontalDivider(color = foreground.copy(alpha = .10f))
                SpaceCompassNavigationMenuItem(stringResource(title), { expanded = false; navigate(route) },
                    Modifier.testTag("open-$route")) { tint -> SpaceCompassSettingsMenuIcon(route, tint) }
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
        "observer" -> R.string.observer_management_title
        else -> R.string.pc_info
    }
    Surface(color = spaceCompassPageBackground(), contentColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxSize().testTag("page-$route")) {
        Column(Modifier.fillMaxSize()) {
            if (route != "observer") SpaceCompassPageToolbar(stringResource(title), onBack)
            val resourceLanguageTag = stringResource(R.string.settings_resource_language_tag)
            when (route) {
                "observer" -> SpaceCompassObserverPage(Modifier.weight(1f), onBack)
                "info" -> SpaceCompassInfoPage(Modifier.weight(1f))
                "language" -> {
                    val scroll = rememberScrollState()
                    Column(Modifier.fillMaxWidth().weight(1f)
                        .scrollbarOverlay(scroll, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .46f))
                        .verticalScroll(scroll).padding(spaceCompassPageContentPadding)) {
                        SpaceCompassSettingsChoices(SpaceCompassSettingSpec(R.string.settings_section_language,
                            "language", "system", listOf("system" to stringResource(R.string.language_system)) +
                                spaceCompassLanguages.sortedBy { it.first },
                            description = stringResource(R.string.language_description), examples = mapOf("system" to
                                (spaceCompassLanguages.firstOrNull { it.first == resourceLanguageTag } ?: spaceCompassLanguages.first()).second)), languageCodes = true)
                    }
                }
                else -> {
                    // Keep the Display definition and saved preference, but hide the unfinished island.
                    val settings = if (route == "appearance") spaceCompassAppearanceSettings().filterNot { it.key == "display" }
                        else spaceCompassUnitSettings()
                    SpaceCompassIslandGrid(Modifier.fillMaxWidth().weight(1f)) {
                        if (route == "appearance") {
                            // Each island keeps its natural height and participates in landscape packing.
                            item(key = "appearance-theme") { SpaceCompassSettingsChoices(settings.first()) }
                            item(key = "appearance-fluidity") { SpaceCompassSettingsChoices(spaceCompassSkyFluiditySettings()) }
                            item(key = "appearance-events") { SpaceCompassEventsIsland() }
                            item(key = "appearance-music") { SpaceCompassAmbientMusicIsland() }
                            item(key = "appearance-screensaver") { SpaceCompassScreensaverIsland() }
                            items(settings.drop(1), key = { it.key }) { spec -> SpaceCompassSettingsChoices(spec) }
                        } else {
                            items(settings, key = { it.key }) { spec -> SpaceCompassSettingsChoices(spec) }
                        }
                    }
                }
            }
        }
    }
}
