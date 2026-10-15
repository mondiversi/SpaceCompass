package me.mondiversi.spacecompass

import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import java.time.LocalTime

internal val LocalSpaceCompassScreensaverActive = staticCompositionLocalOf { false }
internal val LocalSpaceCompassScreensaverSettings = staticCompositionLocalOf { SpaceCompassScreensaverSettings() }
internal val LocalSpaceCompassScreensaverSolarObserver = staticCompositionLocalOf<(SpaceCompassScreensaverSolarObserver?, SpaceCompassObserverPlan?) -> Unit> { { _, _ -> } }

private data class SpaceCompassScreensaverScene(val lightHour: Double, val season: SpaceCompassScreensaverSeason)

private fun currentScreensaverScene(state: SpaceCompassScreensaverState): SpaceCompassScreensaverScene {
    val now = System.currentTimeMillis()
    return SpaceCompassScreensaverScene(
        spaceCompassScreensaverLightHour(state.solarObserver?.elevation(now), LocalTime.now().toSecondOfDay() / 3600.0),
        spaceCompassScreensaverSeason(now, state.observerPlan))
}

/** Retain the current page and scroll state beneath a non-click-through cover. */
@Composable
internal fun SpaceCompassScreensaverHost(state: SpaceCompassScreensaverState, content: @Composable () -> Unit) {
    val visible = state.visible
    val parentOwner = LocalLifecycleOwner.current
    val pageOwner = remember(parentOwner) { SpaceCompassScreensaverContentLifecycle(parentOwner.lifecycle, visible) }
    DisposableEffect(pageOwner) {
        pageOwner.attach()
        onDispose { pageOwner.release() }
    }
    SideEffect { pageOwner.setPaused(visible) }
    val initialScene = remember(state) { currentScreensaverScene(state) }
    val scene by produceState(initialScene, state, visible, state.observerPlan) {
        value = currentScreensaverScene(state)
        if (visible) while (true) {
            delay(60_000L)
            value = currentScreensaverScene(state)
        }
    }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    SideEffect { state.keyboard(keyboardVisible) }
    DisposableEffect(state) {
        state.presented(true)
        onDispose { state.presented(false) }
    }
    CompositionLocalProvider(LocalSpaceCompassScreensaverActive provides visible,
        LocalSpaceCompassScreensaverSettings provides state.settings,
        LocalSpaceCompassScreensaverSolarObserver provides state::solarObserver) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().layout { measurable, constraints ->
                val child = measurable.measure(constraints)
                layout(child.width, child.height) { if (!visible) child.placeRelative(0, 0) }
            }) {
                CompositionLocalProvider(LocalLifecycleOwner provides pageOwner) { content() }
            }
            if (visible) {
                BackHandler { state.dismiss() }
                SpaceCompassScreensaverLayer(isSystemInDarkTheme(), scene.lightHour, scene.season,
                    stringResource(R.string.screensaver_dismiss), state.sceneStartedAt, state.location, state::dismiss)
            }
        }
    }
}

/** Mount outside Compose's canvas, like the native position map, for legacy WebView compatibility. */
@Composable
private fun SpaceCompassScreensaverLayer(dark: Boolean, lightHour: Double, season: SpaceCompassScreensaverSeason, label: String,
    startedAt: Long, location: SpaceCompassScreensaverLocation?, onDismiss: () -> Unit) {
    val activity = LocalActivity.current ?: return
    val root = remember(activity) { activity.findViewById<ViewGroup>(android.R.id.content) }
    val callback = rememberUpdatedState(onDismiss)
    val info = rememberSpaceCompassScreensaverInfo()
    val view = remember(activity) {
        SpaceCompassScreensaverView(activity, dark, lightHour,
            (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L) / 1000.0, location, season) { callback.value() }
    }
    DisposableEffect(view, root) {
        val immersive = SpaceCompassScreensaverWindow(activity.window)
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        immersive.enter()
        androidx.core.view.ViewCompat.requestApplyInsets(view)
        onDispose { root.removeView(view); view.release(); immersive.restore() }
    }
    SideEffect {
        view.contentDescription = label
        view.updateHint(label)
        view.updateLighting(dark, lightHour)
        view.updateLocation(location)
        view.updateSeason(season)
        view.updateInfo(info)
    }
}
