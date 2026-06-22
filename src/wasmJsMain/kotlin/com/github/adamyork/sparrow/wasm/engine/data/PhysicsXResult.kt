package com.github.adamyork.sparrow.wasm.engine.data

import com.github.adamyork.sparrow.wasm.data.player.PlayerMovingState

data class PhysicsXResult(val x: Int, val vx: Double, val moving: PlayerMovingState)
