package com.github.adamyork.sparrow.wasm.engine.data

import com.github.adamyork.sparrow.wasm.common.data.player.PlayerJumpingState

data class PhysicsYResult(
    val y: Int,
    val vy: Double,
    val jumping: PlayerJumpingState
)
