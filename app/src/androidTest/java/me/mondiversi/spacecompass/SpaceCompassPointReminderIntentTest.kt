package me.mondiversi.spacecompass

import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Construct handoff intents only. These checks never launch a receiving app or
 * create an alarm, calendar entry, invitation or background task. */
@RunWith(AndroidJUnit4::class)
class SpaceCompassPointReminderIntentTest {
    @Test fun clockReceivesOneNonRepeatingAlarmWithItsNormalUi() {
        val request=SpaceCompassPointReminder(SpaceCompassPointReminderKind.ALARM,1_800_000_000_000,
            "Europe/Rome",16,45)
        val intent=spaceCompassPointReminderIntent(request,"Luna · Sorge")
        assertEquals(AlarmClock.ACTION_SET_ALARM,intent.action)
        assertEquals(16,intent.getIntExtra(AlarmClock.EXTRA_HOUR,-1))
        assertEquals(45,intent.getIntExtra(AlarmClock.EXTRA_MINUTES,-1))
        assertEquals("Luna · Sorge",intent.getStringExtra(AlarmClock.EXTRA_MESSAGE))
        assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI,true))
        assertFalse(intent.hasExtra(AlarmClock.EXTRA_DAYS))
        assertEquals(4,intent.extras!!.size())
    }
    @Test fun calendarOpensAnEditorWithAnExactDateAndNoCoordinatesOrInvitations() {
        val request=SpaceCompassPointReminder(SpaceCompassPointReminderKind.CALENDAR,1_900_000_000_123,
            "Asia/Kathmandu",4,35)
        val intent=spaceCompassPointReminderIntent(request,"Saturno · Culmine")
        assertEquals(Intent.ACTION_INSERT,intent.action)
        assertEquals(CalendarContract.Events.CONTENT_URI,intent.data)
        assertEquals("vnd.android.cursor.dir/event",intent.type)
        assertEquals(request.eventTimeMs,intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,0))
        assertEquals(request.eventTimeMs+60_000,intent.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME,0))
        assertEquals(request.deviceZoneId,intent.getStringExtra(CalendarContract.Events.EVENT_TIMEZONE))
        assertFalse(intent.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY,true))
        assertFalse(intent.hasExtra(CalendarContract.Events.EVENT_LOCATION))
        assertFalse(intent.hasExtra(Intent.EXTRA_EMAIL))
    }
}
