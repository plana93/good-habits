package com.programminghut.pose_detection.effects

import java.util.*

/**
 * Global random provider so filters can be deterministic when seeded.
 * Call setSeed(seed) to make subsequent next*() calls reproducible.
 */
object RandomProvider {
    @Volatile
    private var rng: Random = Random()

    @Synchronized
    fun setSeed(seed: Long) {
        rng = Random(seed)
    }

    fun nextFloat(): Float = rng.nextFloat()
    fun nextDouble(): Double = rng.nextDouble()
    fun nextInt(bound: Int): Int = rng.nextInt(bound)
    fun nextLong(): Long {
        // java.util.Random has nextLong()
        return (rng as? Random)?.nextLong() ?: Random().nextLong()
    }
}
