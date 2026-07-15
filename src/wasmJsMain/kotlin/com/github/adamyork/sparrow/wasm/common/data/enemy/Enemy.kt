package com.github.adamyork.sparrow.wasm.common.data.enemy

import com.github.adamyork.sparrow.wasm.common.AnimationFrameException
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadata
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadataState
import com.github.adamyork.sparrow.wasm.common.data.GameElement
import com.github.adamyork.sparrow.wasm.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.GameElementState
import com.github.adamyork.sparrow.wasm.common.data.player.Player

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Enemy : GameElement {

    val id: Int
    val type: EnemyType
    val originX: Int
    val originY: Int
    var enemyPosition: EnemyPosition
    var colliding: GameElementCollisionState
    var interacting: EnemyInteractionState

    fun getNextPosition(): EnemyPosition

    fun getNextEnemyState(player: Player): GameElementState

    fun getNextCollisionMetadataWithState(
        animatingFrames: HashMap<Int, FrameMetadata>,
        collisionFrames: HashMap<Int, FrameMetadata>,
    ): Pair<FrameMetadata, FrameMetadataState> {
        var metadata = animatingFrames[1] ?: throw AnimationFrameException(animatingFrames.toString(), 1)
        var metadataState = FrameMetadataState(this.colliding, this.interacting, state)
        if (colliding == GameElementCollisionState.COLLIDING) {
            if (frameMetadata.frame == BlockerEnemy.ANIMATION_COLLISION_FRAMES) {
                metadataState = metadataState.copy(colliding = GameElementCollisionState.FREE)
                return Pair(metadata, metadataState)
            } else {
                val nextFrame = frameMetadata.frame + 1
                metadata =
                    collisionFrames[nextFrame] ?: throw AnimationFrameException(collisionFrames.toString(), nextFrame)
                return Pair(metadata, metadataState)
            }
        }
        return Pair(metadata, metadataState)
    }

}
