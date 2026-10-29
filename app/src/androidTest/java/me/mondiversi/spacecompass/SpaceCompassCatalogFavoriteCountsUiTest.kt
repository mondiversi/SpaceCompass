package me.mondiversi.spacecompass

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Isolated favorites verify the scope of displayed totals without provider calls or user preferences. */
class SpaceCompassCatalogFavoriteCountsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName = "test_favorite_counts_" + UUID.randomUUID()
    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val visible = mutableStateOf(true)

    private fun show(favorites: Set<SpaceCompassCelestialBody>) {
        preferences.edit().putStringSet(SPACE_COMPASS_CELESTIAL_FAVORITES_KEY, favorites.map { it.name }.toSet()).commit()
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences,
                LocalSpaceCompassNumericFormat provides SpaceCompassNumericFormat.INTERNATIONAL) {
                if (visible.value) SpaceCompassCelestialCatalogPage(1_790_000_000_000L,
                    SpaceCompassCelestialRemoteData(), setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON),
                    MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.background,
                    onBack = {}, onToggleAll = {}, onSelect = {})
            }
        } }
        compose.onNodeWithTag("catalog-filter-toggle").performClick()
    }

    @After fun dispose() {
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        context.deleteSharedPreferences(preferenceName)
    }

    private fun assertTypeCount(count: Int) {
        val expected = context.getString(R.string.catalog_filter_type) + ": " + context.getString(R.string.catalog_all) + " ($count)"
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("catalog-type-menu").fetchSemanticsNodes().any {
                it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(expected) == true
            }
        }
        compose.onNodeWithTag("catalog-type-menu").assertContentDescriptionEquals(expected)
    }

    @Test fun heartRestrictsCategoryTotalsAndDropdownChoicesWithoutChangingTheCatalogueTitle() {
        show(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MARS))
        val total = spaceCompassCelestialCatalogOrder.size
        assertTypeCount(total)
        compose.onNodeWithTag("catalog-favorites").performClick()
        assertTypeCount(2)
        compose.onNodeWithTag("catalog-title").assertTextEquals(context.getString(R.string.pc_celestial_object, total.toString()))
        compose.onNodeWithTag("catalog-type-menu").performClick()
        compose.onNodeWithTag("catalog-type-all").assertTextEquals(context.getString(R.string.catalog_all), "(2)")
        compose.onNodeWithTag("catalog-type-STAR").assertTextEquals(context.getString(R.string.catalog_type_star), "(1)")
        compose.onNodeWithTag("catalog-type-NATURAL_SATELLITE").performScrollTo()
            .assertTextEquals(context.getString(R.string.catalog_type_moon), "(1)")
    }

    @Test fun emptyAndChangedFavoritesUpdateCountsWhileThePanelRemainsOpen() {
        show(emptySet())
        compose.onNodeWithTag("catalog-favorites").performClick()
        assertTypeCount(0)
        compose.onNodeWithText(context.getString(R.string.catalog_no_results)).assertExists()
        compose.runOnIdle { preferences.edit().putStringSet(SPACE_COMPASS_CELESTIAL_FAVORITES_KEY, setOf("SUN")).apply() }
        assertTypeCount(1)
        compose.onNodeWithTag("catalog-favorites").performClick()
        assertTypeCount(spaceCompassCelestialCatalogOrder.size)
    }
}
