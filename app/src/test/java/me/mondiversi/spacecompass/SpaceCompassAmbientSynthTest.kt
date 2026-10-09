package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class SpaceCompassAmbientSynthTest {
    @Test fun complexTransformRoundTripsRealAndImaginarySamples() {
        val fft = SpaceCompassAmbientFft(32)
        val data = FloatArray(64) { sin(it * .37).toFloat() }
        val expected = data.copyOf()
        fft.transform(data); fft.transform(data, inverse = true)
        assertArrayEquals(expected, data, .00001f)
    }

    @Test fun partitionedReverbMatchesDirectConvolutionAcrossSeveralBlockBoundaries() {
        val block = 16
        val impulse = FloatArray(53) { sin(it * .71).toFloat() * .04f }
        val input = FloatArray(96) { sin(it * .19).toFloat() }
        val expected = FloatArray(160)
        for (i in input.indices) for (j in impulse.indices) expected[i + j] += input[i] * impulse[j]
        val convolver = SpaceCompassAmbientConvolver(impulse, SpaceCompassAmbientFft(block * 2))
        val output = FloatArray(expected.size)
        for (start in output.indices step block) {
            val samples = FloatArray(block) { input.getOrElse(start + it) { 0f } }
            val result = FloatArray(block); convolver.process(samples, result)
            result.copyInto(output, start)
        }
        assertArrayEquals(expected, output, .00001f)
    }

    @Test fun localImpulseHasIndependentStereoChannelsAndATaperedCompleteTail() {
        val impulse = spaceCompassAmbientImpulses(12000, 426L)
        assertEquals(2, impulse.size); assertEquals(43200, impulse[0].size)
        assertTrue(impulse[0].indices.any { impulse[0][it] != impulse[1][it] })
        for (channel in impulse) {
            assertTrue(channel.all { it.isFinite() })
            assertTrue(channel.takeLast(1200).sumOf { (it * it).toDouble() } <
                channel.take(1200).sumOf { (it * it).toDouble() } / 10000)
        }
    }

    @Test fun sineVoiceKeepsTheMusicalPitchAndEndsAtZeroWithoutAnAbruptCut() {
        val tone = SpaceCompassAmbientTone(12000)
        tone.start(60, 0.0, .1f, false)
        repeat(12000 * 5) { tone.next() }
        var previous = tone.next(); var crossings = 0
        repeat(12000) {
            val sample = tone.next()
            if (previous <= 0 && sample > 0) crossings++
            previous = sample
        }
        assertTrue("Middle C pitch", crossings in 260..263)
        var ending = 0f
        repeat(12000 * 21) { ending = tone.next() }
        assertFalse(tone.active); assertEquals(0f, ending, 0f)
    }

    @Test fun overlappingScoreAndBlockEdgesRemainContinuousAndStereo() {
        val synth = SpaceCompassAmbientSynth()
        val block = FloatArray(SPACE_COMPASS_AMBIENT_BLOCK * 2)
        var previous = 0f; var maximumStep = 0f; var stereoDifference = 0.0
        var overlapPower = 0.0; var overlapSamples = 0
        repeat(310) {
            synth.render(block, .2f)
            for (index in block.indices step 2) {
                val left = block[index]; val right = block[index + 1]
                assertTrue(left.isFinite() && right.isFinite())
                assertTrue(abs(left) < 1 && abs(right) < 1)
                maximumStep = maxOf(maximumStep, abs(left - previous)); previous = left
                stereoDifference += abs(left - right)
                if (synth.frame > 12000 * 19 && synth.frame < 12000 * 25) {
                    overlapPower += left * left; overlapSamples++
                }
            }
        }
        assertEquals(310L * SPACE_COMPASS_AMBIENT_BLOCK, synth.frame)
        assertTrue("No click at block/chord boundaries", maximumStep < .12f)
        assertTrue("Audible overlapping chords", overlapPower / overlapSamples > .000001)
        assertTrue("Stereo is retained", stereoDifference > 1)
    }

    @Test fun invalidOrMutedGainNeverProducesNonFiniteOrAudibleSamples() {
        val synth = SpaceCompassAmbientSynth()
        val block = FloatArray(SPACE_COMPASS_AMBIENT_BLOCK * 2)
        for (gain in listOf(Float.NaN, Float.POSITIVE_INFINITY, -10f, 0f)) {
            synth.render(block, gain)
            assertTrue(block.all { it.isFinite() && it == 0f })
        }
    }
}
