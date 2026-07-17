package com.github.adamyork.sparrow.platform.common

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface ThrottledAnimator {

    val platformInterop: PlatformInterop
    val animationTargetFps: Double
    var animationTickCounter: Int
    var lastAnimationTickTimeMs: Double
    var animationTickBufferMs: Double

    private val animationFrameIntervalMs: Double
        get() = 1000.0 / animationTargetFps.coerceAtLeast(1.0)

    fun shouldAdvanceAnimationFrame(): Boolean {
        val nowMs = platformInterop.getPlatformNowTime()
        if (lastAnimationTickTimeMs <= 0.0) {
            lastAnimationTickTimeMs = nowMs
            return false
        }
        val elapsedMs = (nowMs - lastAnimationTickTimeMs).coerceAtLeast(0.0)
        lastAnimationTickTimeMs = nowMs
        animationTickBufferMs += elapsedMs
        animationTickCounter += 1
        if (animationTickBufferMs < animationFrameIntervalMs) {
            return false
        }
        animationTickBufferMs -= animationFrameIntervalMs
        animationTickCounter = 0
        return true
    }
}
