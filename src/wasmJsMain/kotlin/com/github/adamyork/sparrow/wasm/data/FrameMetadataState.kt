package com.github.adamyork.sparrow.wasm.data

import com.github.adamyork.sparrow.wasm.data.enemy.EnemyInteractionState

data class FrameMetadataState(
    val colliding: GameElementCollisionState,
    val interacting: EnemyInteractionState,
    val state: GameElementState
)
