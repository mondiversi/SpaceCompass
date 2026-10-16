package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

/** No preference writes: the system preview must stay independent of the forced app theme. */
class SpaceCompassDefaultThemeExampleUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun oppositeAppThemeDoesNotChangeTheDeviceDefaultExample() {
        val deviceDark = mutableStateOf(true)
        var expected = ""
        compose.setContent {
            val source = LocalConfiguration.current
            val opposite = Configuration(source).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (deviceDark.value) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
            }
            CompositionLocalProvider(LocalConfiguration provides opposite,
                LocalSpaceCompassDeviceDarkTheme provides deviceDark.value) {
                MaterialTheme {
                    expected = stringResource(if (deviceDark.value) R.string.settings_theme_dark else R.string.settings_theme_light)
                    Text(spaceCompassAppearanceSettings().first().examples.getValue("system"),
                        Modifier.testTag("device-theme-example"))
                }
            }
        }
        compose.onNodeWithTag("device-theme-example").assertTextEquals(expected)
        compose.runOnIdle { deviceDark.value = false }
        compose.waitForIdle()
        compose.onNodeWithTag("device-theme-example").assertTextEquals(expected)
    }
}
