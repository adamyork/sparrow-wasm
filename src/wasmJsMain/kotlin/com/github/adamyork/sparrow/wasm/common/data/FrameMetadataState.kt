package com.github.adamyork.sparrow.wasm.common.data

import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState

data class FrameMetadataState(
    val colliding: GameElementCollisionState,
    val interacting: EnemyInteractionState,
    val state: GameElementState
)
