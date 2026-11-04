package me.mondiversi.spacecompass

import android.content.SharedPreferences

internal const val SPACE_COMPASS_RETICLE_VISIBLE_KEY = "sky_reticle_visible"
internal const val SPACE_COMPASS_RETICLE_VISIBLE_DEFAULT = true
internal const val SPACE_COMPASS_SKY_CONTROLS_VISIBLE_KEY = "sky_controls_visible"

internal fun readSpaceCompassReticleVisible(preferences: SharedPreferences?): Boolean =
    preferences?.getBoolean(SPACE_COMPASS_RETICLE_VISIBLE_KEY, SPACE_COMPASS_RETICLE_VISIBLE_DEFAULT)
        ?: SPACE_COMPASS_RETICLE_VISIBLE_DEFAULT

internal fun saveSpaceCompassReticleVisible(preferences: SharedPreferences?, visible: Boolean) {
    preferences?.edit()?.putBoolean(SPACE_COMPASS_RETICLE_VISIBLE_KEY, visible)?.apply()
}
