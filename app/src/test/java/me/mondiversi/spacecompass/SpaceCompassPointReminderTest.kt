package me.mondiversi.spacecompass

import java.io.File
import java.time.Instant
import java.time.ZoneId
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPointReminderTest {
    private val utc = ZoneId.of("UTC")
    private fun ms(value: String) = Instant.parse(value).toEpochMilli()
    private val now = ms("2026-10-16T08:10:20Z")
    private fun plan(event: String, viewed: Long = now, real: Long = now,
        current: Boolean = false, zone: ZoneId = utc) = spaceCompassPointReminder(ms(event), viewed, real, current, zone)

    @Test fun currentIsNeverAnActionableFutureEventEvenInAFutureScenario() {
        assertNull(plan("2030-01-01T12:00:00Z", current = true))
    }
    @Test fun realPastAndPresentCannotScheduleTheNextDayByMistake() {
        assertNull(plan("2026-10-16T08:10:19Z"))
        assertNull(plan("2026-10-16T08:10:20Z"))
        assertNull(plan("2025-10-16T10:00:00Z", viewed = ms("2025-10-16T09:00:00Z")))
    }
    @Test fun pointsBeforeOrAtTheViewedMomentStayDisabledInFutureScenarios() {
        val future = ms("2030-01-01T12:00:00Z")
        assertNull(plan("2030-01-01T11:00:00Z", viewed = future))
        assertNull(plan("2030-01-01T12:00:00Z", viewed = future))
        assertEquals(SpaceCompassPointReminderKind.CALENDAR,
            plan("2030-01-01T13:00:00Z", viewed = future)!!.kind)
    }
    @Test fun aFuturePointTodayUsesTheDeviceClock() {
        val result = plan("2026-10-16T09:32:45Z")!!
        assertEquals(SpaceCompassPointReminderKind.ALARM, result.kind)
        assertEquals(9, result.alarmHour); assertEquals(32, result.alarmMinute)
        assertEquals(ms("2026-10-16T09:32:45Z"), result.eventTimeMs)
    }
    @Test fun tomorrowCanUseTheClockOnlyWhenItIsTheNextOccurrenceOfThatTime() {
        assertEquals(SpaceCompassPointReminderKind.ALARM, plan("2026-10-17T07:00:00Z")!!.kind)
        assertEquals(SpaceCompassPointReminderKind.CALENDAR, plan("2026-10-17T10:00:00Z")!!.kind)
    }
    @Test fun aFarFutureDateUsesCalendarRatherThanAnAlarmToday() {
        assertEquals(SpaceCompassPointReminderKind.CALENDAR, plan("2032-06-01T10:00:00Z")!!.kind)
    }
    @Test fun anImminentEventInTheCurrentMinuteDoesNotRollOverToTomorrow() {
        assertEquals(SpaceCompassPointReminderKind.CALENDAR, plan("2026-10-16T08:10:55Z")!!.kind)
        assertEquals(SpaceCompassPointReminderKind.ALARM, plan("2026-10-16T08:11:00Z")!!.kind)
    }
    @Test fun aScenarioInAnotherTimeZoneStillPassesTheCorrectPhoneHour() {
        val request = plan("2026-10-16T14:05:00Z", zone = ZoneId.of("Europe/Rome"))!!
        assertEquals(16, request.alarmHour); assertEquals(5, request.alarmMinute)
        assertEquals("Europe/Rome", request.deviceZoneId)
        assertEquals(10, Instant.ofEpochMilli(request.eventTimeMs).atZone(ZoneId.of("America/New_York")).hour)
        assertEquals(SpaceCompassPointReminderKind.ALARM, request.kind)
    }
    @Test fun nonIntegralTimeZonesAndDateBoundariesKeepTheExactInstant() {
        val request = plan("2026-10-16T22:50:00Z", zone = ZoneId.of("Asia/Kathmandu"))!!
        assertEquals(4, request.alarmHour); assertEquals(35, request.alarmMinute)
        assertEquals(SpaceCompassPointReminderKind.ALARM, request.kind)
        assertEquals(ms("2026-10-16T22:50:00Z"), request.eventTimeMs)
    }
    @Test fun bothRepeatedAutumnHoursUseCalendarWithTheirDistinctInstants() {
        val real = ms("2026-10-24T23:00:00Z")
        val first = plan("2026-10-25T00:30:00Z", viewed = real, real = real, zone = ZoneId.of("Europe/Rome"))!!
        val second = plan("2026-10-25T01:30:00Z", viewed = real, real = real, zone = ZoneId.of("Europe/Rome"))!!
        assertEquals(SpaceCompassPointReminderKind.CALENDAR, first.kind)
        assertEquals(SpaceCompassPointReminderKind.CALENDAR, second.kind)
        assertEquals(first.alarmHour, second.alarmHour)
        assertEquals(3_600_000L, second.eventTimeMs - first.eventTimeMs)
    }
    @Test fun springForwardUsesAnUnambiguousFutureHour() {
        val real = ms("2027-03-28T00:00:00Z")
        val request = plan("2027-03-28T01:30:00Z", viewed = real, real = real, zone = ZoneId.of("Europe/Rome"))!!
        assertEquals(SpaceCompassPointReminderKind.ALARM, request.kind)
        assertEquals(3, request.alarmHour)
    }
    @Test fun nonexistentClockTimesTodayDoNotReplaceTheRequestedDateTomorrow() {
        val real = ms("2027-03-28T00:00:00Z")
        assertEquals(SpaceCompassPointReminderKind.CALENDAR,
            plan("2027-03-29T00:30:00Z", viewed = real, real = real, zone = ZoneId.of("Europe/Rome"))!!.kind)
    }
    @Test fun recheckingAfterTimeHasPassedDisablesTheSamePoint() {
        val event = "2026-10-16T09:00:00Z"
        assertNotNull(plan(event))
        assertNull(plan(event, real = ms("2026-10-16T09:00:00Z")))
    }
    @Test fun allObjectsAndAllSpecialEventsShareTheSameSchedulingPolicy() {
        for (body in spaceCompassCelestialCatalogOrder) for (event in SpaceCompassSunPathEvent.entries) {
            val point = SpaceCompassSunPathPoint(now+3_600_000, SpaceCompassSunPosition(180.0, 25.0), event)
            assertEquals(body.name, SpaceCompassPointReminderKind.ALARM,
                spaceCompassPointReminder(point.timeMs, now, now, false, utc)!!.kind)
        }
    }
    @Test fun anUnsafeCalendarEndOverflowIsRejected() {
        assertNull(spaceCompassPointReminder(Long.MAX_VALUE, now, now, false, utc))
    }
    @Test fun manifestUsesOnlyClockHandoffPermissionAndNarrowPackageQueries() {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val permissions = root.getElementsByTagName("uses-permission").let { nodes ->
            (0 until nodes.length).map { nodes.item(it).attributes.getNamedItem("android:name").nodeValue }
        }
        assertTrue("com.android.alarm.permission.SET_ALARM" in permissions)
        assertFalse(permissions.any { it.endsWith("READ_CALENDAR") || it.endsWith("WRITE_CALENDAR") ||
            it.endsWith("SCHEDULE_EXACT_ALARM") || it.endsWith("USE_EXACT_ALARM") })
        assertEquals(1, root.getElementsByTagName("queries").length)
    }
    @Test fun reminderStringsHaveCompleteParityAcrossTwentyLanguages() {
        val files = File("src/main/res").listFiles()!!.map { File(it,"point_reminders.xml") }.filter { it.isFile }
        assertEquals(20, files.size)
        val expected = setOf("point_reminder_alarm","point_reminder_calendar","point_reminder_unavailable",
            "point_reminder_no_clock","point_reminder_no_calendar", "settings_events_title",
            "settings_events_show_alarm", "settings_events_description")
        for (file in files) {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
            assertEquals(file.path, expected.size, nodes.length)
            assertEquals(file.path, expected, (0 until nodes.length).map {
                assertTrue(nodes.item(it).textContent.isNotBlank())
                nodes.item(it).attributes.getNamedItem("name").nodeValue
            }.toSet())
        }
    }
}
