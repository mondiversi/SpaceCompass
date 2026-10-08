package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogRefreshNoticeTest {
    @Test fun openingTheCatalogAndAutomaticUpdatesDoNotProduceAManualCompletionNotice() {
        val notice = SpaceCompassCatalogRefreshNotice()
        assertFalse(notice.consumeIfReady(0, false, false))
        assertFalse(notice.consumeIfReady(3, false, false))
    }

    @Test fun localRecalculationAndAnIdleGapBeforeTheRemotePassDoNotCompleteTheRequest() {
        val notice = SpaceCompassCatalogRefreshNotice()
        notice.requested(2)
        assertFalse(notice.consumeIfReady(2, false, false))
        assertFalse(notice.consumeIfReady(2, false, true))
        assertFalse(notice.consumeIfReady(2, true, false))
    }

    @Test fun theRemotePassAndTheFinalLocalRowsMustBothFinish() {
        val notice = SpaceCompassCatalogRefreshNotice()
        notice.requested(2)
        assertFalse(notice.consumeIfReady(3, true, false))
        assertFalse(notice.consumeIfReady(3, false, true))
        assertTrue(notice.consumeIfReady(3, false, false))
        assertFalse(notice.consumeIfReady(3, false, false))
        assertFalse(notice.consumeIfReady(4, false, false))
    }

    @Test fun cancellationWithoutACompletedRemotePassDoesNotProduceANotice() {
        val notice = SpaceCompassCatalogRefreshNotice()
        notice.requested(5)
        assertFalse(notice.consumeIfReady(5, true, false))
        assertFalse(notice.consumeIfReady(5, false, false))
        assertTrue(notice.consumeIfReady(6, false, false))
    }

    @Test fun aSecondRequestHasItsOwnSingleAcknowledgementAndCoalescedPassesAreAccepted() {
        val notice = SpaceCompassCatalogRefreshNotice()
        notice.requested(0)
        assertTrue(notice.consumeIfReady(1, false, false))
        notice.requested(1)
        assertFalse(notice.consumeIfReady(1, false, false))
        assertTrue(notice.consumeIfReady(3, false, false))
        assertFalse(notice.consumeIfReady(3, false, false))
    }
}
