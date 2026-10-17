package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Accelerated hours must not cancel/restart an online request every animation frame. */
@Composable
internal fun rememberSpaceCompassScenarioRequestTime(plan: SpaceCompassObserverPlan?, wallNow: Long): Long? {
    val accelerated = plan?.scenarioClock?.settings?.accelerated == true
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val saver = LocalSpaceCompassScreensaverActive.current
    // Rate/direction changes replace the runtime clock without changing saved plan equality.
    val sampled by produceState(plan?.dataRequestTimeMs(wallNow), plan, plan?.scenarioClock, accelerated, lifecycle, saver) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (accelerated && !saver) while (true) {
                value = plan?.dataRequestTimeMs(System.currentTimeMillis())
                delay(30_000)
            }
        }
    }
    return if (accelerated) sampled else plan?.dataRequestTimeMs(wallNow)
}
