package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassStarAtlasTest {
    @Test fun fullDetailIsRetainedWhenMemoryAndGpuSupportTheSource() {
        assertEquals(1, spaceCompassStarAtlasSampleSize(4096, 2048, 4096, true))
        assertEquals(1, spaceCompassStarAtlasSampleSize(4096, 2048, 16384, true))
    }

    @Test fun olderGpuAndLowMemoryDevicesDoNotDecodeAnUnsupportedAtlas() {
        assertEquals(2, spaceCompassStarAtlasSampleSize(4096, 2048, 2048, true))
        assertEquals(2, spaceCompassStarAtlasSampleSize(4096, 2048, 16384, false))
        assertEquals(4, spaceCompassStarAtlasSampleSize(4096, 2048, 1024, false))
    }

    @Test fun bothAxesAndNonPowerOfTwoLimitsAreRespectedWithoutUpscaling() {
        assertEquals(2, spaceCompassStarAtlasSampleSize(2048, 4096, 2048, true))
        assertEquals(2, spaceCompassStarAtlasSampleSize(4096, 2048, 3072, true))
        assertEquals(4, spaceCompassStarAtlasSampleSize(4097, 2048, 2048, true))
        assertEquals(1, spaceCompassStarAtlasSampleSize(512, 256, 4096, false))
    }

    @Test fun invalidDimensionsAreRejectedBeforeBitmapAllocation() {
        for ((width, height, limit) in listOf(Triple(0, 2048, 4096), Triple(4096, -1, 4096),
            Triple(4096, 2048, 0))) {
            assertThrows(IllegalArgumentException::class.java) {
                spaceCompassStarAtlasSampleSize(width, height, limit, true)
            }
        }
    }
}
