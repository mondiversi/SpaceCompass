package me.mondiversi.spacecompass

import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** Observe route changes only in the foreground; no probes, GPS or background downloads. */
@Composable
internal fun rememberSpaceCompassNetworkRevision(resumed: Boolean, onChanged: () -> Unit): Int {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    val changed by rememberUpdatedState(onChanged)
    DisposableEffect(context, resumed) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val main = Handler(Looper.getMainLooper())
        var previous = manager.activeNetwork
        var active = resumed
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                main.post {
                    if (active && previous != network) {
                        previous = network
                        changed()
                        revision++
                    }
                }
            }
        }
        if (resumed) manager.registerDefaultNetworkCallback(callback)
        onDispose {
            active = false
            if (resumed) manager.unregisterNetworkCallback(callback)
        }
    }
    return revision
}
