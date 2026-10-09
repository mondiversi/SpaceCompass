package me.mondiversi.spacecompass

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Reusable radix-2 complex transform; no allocation or trigonometry in the audio loop. */
internal class SpaceCompassAmbientFft(val size: Int) {
    init { require(size >= 8 && size and (size - 1) == 0) }
    private val real = FloatArray(size / 2) { cos(2 * PI * it / size).toFloat() }
    private val imaginary = FloatArray(size / 2) { sin(2 * PI * it / size).toFloat() }

    fun transform(data: FloatArray, inverse: Boolean = false) {
        require(data.size == size * 2)
        var reversed = 0
        for (index in 1 until size) {
            var bit = size shr 1
            while (reversed and bit != 0) { reversed = reversed xor bit; bit = bit shr 1 }
            reversed = reversed xor bit
            if (index < reversed) {
                val a = index * 2; val b = reversed * 2
                val r = data[a]; val i = data[a + 1]
                data[a] = data[b]; data[a + 1] = data[b + 1]
                data[b] = r; data[b + 1] = i
            }
        }
        var length = 2
        while (length <= size) {
            val half = length / 2; val stride = size / length
            var base = 0
            while (base < size) {
                for (offset in 0 until half) {
                    val phase = offset * stride
                    val wr = real[phase]; val wi = imaginary[phase] * if (inverse) 1f else -1f
                    val a = (base + offset) * 2; val b = (base + offset + half) * 2
                    val r = wr * data[b] - wi * data[b + 1]
                    val i = wr * data[b + 1] + wi * data[b]
                    data[b] = data[a] - r; data[b + 1] = data[a + 1] - i
                    data[a] += r; data[a + 1] += i
                }
                base += length
            }
            length *= 2
        }
        if (inverse) for (index in data.indices) data[index] /= size.toFloat()
    }
}
