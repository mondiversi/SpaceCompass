package me.mondiversi.spacecompass

import java.util.Random
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

/** Uniform partitioned convolution retains the complete tail across streaming blocks. */
internal class SpaceCompassAmbientConvolver(impulse: FloatArray, private val fft: SpaceCompassAmbientFft) {
    val blockSize = fft.size / 2
    private val count = ceil(impulse.size.toDouble() / blockSize).toInt().coerceAtLeast(1)
    private val kernels = Array(count) { partition ->
        check(!Thread.currentThread().isInterrupted)
        FloatArray(fft.size * 2).also { data ->
            for (index in 0 until blockSize) data[index * 2] = impulse.getOrElse(partition * blockSize + index) { 0f }
            fft.transform(data)
        }
    }
    private val history = Array(count) { FloatArray(fft.size * 2) }
    private val sum = FloatArray(fft.size * 2)
    private val overlap = FloatArray(blockSize)
    private var cursor = 0

    fun process(input: FloatArray, output: FloatArray) {
        require(input.size == blockSize && output.size == blockSize)
        val recent = history[cursor]; recent.fill(0f)
        for (index in input.indices) recent[index * 2] = input[index]
        fft.transform(recent); sum.fill(0f)
        for (partition in 0 until count) {
            val kernel = kernels[partition]
            val previous = history[(cursor - partition + count) % count]
            var index = 0
            while (index < sum.size) {
                val xr = previous[index]; val xi = previous[index + 1]
                val hr = kernel[index]; val hi = kernel[index + 1]
                sum[index] += xr * hr - xi * hi
                sum[index + 1] += xr * hi + xi * hr
                index += 2
            }
        }
        fft.transform(sum, inverse = true)
        for (index in output.indices) {
            output[index] = sum[index * 2] + overlap[index]
            overlap[index] = sum[(index + blockSize) * 2]
        }
        cursor = (cursor + 1) % count
    }
}

/** Original website's 3.6-second stereo noise impulse and power-2.8 decay, generated locally. */
internal fun spaceCompassAmbientImpulses(sampleRate: Int, seed: Long): Array<FloatArray> {
    val length = (sampleRate * 3.6).toInt()
    val random = Random(seed)
    val channels = Array(2) { FloatArray(length) }
    var power = 0.0
    for (channel in channels) for (index in channel.indices) {
        if (index % 1024 == 0) check(!Thread.currentThread().isInterrupted)
        val value = ((random.nextDouble() * 2 - 1) * (1.0 - index.toDouble() / length).pow(2.8)).toFloat()
        channel[index] = value; power += value * value
    }
    // Preserve the website's 44.1 kHz impulse energy at this lower, band-limited synthesis rate.
    val scale = (.00125 / sqrt(power / (2 * length)).coerceAtLeast(.000125) * sqrt(44100.0 / sampleRate)).toFloat()
    for (channel in channels) for (index in channel.indices) channel[index] *= scale
    return channels
}
