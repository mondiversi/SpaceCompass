package me.mondiversi.spacecompass

/** A manual request is acknowledged once, after its remote pass and local rows are ready. */
internal class SpaceCompassCatalogRefreshNotice {
    private var completedBeforeRequest: Int? = null

    fun requested(completedRevision: Int) {
        completedBeforeRequest = completedRevision
    }

    fun consumeIfReady(completedRevision: Int, remoteBusy: Boolean, calculating: Boolean): Boolean {
        val previous = completedBeforeRequest ?: return false
        if (completedRevision <= previous || remoteBusy || calculating) return false
        completedBeforeRequest = null
        return true
    }
}
