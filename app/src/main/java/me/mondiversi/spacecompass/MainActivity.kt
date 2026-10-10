package me.mondiversi.spacecompass

import android.os.Bundle
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Independent launcher: no hardware pairing, UVIR archive or hidden entry gesture. */
class MainActivity : ComponentActivity() {
    private lateinit var screensaver: SpaceCompassScreensaverState
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpaceCompassErrorLog.install(applicationContext)
        screensaver = androidx.lifecycle.ViewModelProvider(this)[SpaceCompassScreensaverState::class.java]
        screensaver.bind(getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, MODE_PRIVATE))
        enableEdgeToEdge()
        SpaceCompassUpdates.start(applicationContext, freshLaunch = savedInstanceState == null)
        setContent {
            SpaceCompassPreferences {
                val dark = isSystemInDarkTheme()
                val background = if (dark) Color(0xFF101418) else Color(0xFFF5F7F8)
                val primary = if (dark) Color.White else Color(0xFF101418)
                val secondary = primary.copy(alpha = 0.72f)
                SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
                    // Modern edge-to-edge windows draw the Compose background behind navigation.
                    if (android.os.Build.VERSION.SDK_INT < 35) {
                        window.navigationBarColor = background.toArgb()
                    }
                }
                MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF55C8DB)) else lightColorScheme(primary = Color(0xFF176B88))) {
                    SpaceCompassSystemBarsContent(background) {
                        SpaceCompassAdaptiveDisplay {
                            SpaceCompassLaunchGate(showLaunchScreen = savedInstanceState == null) {
                                SpaceCompassScreensaverHost(screensaver) {
                                    SpaceCompassAppPages {
                                        SpaceCompassSunFinderScreen(background, primary, secondary) { finish() }
                                    }
                                    SpaceCompassUpdateHost()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        screensaver.resumed(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        screensaver.resumed(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onStop() {
        if (!isChangingConfigurations) screensaver.reset()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (::screensaver.isInitialized) screensaver.focused(hasFocus)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::screensaver.isInitialized) screensaver.interaction()
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (::screensaver.isInitialized) when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> screensaver.touching(true)
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> screensaver.touching(false)
            else -> screensaver.interaction()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        if (::screensaver.isInitialized && event.actionMasked == android.view.MotionEvent.ACTION_SCROLL) screensaver.interaction()
        return super.dispatchGenericMotionEvent(event)
    }
}
