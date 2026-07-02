package com.github.adamyork.sparrow.wasm.common

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface StatusProvider {

    var running: Boolean
    var lastPaintTime: Double

    fun getDeltaTimeCoefficient(): Double

    fun atOrUnderTargetFps(nextPaintTimeMs: Double): Boolean

    fun getFps(): Double

    fun reset()

    fun setCurrentFrameTime(timestamp: Double)

    fun getCurrentFrameTime() : Double
}