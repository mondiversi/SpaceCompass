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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpaceCompassErrorLog.install(applicationContext)
        enableEdgeToEdge()
        if (savedInstanceState == null) SpaceCompassUpdates.check(applicationContext, manual = false)
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

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }
}
