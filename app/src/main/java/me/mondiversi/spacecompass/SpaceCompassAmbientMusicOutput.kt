package me.mondiversi.spacecompass

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.Executors

/** One serialized PCM worker; cancellation cannot restart audio after focus/lifecycle loss. */
internal class SpaceCompassAmbientMusicOutput(private val attributes: AudioAttributes,
    private val onError: () -> Unit) {
    private class Session(gain: Float) {
        val lock = Any()
        @Volatile var cancelled = false
        @Volatile var gain = gain
        var track: AudioTrack? = null
    }
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "SpaceCompassAudio").apply { isDaemon = true }
    }
    private var active: Session? = null
    private var closed = false
    // Owned exclusively by the serialized worker: notes, phase and reverb survive ordinary pauses.
    private var synth: SpaceCompassAmbientSynth? = null

    fun start(gain: Float) {
        if (closed) return
        active?.let { it.gain = gain; return }
        val session = Session(gain); active = session
        worker.execute { stream(session) }
    }

    fun stop() {
        val session = active ?: return
        active = null
        synchronized(session.lock) {
            session.cancelled = true
            session.track?.let { track -> runCatching { track.pause(); track.flush() } }
        }
    }

    private fun stream(session: Session) {
        var track: AudioTrack? = null
        var blocks = 0L; var totalRender = 0L; var maxRender = 0L
        try {
            if (session.cancelled) return
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val renderer = synth ?: SpaceCompassAmbientSynth().also { synth = it }
            if (session.cancelled) return
            renderer.fadeIn()
            val rate = intArrayOf(24000, 48000).firstOrNull {
                AudioTrack.getMinBufferSize(it, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT) > 0
            } ?: error("PCM output unavailable")
            val ratio = rate / SPACE_COMPASS_AMBIENT_RATE
            val samples = FloatArray(SPACE_COMPASS_AMBIENT_BLOCK * 2)
            val output = FloatArray(samples.size * ratio)
            val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
            val bufferBytes = minimum.coerceAtLeast(output.size * 4 * 3)
            track = AudioTrack.Builder().setAudioAttributes(attributes)
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(bufferBytes).build()
            check(track.state == AudioTrack.STATE_INITIALIZED)
            synchronized(session.lock) {
                if (session.cancelled) return
                session.track = track
            }
            var previousLeft = 0f; var previousRight = 0f
            while (!session.cancelled) {
                val started = SystemClock.elapsedRealtimeNanos()
                renderer.render(samples, session.gain)
                var cursor = 0
                for (frame in 0 until SPACE_COMPASS_AMBIENT_BLOCK) {
                    val left = samples[frame * 2]; val right = samples[frame * 2 + 1]
                    for (part in 1..ratio) {
                        val fraction = part.toFloat() / ratio
                        output[cursor++] = previousLeft + (left - previousLeft) * fraction
                        output[cursor++] = previousRight + (right - previousRight) * fraction
                    }
                    previousLeft = left; previousRight = right
                }
                val elapsed = SystemClock.elapsedRealtimeNanos() - started
                blocks++; totalRender += elapsed; maxRender = maxOf(maxRender, elapsed)
                var written = 0
                while (written < output.size && !session.cancelled) {
                    val count = track.write(output, written, output.size - written, AudioTrack.WRITE_BLOCKING)
                    if (session.cancelled) return
                    check(count > 0) { "PCM write failed ($count)" }
                    written += count
                }
                // Pre-fill one complete block, then atomically check cancellation before playing.
                synchronized(session.lock) {
                    if (session.cancelled) return
                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
                }
            }
        } catch (_: Exception) {
            if (!session.cancelled) main.post {
                if (!closed && active === session && !session.cancelled) {
                    active = null
                    Log.w("SpaceCompassMusic", "Generated audio could not be played")
                    onError()
                }
            }
        } finally {
            val underruns = track?.let { runCatching { it.underrunCount }.getOrDefault(-1) } ?: -1
            synchronized(session.lock) {
                session.track = null
                track?.let { runCatching { it.pause(); it.flush(); it.release() } }
            }
            if (blocks > 0) Log.i("SpaceCompassMusic", "PCM blocks=$blocks averageMs=${totalRender / blocks / 1_000_000.0} maxMs=${maxRender / 1_000_000.0} underruns=$underruns")
        }
    }

    fun release() {
        closed = true; stop(); worker.shutdownNow()
    }
}
