package me.mondiversi.spacecompass

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

internal fun spaceCompassCelestialFavoriteColor(darkTheme: Boolean): Color =
    if (darkTheme) Color(0xFFF29ADE) else Color(0xFFB83291)

internal data class SpaceCompassCelestialFavoritesPreference(val bodies: Set<SpaceCompassCelestialBody>,
    val toggle: (SpaceCompassCelestialBody) -> Unit)

/** The retained catalogue and independently opened observatories observe the same stored favorites. */
@Composable
internal fun rememberSpaceCompassCelestialFavorites(): SpaceCompassCelestialFavoritesPreference {
    val context = LocalContext.current
    val preferences = LocalSpaceCompassPreferences.current ?: remember(context) {
        context.getSharedPreferences(SPACE_COMPASS_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    var favorites by remember(preferences) { mutableStateOf(readSpaceCompassCelestialFavorites(preferences)) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { saved, key ->
            if (key == null || key == SPACE_COMPASS_CELESTIAL_FAVORITES_KEY)
                favorites = readSpaceCompassCelestialFavorites(saved)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        favorites = readSpaceCompassCelestialFavorites(preferences)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return SpaceCompassCelestialFavoritesPreference(favorites) { body ->
        favorites = toggleSpaceCompassCelestialFavorite(preferences, body)
    }
}

@Composable
internal fun SpaceCompassCelestialFavoriteIcon(filled: Boolean) {
    Icon(painterResource(if (filled) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline),
        contentDescription = null, modifier = Modifier.size(SpaceCompassTitleActionIconSize))
}

/** Shared toolbar dimensions and stroke; preference changes also update retained observatories. */
@Composable
internal fun SpaceCompassCelestialFavoriteAction(body: SpaceCompassCelestialBody, enabled: Boolean = true) {
    val favorites = rememberSpaceCompassCelestialFavorites()
    val favorite = body in favorites.bodies
    SpaceCompassTitleActionButton(
        contentDescription = stringResource(if (favorite) R.string.celestial_favorite_remove else R.string.celestial_favorite_add),
        onClick = { favorites.toggle(body) }, modifier = Modifier.testTag("observatory-favorite"),
        enabled = enabled, selected = favorite,
        iconColor = if (favorite) spaceCompassCelestialFavoriteColor(isSystemInDarkTheme())
            else spaceCompassTitleActionColor()
    ) { SpaceCompassCelestialFavoriteIcon(favorite) }
}
