package me.mondiversi.spacecompass

import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import java.time.LocalTime

internal val LocalSpaceCompassScreensaverActive = staticCompositionLocalOf { false }
internal val LocalSpaceCompassScreensaverSettings = staticCompositionLocalOf { SpaceCompassScreensaverSettings() }
internal val LocalSpaceCompassScreensaverSunElevation = staticCompositionLocalOf<(Double?) -> Unit> { {} }

private fun currentScreensaverLightHour(state: SpaceCompassScreensaverState): Double =
    spaceCompassScreensaverLightHour(state.sunElevationDegrees, LocalTime.now().toSecondOfDay() / 3600.0)

/** Retain the current page and scroll state beneath a non-click-through cover. */
@Composable
internal fun SpaceCompassScreensaverHost(state: SpaceCompassScreensaverState, content: @Composable () -> Unit) {
    val visible = state.visible
    val lightHour by produceState(currentScreensaverLightHour(state), state, visible) {
        if (visible) while (true) {
            value = currentScreensaverLightHour(state)
            delay(30_000L)
        }
    }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var bounds by remember { mutableStateOf(IntRect.Zero) }
    SideEffect { state.keyboard(keyboardVisible) }
    DisposableEffect(state) {
        state.presented(true)
        onDispose { state.presented(false) }
    }
    CompositionLocalProvider(LocalSpaceCompassScreensaverActive provides visible,
        LocalSpaceCompassScreensaverSettings provides state.settings,
        LocalSpaceCompassScreensaverSunElevation provides state::sunElevation) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            val rect = coordinates.boundsInWindow()
            bounds = IntRect(rect.left.roundToInt(), rect.top.roundToInt(), rect.right.roundToInt(), rect.bottom.roundToInt())
        }) {
            Box(Modifier.fillMaxSize().layout { measurable, constraints ->
                val child = measurable.measure(constraints)
                layout(child.width, child.height) { if (!visible) child.placeRelative(0, 0) }
            }) { content() }
            if (visible && bounds.width > 0 && bounds.height > 0) {
                BackHandler { state.dismiss() }
                SpaceCompassScreensaverLayer(bounds, isSystemInDarkTheme(), lightHour,
                    stringResource(R.string.screensaver_dismiss), state.sceneStartedAt, state::dismiss)
            }
        }
    }
}

/** Mount outside Compose's canvas, like the native position map, for legacy WebView compatibility. */
@Composable
private fun SpaceCompassScreensaverLayer(bounds: IntRect, dark: Boolean, lightHour: Double, label: String,
    startedAt: Long, onDismiss: () -> Unit) {
    val activity = LocalActivity.current ?: return
    val root = remember(activity) { activity.findViewById<ViewGroup>(android.R.id.content) }
    val callback = rememberUpdatedState(onDismiss)
    val view = remember(activity) {
        SpaceCompassScreensaverView(activity, dark, lightHour,
            (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L) / 1000.0) { callback.value() }
    }
    DisposableEffect(view, root) {
        root.addView(view)
        onDispose { root.removeView(view); view.release() }
    }
    SideEffect {
        val location = IntArray(2)
        root.getLocationInWindow(location)
        view.layoutParams = FrameLayout.LayoutParams(bounds.width, bounds.height).apply {
            leftMargin = bounds.left - location[0]
            topMargin = bounds.top - location[1]
        }
        view.contentDescription = label
        view.updateLighting(dark, lightHour)
    }
}
