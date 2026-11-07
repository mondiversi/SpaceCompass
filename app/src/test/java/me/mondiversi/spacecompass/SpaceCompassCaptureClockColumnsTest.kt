package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCaptureClockColumnsTest {
    @Test fun ordinaryAndNamedPointClockRowsBecomeTwoColumnsWhileAngleSeparatorsRemain() {
        for (title in listOf<String?>(null, "Culmination", "Personal · T+3h27m")) {
            val caption = formatSpaceCompassPanoramaPointLabel("21/06/2027 12:00", SpaceCompassSunPosition(180.0, 45.0),
                SpaceCompassPanoramaFormatting(), title, "T−9999h59m")
            val lines = caption.lines()
            val row = spaceCompassCaptureClockColumns(lines, lines.lastIndex - 1)!!
            assertEquals("21/06/2027 12:00", row.clock)
            assertEquals("T−9999h59m", row.countdown)
            assertTrue(lines.last().contains(" · ↕ "))
            if (title != null) assertNull(spaceCompassCaptureClockColumns(lines, 0))
            assertNull(spaceCompassCaptureClockColumns(lines, lines.lastIndex))
        }
    }

    @Test fun localizedCountdownDigitsAreKeptAndClockOnlyRowsStayUnchanged() {
        val lines = listOf("Attuale", "1:30 PM · T−١h٢٧m", "↔ 180,0° · ↕ +30,0°")
        assertEquals(SpaceCompassCaptureClockColumns("1:30 PM", "T−١h٢٧m"), spaceCompassCaptureClockColumns(lines, 1))
        assertNull(spaceCompassCaptureClockColumns(listOf("12:00", lines.last()), 0))
        assertNull(spaceCompassCaptureClockColumns(listOf("A title · T+3h27m", "12:00", lines.last()), 0))
    }

    @Test fun measuredCaptionsAlwaysLeaveRoomForBothOppositeColumnsAtEveryScale() {
        val cases = listOf(
            listOf("12:00 · T+0h0m", "↔ 1,0° · ↕ 2,0°"),
            listOf("Rise", "21/06/2027 11:59 PM · T−123456h59m", "↔ 123,4° · ↕ -56,7°"),
            listOf("A much longer personal title that sets the caption width", "١٢:٣٠ · T+١h٢٧m", "↔ 10° · ↕ 20°"))
        for (scale in listOf(.5f, 1f, 2f)) for (lines in cases) {
            val measure: (String) -> Float = { text -> text.sumOf { if (it == ' ') 4 else 8 }.toFloat() * scale }
            val gap = measure("  ")
            val width = spaceCompassCaptureCaptionContentWidth(lines, gap, measure)
            val columns = spaceCompassCaptureClockColumns(lines, lines.lastIndex - 1)!!
            val clockEnd = measure(columns.clock)
            val countdownStart = width - measure(columns.countdown)
            assertTrue(countdownStart - clockEnd >= gap - .001f)
            assertTrue(width >= measure(lines.last()))
            if (lines.size == 3) assertTrue(width >= measure(lines.first()))
        }
    }
}
