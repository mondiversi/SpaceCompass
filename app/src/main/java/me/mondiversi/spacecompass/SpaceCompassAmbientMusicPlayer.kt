package me.mondiversi.spacecompass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** Foreground-only generative music; saved policy and device media volume remain independent. */
internal class SpaceCompassAmbientMusicPlayer(context: Context) {
    private val context = context.applicationContext
    private val audio = this.context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val policy = SpaceCompassAmbientMusicPolicy()
    private var ownsRequest = false
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val output = SpaceCompassAmbientMusicOutput(attributes) {
        policy.onFocus(SpaceCompassMusicFocus.LOST)
        abandonFocus()
    }
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
                output.stop(); abandonFocus()
            }
        }
    }
    init {
        ContextCompat.registerReceiver(this.context, noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun update(settings: SpaceCompassAmbientMusicSettings) { policy.update(settings); reconcile() }
    fun foreground(value: Boolean) { policy.setForeground(value); reconcile() }

    private fun reconcile() {
        if (!policy.wantsAudio) { output.stop(); abandonFocus(); return }
        if (policy.canRequestFocus) {
            val granted = audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            ownsRequest = granted
            policy.onFocus(if (granted) SpaceCompassMusicFocus.GRANTED else SpaceCompassMusicFocus.LOST)
        }
        if (policy.canPlay) output.start(policy.settings.gain) else output.stop()
    }

    private fun abandonFocus() {
        if (ownsRequest) audio.abandonAudioFocusRequest(request)
        ownsRequest = false
    }

    fun release() {
        policy.setForeground(false); output.release(); abandonFocus()
        context.unregisterReceiver(noisy)
    }
}
