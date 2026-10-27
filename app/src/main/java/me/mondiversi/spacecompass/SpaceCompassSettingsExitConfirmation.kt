package me.mondiversi.spacecompass

/** A settings visit is confirmed once, after final editor disposal has flushed its saved values. */
internal class SpaceCompassSettingsExitConfirmation(
    var page: String? = null,
    var baseline: Map<String, String> = emptyMap()
) {
    private var rejectedExit = false
    fun rejectExit() { rejectedExit = true }

    fun moveTo(nextPage: String?, savedValues: Map<String, String>): Boolean {
        if (page == nextPage) return false
        val announce = page != null && !rejectedExit && baseline != savedValues
        page = nextPage
        baseline = savedValues.toMap()
        rejectedExit = false
        return announce
    }
}
