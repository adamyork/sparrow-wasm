package com.github.adamyork.sparrow.wasm.common

import com.github.adamyork.sparrow.wasm.common.data.GameLifeCycleState
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface StatusProvider {

    var gameLifeCycleState: GameLifeCycleState
    var gameMapState: GameMapState
    var lastPaintTime: Double

    fun getDeltaTimeCoefficient(): Double

    fun getFps(): Double

    fun setCurrentFrameTime(timestamp: Double)

    fun getCurrentFrameTime() : Double

    fun reset()

}


