package me.mondiversi.planetcompass

import android.os.Bundle
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
        PlanetCompassErrorLog.install(applicationContext)
        enableEdgeToEdge()
        setContent {
            val dark = isSystemInDarkTheme()
            val background = if (dark) Color(0xFF101418) else Color(0xFFF5F7F8)
            val primary = if (dark) Color.White else Color(0xFF101418)
            val secondary = primary.copy(alpha = 0.72f)
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                PlanetCompassAdaptiveDisplay {
                    PlanetCompassSystemBarsContent(background) {
                        PlanetCompassSunFinderScreen(background, primary, secondary) { finish() }
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
