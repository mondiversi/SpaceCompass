package me.mondiversi.spacecompass

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPlaceRepositoryTest {
    private val key = spaceCompassPlaceKey(28.05111096, -14.351552, "it-IT")!!
    private class Store {
        val values = mutableMapOf<String, String>()
        var now = 1_000_000L
        var elapsed = 0L
        var calls = 0
        var result: String? = "Morro Jable, Canarias, Spagna"
        fun repository(fetch: (suspend (SpaceCompassPlaceKey) -> String?)? = null) = SpaceCompassPlaceRepository(
            read = { SpaceCompassSavedPlace.decode(values[spaceCompassPlaceStorageKey(it)]) },
            write = { key, value -> values[spaceCompassPlaceStorageKey(key)] = value.encode() },
            wall = { now }, elapsed = { elapsed }, fetch = fetch ?: { calls++; result })
    }

    @Test fun aKnownNameSurvivesRepositoryRecreationWithoutAnotherProviderRequest() = runBlocking {
        val store = Store()
        val first = store.repository(); first.resolve(key)
        val reopened = store.repository()
        assertEquals(store.result, reopened.cached(key))
        assertFalse(reopened.needsLookup(key))
        assertEquals(store.result, reopened.resolve(key).text)
        assertEquals(1, store.calls)
    }

    @Test fun repeatedPopupAndEditorRequestsShareOneSuccessfulLookup() = runBlocking {
        val store = Store(); val repository = store.repository()
        repeat(5) { assertEquals(store.result, repository.resolve(key).text) }
        store.elapsed += SPACE_COMPASS_PLACE_SUCCESS_TTL_MS
        repository.resolve(key) // Memory expiry reloads the persisted name, not the provider.
        assertEquals(1, store.calls)
    }

    @Test fun locationsAndLanguagesNeverBorrowAnotherPlacesLabel() = runBlocking {
        val store = Store(); val repository = store.repository(); repository.resolve(key)
        val english = key.copy(languageTag = "en-US")
        val elsewhere = key.copy(latitude = 45.0)
        assertNull(repository.cached(english)); assertNull(repository.cached(elsewhere))
        store.result = "Morro Jable, Canary Islands, Spain"; repository.resolve(english)
        assertEquals("Morro Jable, Canarias, Spagna", repository.cached(key))
        assertEquals(store.result, repository.cached(english))
        assertNull(repository.cached(elsewhere)); assertEquals(2, store.calls)
    }

    @Test fun anOldSavedNameRemainsImmediatelyReadableDuringARefresh() = runBlocking {
        val store = Store(); store.repository().seed(key, "Nome precedente")
        store.now += SPACE_COMPASS_SAVED_PLACE_TTL_MS
        val reopened = store.repository()
        assertEquals("Nome precedente", reopened.cached(key))
        assertTrue(reopened.needsLookup(key))
        assertEquals(store.result, reopened.resolve(key).text)
        assertEquals(1, store.calls)
    }

    @Test fun aFailedRefreshDoesNotEraseASavedName() = runBlocking {
        val store = Store(); store.repository().seed(key, "Morro Jable")
        store.now += SPACE_COMPASS_SAVED_PLACE_TTL_MS; store.result = null
        val reopened = store.repository()
        assertEquals("Morro Jable", reopened.resolve(key).text)
        assertEquals("Morro Jable", store.repository().cached(key))
    }

    @Test fun unavailableResultsAreRetriedLaterAndNeverPersistedAsPlaceNames() = runBlocking {
        val store = Store(); store.result = null
        val repository = store.repository()
        assertNull(repository.resolve(key).text); assertFalse(repository.needsLookup(key))
        repository.resolve(key); assertEquals(1, store.calls); assertTrue(store.values.isEmpty())
        store.elapsed += SPACE_COMPASS_PLACE_RETRY_MS
        store.result = "Morro Jable"; assertEquals("Morro Jable", repository.resolve(key).text)
        assertEquals(2, store.calls)
    }

    @Test fun concurrentRequestsForTheSameCoordinatesAreCoalesced() = runBlocking {
        var calls = 0
        val store = Store()
        val repository = store.repository { calls++; delay(20); "Morro Jable" }
        val editor = async { repository.resolve(key).text }
        val summary = async { repository.resolve(key).text }
        assertEquals("Morro Jable", editor.await()); assertEquals("Morro Jable", summary.await())
        assertEquals(1, calls)
    }

    @Test fun aSearchResultSeedsBothMemoryAndPersistentStorageWithoutReverseGeocoding() = runBlocking {
        val store = Store(); val repository = store.repository()
        repository.seed(key, " Morro\nJable, Canarias, Spagna ")
        assertEquals("Morro Jable, Canarias, Spagna", repository.resolve(key).text)
        assertEquals("Morro Jable, Canarias, Spagna", store.repository().cached(key))
        assertEquals(0, store.calls)
    }

    @Test fun cancellingALookupDoesNotSaveAFailureOrBlockTheNextRequest() = runBlocking {
        val store = Store(); var cancel = true
        val repository = store.repository { if (cancel) throw CancellationException("fixture") else "Morro Jable" }
        try { repository.resolve(key); fail("Cancellation must propagate") } catch (_: CancellationException) { }
        assertTrue(store.values.isEmpty()); assertTrue(repository.needsLookup(key))
        cancel = false; assertEquals("Morro Jable", repository.resolve(key).text)
    }

    @Test fun malformedRecordsAndClockChangesCannotTurnIntoFreshLocationLabels() {
        for (bad in listOf(null, "", "not-a-time\nRoma", "0\nRoma", "1000\n", "1000\nRoma\nVia Roma", "1000\n" + "x".repeat(513)))
            assertNull(SpaceCompassSavedPlace.decode(bad))
        val saved = SpaceCompassSavedPlace("القاهرة, مصر", 1000L)
        assertEquals(saved, SpaceCompassSavedPlace.decode(saved.encode()))
        assertFalse(saved.fresh(999L)); assertTrue(saved.fresh(1000L))
        assertFalse(saved.fresh(1000L + SPACE_COMPASS_SAVED_PLACE_TTL_MS))
        assertNotEquals(spaceCompassPlaceStorageKey(key), spaceCompassPlaceStorageKey(key.copy(languageTag = "en")))
    }

    @Test fun aNameSeededWhileAProviderRequestIsRunningIsNotOverwrittenByItsFailure() = runBlocking {
        val store = Store(); val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val repository = store.repository { started.complete(Unit); finish.await(); null }
        val request = async { repository.resolve(key).text }
        started.await(); repository.seed(key, "Morro Jable"); finish.complete(Unit)
        assertEquals("Morro Jable", request.await())
        assertEquals("Morro Jable", store.repository().cached(key))
    }
}
