package me.mondiversi.spacecompass

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract

/** The receiving Clock/Calendar owns the reminder. No local alarms, jobs,
 * calendar writes, contacts, coordinates or attendee invitations are involved. */
internal fun spaceCompassPointReminderIntent(request: SpaceCompassPointReminder, title: String): Intent =
    when (request.kind) {
        SpaceCompassPointReminderKind.ALARM -> Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, request.alarmHour)
            .putExtra(AlarmClock.EXTRA_MINUTES, request.alarmMinute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, title)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        SpaceCompassPointReminderKind.CALENDAR -> Intent(Intent.ACTION_INSERT)
            .setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.dir/event")
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, request.eventTimeMs)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, request.eventTimeMs + 60_000)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
            .putExtra(CalendarContract.Events.EVENT_TIMEZONE, request.deviceZoneId)
    }

internal fun openSpaceCompassPointReminder(context: Context, request: SpaceCompassPointReminder, title: String) {
    val missing = if (request.kind == SpaceCompassPointReminderKind.ALARM)
        R.string.point_reminder_no_clock else R.string.point_reminder_no_calendar
    val intent = spaceCompassPointReminderIntent(request, title)
    if (intent.resolveActivity(context.packageManager) == null) {
        showSpaceCompassBottomMessage(context, context.getString(missing))
        return
    }
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(intent) }
    catch (_: ActivityNotFoundException) { showSpaceCompassBottomMessage(context, context.getString(missing)) }
    catch (_: SecurityException) { showSpaceCompassBottomMessage(context, context.getString(missing)) }
}
