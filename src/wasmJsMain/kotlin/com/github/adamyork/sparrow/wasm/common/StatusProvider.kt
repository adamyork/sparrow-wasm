package com.github.adamyork.sparrow.wasm.common

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface StatusProvider {

    var running: Boolean
    var lastPaintTime: Long

    fun getDeltaTimeCoefficient(): Double

    fun atOrUnderFpsMax(nextPaintTimeMs: Long): Boolean

    fun getFps(): Double

    fun reset()
}