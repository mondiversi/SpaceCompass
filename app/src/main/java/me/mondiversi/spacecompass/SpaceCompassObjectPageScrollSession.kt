package me.mondiversi.spacecompass

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.collect

internal enum class SpaceCompassObjectPage { OBSERVATORY, DAILY_PATH }

internal class SpaceCompassObjectPageScrollEntry {
    val scroll = ScrollState(0)
    val position = SpaceCompassObjectPageScrollPosition()
    var headerVisible = true
    var visited = false
    suspend fun scrollTo(offset: Int) {
        position.request(offset)
        scroll.scrollTo(offset)
    }
}

internal data class SpaceCompassObjectPageScrollVisit(val entry: SpaceCompassObjectPageScrollEntry,
    val returning: Boolean)

/** Lazily allocated and confined to the activity session: no preferences, archive or saved state. */
internal class SpaceCompassObjectPageScrollSession {
    private val entries = mutableMapOf<Pair<SpaceCompassCelestialBody, SpaceCompassObjectPage>, SpaceCompassObjectPageScrollEntry>()
    fun open(body: SpaceCompassCelestialBody, page: SpaceCompassObjectPage): SpaceCompassObjectPageScrollVisit {
        val entry = entries.getOrPut(body to page) { SpaceCompassObjectPageScrollEntry() }
        return SpaceCompassObjectPageScrollVisit(entry, entry.visited).also { entry.visited = true }
    }
}

internal class SpaceCompassObjectPageScrollViewModel : ViewModel() {
    val session = SpaceCompassObjectPageScrollSession()
}

internal val LocalSpaceCompassObjectPageScrollSession = staticCompositionLocalOf<SpaceCompassObjectPageScrollSession?> { null }

/** Retained through rotation and normal backgrounding; finishing the activity ends this session. */
@Composable
internal fun rememberSpaceCompassObjectPageScrollSession(): SpaceCompassObjectPageScrollSession {
    val owner = LocalActivity.current as? ViewModelStoreOwner
    return remember(owner) {
        if (owner == null) SpaceCompassObjectPageScrollSession()
        else ViewModelProvider(owner)[SpaceCompassObjectPageScrollViewModel::class.java].session
    }
}

@Composable
internal fun rememberSpaceCompassObjectPageScroll(body: SpaceCompassCelestialBody,
    page: SpaceCompassObjectPage): SpaceCompassObjectPageScrollVisit {
    val fallback = remember { SpaceCompassObjectPageScrollSession() }
    val session = LocalSpaceCompassObjectPageScrollSession.current ?: fallback
    val visit = remember(session, body, page) { session.open(body, page) }
    val entry = visit.entry
    LaunchedEffect(entry) {
        snapshotFlow { Triple(entry.scroll.value, entry.scroll.maxValue, entry.scroll.isScrollInProgress) }
            .collect { (value, maximum, scrolling) ->
                entry.position.observe(value, maximum, scrolling)?.let { entry.scroll.scrollTo(it) }
            }
    }
    DisposableEffect(entry) {
        onDispose { entry.position.observe(entry.scroll.value, entry.scroll.maxValue, entry.scroll.isScrollInProgress) }
    }
    return visit
}
