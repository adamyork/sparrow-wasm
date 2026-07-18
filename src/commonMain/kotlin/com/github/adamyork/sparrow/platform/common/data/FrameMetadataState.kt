package com.github.adamyork.sparrow.platform.common.data

import com.github.adamyork.sparrow.platform.common.data.enemy.EnemyInteractionState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class FrameMetadataState(
    val colliding: GameElementCollisionState,
    val interacting: EnemyInteractionState,
    val state: ElementState
)
