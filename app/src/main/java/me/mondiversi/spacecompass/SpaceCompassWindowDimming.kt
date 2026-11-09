package me.mondiversi.spacecompass

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/** Dim the complete underlying window, including system-bar backgrounds, below a native popup. */
@Composable
internal fun SpaceCompassPopupWindowDimming() {
    val view = LocalView.current
    val dimmer = remember(view) { SpaceCompassPopupWindowDimmer(view) }
    DisposableEffect(dimmer) {
        dimmer.attach()
        onDispose { dimmer.release() }
    }
    SideEffect { dimmer.schedule() }
}

private class SpaceCompassPopupWindowDimmer(private val view: View) : View.OnAttachStateChangeListener {
    private var active = false
    private var root: View? = null
    private var originalDimFlag = 0
    private var originalAmount = 0f
    private val apply = Runnable { applyDimming() }

    fun attach() {
        active = true
        view.addOnAttachStateChangeListener(this)
        schedule()
    }

    fun schedule() {
        if (active && view.isAttachedToWindow) {
            view.removeCallbacks(apply)
            view.post(apply)
        }
    }

    override fun onViewAttachedToWindow(v: View) = schedule()
    override fun onViewDetachedFromWindow(v: View) = Unit

    private fun applyDimming() {
        if (!active || !view.isAttachedToWindow) return
        val target = view.rootView
        val params = target.layoutParams as? WindowManager.LayoutParams ?: return
        if (root !== target) {
            root = target
            originalDimFlag = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND
            originalAmount = params.dimAmount
        }
        if (params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0 && params.dimAmount == .18f) return
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        params.dimAmount = .18f
        val manager = target.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.updateViewLayout(target, params)
    }

    fun release() {
        active = false
        view.removeCallbacks(apply)
        view.removeOnAttachStateChangeListener(this)
        val target = root ?: return
        val params = target.layoutParams as? WindowManager.LayoutParams ?: return
        params.flags = (params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()) or originalDimFlag
        params.dimAmount = originalAmount
        if (target.isAttachedToWindow) {
            val manager = target.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            manager.updateViewLayout(target, params)
        }
        root = null
    }
}

/** Transparent dialog bars reveal the already dimmed activity instead of repainting bright strips. */
@Composable
internal fun SpaceCompassDialogSystemBars() {
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    val window = findSpaceCompassDialogWindow(view) ?: return
    SideEffect {
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 28) window.navigationBarDividerColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private fun findSpaceCompassDialogWindow(view: View): Window? {
    if (view is DialogWindowProvider) return view.window
    var parent = view.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    return null
}
