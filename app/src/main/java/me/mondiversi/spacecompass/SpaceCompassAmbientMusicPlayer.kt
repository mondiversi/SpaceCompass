package me.mondiversi.spacecompass

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/** One asynchronous, foreground-only player; volume never changes the device's media setting. */
internal class SpaceCompassAmbientMusicPlayer(context: Context) {
    private val context = context.applicationContext
    private val audio = this.context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val policy = SpaceCompassAmbientMusicPolicy()
    private var player: MediaPlayer? = null
    private var prepared = false
    private var ownsRequest = false
    private var positionMs = 0
    private var gain = 0f
    private var targetGain = 0f
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change ->
            policy.onFocus(when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> SpaceCompassMusicFocus.GRANTED
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    SpaceCompassMusicFocus.TRANSIENT_LOSS
                else -> SpaceCompassMusicFocus.LOST
            })
            if (change == AudioManager.AUDIOFOCUS_LOSS) abandonFocus()
            reconcile()
        }, handler).build()
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                policy.onFocus(SpaceCompassMusicFocus.LOST)
                releaseTrack(); abandonFocus()
            }
        }
    }
    init {
        ContextCompat.registerReceiver(this.context, noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private val fade = object : Runnable {
        override fun run() {
            if (!prepared || !policy.canPlay) return
            val difference = targetGain - gain
            gain = if (kotlin.math.abs(difference) < .002f) targetGain else gain + difference * .22f
            player?.setVolume(gain, gain)
            if (gain != targetGain) handler.postDelayed(this, 30)
        }
    }

    fun update(settings: SpaceCompassAmbientMusicSettings) { policy.update(settings); reconcile() }
    fun foreground(value: Boolean) { policy.setForeground(value); reconcile() }

    private fun reconcile() {
        if (!policy.wantsAudio) { releaseTrack(); abandonFocus(); return }
        if (policy.canRequestFocus) {
            val granted = audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            ownsRequest = granted
            policy.onFocus(if (granted) SpaceCompassMusicFocus.GRANTED else SpaceCompassMusicFocus.LOST)
        }
        if (!policy.canPlay) {
            handler.removeCallbacks(fade)
            if (prepared && player?.isPlaying == true) player?.pause()
            return
        }
        if (player == null) prepareTrack()
        val active = player ?: return
        if (!prepared) return
        if (!active.isPlaying) {
            gain = 0f; active.setVolume(0f, 0f); active.start()
        }
        targetGain = policy.settings.gain
        handler.removeCallbacks(fade)
        handler.post(fade)
    }

    private fun prepareTrack() {
        val active = MediaPlayer()
        player = active
        try {
            active.setAudioAttributes(attributes)
            context.resources.openRawResourceFd(R.raw.space_ambient).use { asset ->
                active.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            active.setOnPreparedListener { ready ->
                if (player === ready) {
                    prepared = true; ready.isLooping = true
                    if (positionMs > 0) ready.seekTo(positionMs)
                    reconcile()
                }
            }
            active.setOnErrorListener { _, what, extra ->
                Log.w("SpaceCompassMusic", "Playback unavailable ($what/$extra)")
                policy.onFocus(SpaceCompassMusicFocus.LOST)
                releaseTrack(); abandonFocus(); true
            }
            active.prepareAsync()
        } catch (_: Exception) {
            Log.w("SpaceCompassMusic", "Ambient music could not be prepared")
            policy.onFocus(SpaceCompassMusicFocus.LOST)
            releaseTrack(); abandonFocus()
        }
    }

    private fun abandonFocus() {
        if (ownsRequest) audio.abandonAudioFocusRequest(request)
        ownsRequest = false
    }

    private fun releaseTrack() {
        handler.removeCallbacks(fade)
        val active = player
        player = null
        if (active != null) {
            if (prepared) positionMs = runCatching { active.currentPosition }.getOrDefault(0)
            active.setOnPreparedListener(null); active.setOnErrorListener(null)
            active.release()
        }
        prepared = false; gain = 0f
    }

    fun release() {
        policy.setForeground(false); releaseTrack(); abandonFocus()
        context.unregisterReceiver(noisy)
    }
}
