package com.github.adamyork.sparrow.wasm.common.data

import com.github.adamyork.sparrow.platform.common.data.ElementState
import com.github.adamyork.sparrow.platform.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class FrameMetadataState(
    val colliding: GameElementCollisionState,
    val interacting: EnemyInteractionState,
    val state: ElementState
)
