package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface StatusProvider {

    var running: Boolean
    var lastPaintTime: Double
    var gameMapState: GameMapState?

    fun getDeltaTimeCoefficient(): Double


    fun getFps(): Double

    fun reset()

    fun setCurrentFrameTime(timestamp: Double)

    fun getCurrentFrameTime() : Double
}
