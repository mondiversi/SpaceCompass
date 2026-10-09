package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassAmbientMusicPolicyTest {
    @Test fun freshInstallEnablesMusicAtRestrainedVolumeButStillRequiresFocus() {
        val p = SpaceCompassAmbientMusicPolicy()
        assertTrue(p.settings.enabled)
        assertFalse(p.wantsAudio); assertFalse(p.canRequestFocus); assertFalse(p.canPlay)
        p.setForeground(true)
        assertTrue(p.wantsAudio); assertTrue(p.canRequestFocus); assertFalse(p.canPlay)
        assertEquals(20, p.settings.volumePercent)
        p.onFocus(SpaceCompassMusicFocus.GRANTED)
        assertTrue(p.canPlay)
    }

    @Test fun aSavedDisabledChoiceRemainsSilentAcrossForegroundTransitions() {
        val p = SpaceCompassAmbientMusicPolicy()
        p.update(SpaceCompassAmbientMusicSettings(enabled = false))
        p.setForeground(true)
        assertFalse(p.wantsAudio); assertFalse(p.canRequestFocus); assertFalse(p.canPlay)
        p.setForeground(false); p.setForeground(true)
        assertFalse(p.canRequestFocus)
        assertFalse(p.settings.enabled)
    }

    @Test fun savedEnableAndVolumeStillRequireForegroundAndFocus() {
        val p = SpaceCompassAmbientMusicPolicy()
        p.update(SpaceCompassAmbientMusicSettings(true, 64))
        assertFalse(p.canRequestFocus)
        p.setForeground(true); assertTrue(p.canRequestFocus); assertFalse(p.canPlay)
        p.onFocus(SpaceCompassMusicFocus.GRANTED)
        assertTrue(p.canPlay); assertEquals(.64f, p.settings.gain, .00001f)
    }

    @Test fun backgroundAndLateFocusGainCannotRestartAudio() {
        val p = playing()
        p.setForeground(false)
        p.onFocus(SpaceCompassMusicFocus.GRANTED)
        assertFalse(p.canPlay); assertFalse(p.canRequestFocus)
        assertTrue(p.settings.enabled)
        p.setForeground(true); assertTrue(p.canRequestFocus); assertFalse(p.canPlay)
    }

    @Test fun transientLossPausesWithoutStealingFocusAndGainResumes() {
        val p = playing()
        p.onFocus(SpaceCompassMusicFocus.TRANSIENT_LOSS)
        p.update(p.settings.copy(volumePercent = 80))
        assertFalse(p.canPlay); assertFalse(p.canRequestFocus)
        p.onFocus(SpaceCompassMusicFocus.GRANTED); assertTrue(p.canPlay)
    }

    @Test fun permanentLossOrDeniedRequestDoesNotLoopFocusRequests() {
        val p = playing()
        p.onFocus(SpaceCompassMusicFocus.LOST)
        p.update(p.settings.copy(volumePercent = 60))
        assertFalse(p.canRequestFocus); assertFalse(p.canPlay)
        p.setForeground(false); p.setForeground(true)
        assertTrue(p.canRequestFocus)
    }

    @Test fun muteReleasesAudioButKeepsTheSavedEnableChoice() {
        val p = playing()
        p.update(p.settings.copy(volumePercent = 0))
        assertFalse(p.wantsAudio); assertFalse(p.canRequestFocus); assertTrue(p.settings.enabled)
        p.update(p.settings.copy(volumePercent = 40))
        assertTrue(p.canRequestFocus); assertFalse(p.canPlay)
    }

    @Test fun disablingCannotBeUndoneByAnAudioFocusCallback() {
        val p = playing()
        p.update(p.settings.copy(enabled = false))
        p.onFocus(SpaceCompassMusicFocus.GRANTED)
        assertFalse(p.canPlay); assertFalse(p.canRequestFocus)
        p.update(p.settings.copy(enabled = true)); assertTrue(p.canRequestFocus)
    }

    @Test fun corruptVolumeIsClampedWithoutEnablingMusic() {
        val p = SpaceCompassAmbientMusicPolicy()
        p.update(SpaceCompassAmbientMusicSettings(false, -300)); assertEquals(0, p.settings.volumePercent)
        p.update(SpaceCompassAmbientMusicSettings(false, 200)); assertEquals(1f, p.settings.gain, .00001f)
        assertFalse(p.wantsAudio)
    }

    private fun playing() = SpaceCompassAmbientMusicPolicy().apply {
        update(SpaceCompassAmbientMusicSettings(true, 35)); setForeground(true)
        onFocus(SpaceCompassMusicFocus.GRANTED)
    }
}
