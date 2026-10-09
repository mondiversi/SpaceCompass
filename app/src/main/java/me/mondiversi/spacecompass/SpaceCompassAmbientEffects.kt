package me.mondiversi.spacecompass

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** The same 2300 Hz low-pass/Q as the website, with persistent stereo filter state. */
internal class SpaceCompassAmbientLowPass(sampleRate: Int) {
    private val omega = 2 * PI * 2300 / sampleRate
    private val alpha = sin(omega) / (2 * 10.0.pow(.35 / 20))
    private val b0 = (1 - cos(omega)) / 2 / (1 + alpha)
    private val b1 = (1 - cos(omega)) / (1 + alpha)
    private val b2 = b0
    private val a1 = -2 * cos(omega) / (1 + alpha)
    private val a2 = (1 - alpha) / (1 + alpha)
    private var z1 = 0.0
    private var z2 = 0.0

    fun next(input: Float): Float {
        val output = b0 * input + z1
        z1 = b1 * input - a1 * output + z2
        z2 = b2 * input - a2 * output
        return output.toFloat()
    }
}

/** Stereo-linked soft-knee compression: -18 dB, 18 dB knee, 3:1, 20/350 ms. */
internal class SpaceCompassAmbientCompressor(sampleRate: Int) {
    private val attack = exp(-1.0 / (sampleRate * .02)).toFloat()
    private val release = exp(-1.0 / (sampleRate * .35)).toFloat()
    private var envelope = 0f
    private var gain = 1f
    private var counter = 0

    fun gain(left: Float, right: Float): Float {
        val peak = max(abs(left), abs(right))
        val coefficient = if (peak > envelope) attack else release
        envelope = peak + coefficient * (envelope - peak)
        if (counter++ % 16 == 0) {
            val db = 20 * log10(envelope.coerceAtLeast(.000001f).toDouble())
            val reduction = when {
                db < -27 -> 0.0
                db < -9 -> (1.0 / 3 - 1) * (db + 27).pow(2) / 36
                else -> (1.0 / 3 - 1) * (db + 18)
            }
            gain = 10.0.pow(reduction / 20).toFloat()
        }
        return gain
    }
}
