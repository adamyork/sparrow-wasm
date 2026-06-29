package com.github.adamyork.sparrow.wasm.engine.data

import com.github.adamyork.sparrow.wasm.common.data.player.PlayerMovingState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class PhysicsXResult(val x: Int, val vx: Double, val moving: PlayerMovingState)
