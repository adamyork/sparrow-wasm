package com.github.adamyork.sparrow.android.engine.data

/**
 * Per-frame input payload consumed by the Android OpenGL particle runtime.
 */
internal class AndroidGpuParticleFrame(
    val sourceBuffer: FloatArray,
    val viewPortX: Float,
    val viewPortY: Float,
    val sizeMultiplier: Int,
    val deltaTimeSeconds: Float,
    val gravity: Float,
    val projectileSpeed: Float,
    val mapItemReturnSpeed: Float,
    val mapItemReturnMinTravelDist: Float,
    val playerX: Float,
    val playerY: Float,
    val playerWidth: Float,
    val playerHeight: Float
)

