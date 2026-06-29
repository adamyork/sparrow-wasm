package com.github.adamyork.sparrow.wasm.engine.data

import com.github.adamyork.sparrow.wasm.common.data.player.PlayerJumpingState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class PhysicsYResult(
    val y: Int,
    val vy: Double,
    val jumping: PlayerJumpingState
)
