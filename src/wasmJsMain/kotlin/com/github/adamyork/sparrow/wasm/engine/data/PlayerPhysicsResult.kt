package com.github.adamyork.sparrow.wasm.engine.data

import com.github.adamyork.sparrow.wasm.common.data.player.PlayerJumpingState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class PlayerPhysicsResult(
    var nextX: Int = 0,
    var velocityX: Double = 0.0,
    var nextY: Int = 0,
    var velocityY: Double = 0.0,
    var nextJumping: PlayerJumpingState = PlayerJumpingState.GROUNDED
)
