package me.mondiversi.spacecompass

import kotlin.math.exp
import kotlin.math.sin

internal const val SPACE_COMPASS_AMBIENT_RATE = 12000
internal const val SPACE_COMPASS_AMBIENT_BLOCK = 1024

/** Continuous website score, synthesized off the UI thread with bounded, reusable buffers. */
internal class SpaceCompassAmbientSynth(seed: Long = 426L) {
    private val chords = arrayOf(intArrayOf(57, 60, 64, 71), intArrayOf(53, 57, 60, 64),
        intArrayOf(48, 55, 59, 64), intArrayOf(55, 57, 62, 67))
    private val bells = intArrayOf(76, 79, 83, 81, 76, 74, 79, 72)
    private val voices = Array(18) { SpaceCompassAmbientTone(SPACE_COMPASS_AMBIENT_RATE) }
    private val left = FloatArray(SPACE_COMPASS_AMBIENT_BLOCK)
    private val right = FloatArray(SPACE_COMPASS_AMBIENT_BLOCK)
    private val wetLeft = FloatArray(SPACE_COMPASS_AMBIENT_BLOCK)
    private val wetRight = FloatArray(SPACE_COMPASS_AMBIENT_BLOCK)
    private val leftFilter = SpaceCompassAmbientLowPass(SPACE_COMPASS_AMBIENT_RATE)
    private val rightFilter = SpaceCompassAmbientLowPass(SPACE_COMPASS_AMBIENT_RATE)
    private val fft = SpaceCompassAmbientFft(SPACE_COMPASS_AMBIENT_BLOCK * 2)
    private val reverbs = spaceCompassAmbientImpulses(SPACE_COMPASS_AMBIENT_RATE, seed).let {
        arrayOf(SpaceCompassAmbientConvolver(it[0], fft), SpaceCompassAmbientConvolver(it[1], fft))
    }
    private val compressor = SpaceCompassAmbientCompressor(SPACE_COMPASS_AMBIENT_RATE)
    private val smooth = (1 - exp(-1.0 / (SPACE_COMPASS_AMBIENT_RATE * .12))).toFloat()
    private var volume = 0f
    private var nextChord = (SPACE_COMPASS_AMBIENT_RATE * .08).toLong()
    private var nextBell = SPACE_COMPASS_AMBIENT_RATE * 5L
    private var chordIndex = 0
    private var bellIndex = 0L
    var frame = 0L; private set

    fun fadeIn() { volume = 0f }

    fun render(output: FloatArray, targetGain: Float) {
        require(output.size == SPACE_COMPASS_AMBIENT_BLOCK * 2)
        val target = targetGain.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        for (index in left.indices) {
            if (frame >= nextChord) {
                val notes = chords[chordIndex++ % chords.size]
                for (i in notes.indices) {
                    start(notes[i], -.5 + i * .3, .1f, false)
                    start(notes[i], .45 - i * .25, .055f, false)
                }
                nextChord += SPACE_COMPASS_AMBIENT_RATE * 19L
            }
            if (frame >= nextBell) {
                start(bells[(bellIndex % bells.size).toInt()], sin(bellIndex.toDouble()) * .55, .075f, true)
                bellIndex++; nextBell += (SPACE_COMPASS_AMBIENT_RATE * 8.5).toLong()
            }
            var l = 0f; var r = 0f
            for (voice in voices) {
                if (!voice.active) continue
                voice.next()
                if (voice.active) { l += voice.left; r += voice.right }
            }
            left[index] = leftFilter.next(l); right[index] = rightFilter.next(r); frame++
        }
        reverbs[0].process(left, wetLeft); reverbs[1].process(right, wetRight)
        for (index in left.indices) {
            volume += (target - volume) * smooth
            val l = (left[index] * .7f + wetLeft[index] * .4f) * volume
            val r = (right[index] * .7f + wetRight[index] * .4f) * volume
            val gain = compressor.gain(l, r)
            output[index * 2] = (l * gain).coerceIn(-1f, 1f)
            output[index * 2 + 1] = (r * gain).coerceIn(-1f, 1f)
        }
    }

    private fun start(note: Int, pan: Double, peak: Float, bell: Boolean) {
        val voice = voices.firstOrNull { !it.active } ?: error("Ambient voice pool exhausted")
        voice.start(note, pan, peak, bell)
    }
}
