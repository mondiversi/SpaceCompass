package me.mondiversi.spacecompass

internal const val SPACE_COMPASS_MAIN_REMINDER_BUTTON_KEY = "main_reminder_button"

/** Presentation only: daily-path controls and reminder eligibility remain independent. */
internal fun spaceCompassMainReminderButtonVisible(settings: Map<String, String?>): Boolean =
    settings[SPACE_COMPASS_MAIN_REMINDER_BUTTON_KEY] != "false"
