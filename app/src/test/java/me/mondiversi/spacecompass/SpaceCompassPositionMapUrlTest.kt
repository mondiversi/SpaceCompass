package me.mondiversi.spacecompass

import java.net.URI
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPositionMapUrlTest {
    @Test fun coordinatesUseUrlDecimalsEvenWhenThePhoneUsesDecimalCommas() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            val url = URI(requireNotNull(spaceCompassPositionMapUrl(41.9028, 12.4964)))
            assertEquals("https", url.scheme)
            assertEquals("www.openstreetmap.org", url.host)
            assertTrue(url.query.contains("marker=41.9028,12.4964"))
            val bounds = url.query.substringAfter("bbox=").substringBefore('&').split(',').map(String::toDouble)
            assertEquals(4, bounds.size)
            assertTrue(bounds[0] < 12.4964 && bounds[2] > 12.4964)
            assertTrue(bounds[1] < 41.9028 && bounds[3] > 41.9028)
        } finally { Locale.setDefault(old) }
    }
    @Test fun invalidAndMissingFixesNeverCreateMapRequests() {
        for ((lat, lon) in listOf(null to 12.0, 41.0 to null, Double.NaN to 12.0, 41.0 to Double.POSITIVE_INFINITY,
            91.0 to 12.0, 41.0 to -181.0)) assertNull(spaceCompassPositionMapUrl(lat, lon))
    }
    @Test fun polarAndDateLineBoxesRemainWithinTheWebMapProjection() {
        for (lat in listOf(-90.0, 0.0, 90.0)) for (lon in listOf(-180.0, 180.0)) {
            val url = requireNotNull(spaceCompassPositionMapUrl(lat, lon))
            val b = url.substringAfter("bbox=").substringBefore('&').split(',').map(String::toDouble)
            assertTrue(b.all { it.isFinite() })
            assertTrue(b[0] >= -180 && b[2] <= 180 && b[0] < b[2])
            assertTrue(b[1] >= -85.0511 && b[3] <= 85.0511 && b[1] < b[3])
        }
    }
}
