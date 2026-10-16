package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Accelerated hours must not cancel/restart an online request every animation frame. */
@Composable
internal fun rememberSpaceCompassScenarioRequestTime(plan: SpaceCompassObserverPlan?, wallNow: Long): Long? {
    val accelerated = (plan?.scenarioClock?.settings?.multiplier ?: 1) > 1
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val saver = LocalSpaceCompassScreensaverActive.current
    val sampled by produceState(plan?.dataRequestTimeMs(wallNow), plan, accelerated, lifecycle, saver) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (accelerated && !saver) while (true) {
                value = plan?.dataRequestTimeMs(System.currentTimeMillis())
                delay(30_000)
            }
        }
    }
    return if (accelerated) sampled else plan?.dataRequestTimeMs(wallNow)
}
