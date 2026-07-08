package com.github.adamyork.sparrow.wasm.common


import kotlinx.browser.window

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface ThrottledAnimator {

    val animationTargetFps: Double
    var animationTickCounter: Int
    var lastAnimationTickTimeMs: Double
    var animationTickBufferMs: Double

    private val animationFrameIntervalMs: Double
        get() = 1000.0 / animationTargetFps.coerceAtLeast(1.0)

    fun shouldAdvanceAnimationFrame(): Boolean {
        val nowMs = window.performance.now()
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