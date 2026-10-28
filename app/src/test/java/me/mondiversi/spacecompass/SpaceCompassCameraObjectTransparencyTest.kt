package me.mondiversi.spacecompass

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCameraObjectTransparencyTest {
    private val lens=SpaceCompassCameraLens(0.0,0.0,1024.0,768.0,768.0,768.0,512.0,384.0,0)

    @Test fun theRememberedCameraAidCannotFadeAVirtualSkyOrAnUnmodifiedPhoto() {
        assertEquals(1f,spaceCompassCameraObjectOpacity(false,false),0f)
        assertEquals(1f,spaceCompassCameraObjectOpacity(false,true),0f)
        assertEquals(1f,spaceCompassCameraObjectOpacity(true,false),0f)
        assertEquals(.5f,spaceCompassCameraObjectOpacity(true,true),0f)
        assertEquals(1f,SpaceCompassCameraPhotoSnapshot(File("frame.jpg"),lens,0,null).objectOpacity,0f)
    }
    @Test fun aCapturedPhotoKeepsItsOpacityWhenLaterControlsAndExportLanguageChange() {
        val captured=SpaceCompassCameraPhotoSnapshot(File("frame.jpg"),lens,0,null,
            objectOpacity=spaceCompassCameraObjectOpacity(true,true))
        val laterChoice=spaceCompassCameraObjectOpacity(true,false)
        assertEquals(1f,laterChoice,0f)
        assertEquals(.5f,captured.objectOpacity,0f)
        assertEquals(.5f,captured.copy(warning="Localized export warning").objectOpacity,0f)
    }
    @Test fun invalidOpacityCannotEnterAFrozenPhoto() {
        for (value in listOf(Float.NaN,Float.POSITIVE_INFINITY,-.1f,1.1f))
            assertThrows(IllegalArgumentException::class.java) {
                SpaceCompassCameraPhotoSnapshot(File("frame.jpg"),lens,0,null,objectOpacity=value)
            }
    }
}
