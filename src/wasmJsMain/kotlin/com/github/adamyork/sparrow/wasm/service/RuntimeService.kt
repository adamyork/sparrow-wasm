package com.github.adamyork.sparrow.wasm.service

import com.github.adamyork.sparrow.platform.common.data.LifeCycleState
import com.github.adamyork.sparrow.wasm.common.data.map.GameMapState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface RuntimeService {

    var lifeCycleState: LifeCycleState
    var gameMapState: GameMapState
    var lastPaintTime: Double

    fun getDeltaTimeCoefficient(): Double

    fun getFps(): Double

    fun setCurrentFrameTime(timestamp: Double)

    fun getCurrentFrameTime(): Double

    fun reset()

}


