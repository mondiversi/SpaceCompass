package me.mondiversi.spacecompass

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private const val AMBIENT_TABLE_SIZE = 4096
private val ambientSine = FloatArray(AMBIENT_TABLE_SIZE + 1) { sin(2 * PI * it / AMBIENT_TABLE_SIZE).toFloat() }

/** A bounded voice pool reproduces the website's detuned sine pads and sparse bells. */
internal class SpaceCompassAmbientTone(private val sampleRate: Int) {
    var active = false; private set
    var left = 0f; private set
    var right = 0f; private set
    private var phase = 0.0
    private var step = 0.0
    private var age = 0
    private var attack = 0
    private var plateau = 0
    private var duration = 0
    private var peak = 0f
    private var bell = false
    private var decay = 1f
    private var envelope = 0f
    private var leftPan = 0f
    private var rightPan = 0f

    fun start(note: Int, pan: Double, peak: Float, bell: Boolean) {
        this.peak = peak; this.bell = bell
        age = 0; phase = 0.0; active = true; envelope = peak
        attack = (sampleRate * if (bell) .018 else 4.8).toInt()
        duration = sampleRate * if (bell) 7 else 26
        plateau = duration - sampleRate * 7
        val frequency = 440 * 2.0.pow((note - 69) / 12.0) * 2.0.pow(if (bell) 0.0 else pan * 8 / 1200)
        step = frequency * AMBIENT_TABLE_SIZE / sampleRate
        leftPan = cos((pan + 1) * PI / 4).toFloat(); rightPan = sin((pan + 1) * PI / 4).toFloat()
        decay = (.0001 / peak).pow(1.0 / (duration - attack)).toFloat()
    }

    fun next(): Float {
        if (!active) return 0f
        if (age >= duration) { active = false; return 0f }
        val gain = when {
            age < attack -> peak * age / attack
            bell -> envelope.also { envelope *= decay }
            age < plateau -> peak
            else -> peak * (duration - age) / (duration - plateau)
        }
        val index = phase.toInt(); val fraction = (phase - index).toFloat()
        val value = (ambientSine[index] + (ambientSine[index + 1] - ambientSine[index]) * fraction) * gain
        phase += step
        if (phase >= AMBIENT_TABLE_SIZE) phase -= AMBIENT_TABLE_SIZE
        age++; left = value * leftPan; right = value * rightPan
        return value
    }
}
