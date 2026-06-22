package com.github.adamyork.sparrow.wasm.data.enemy

import com.github.adamyork.sparrow.wasm.common.AnimationFrameException
import com.github.adamyork.sparrow.wasm.data.enemy.BlockerEnemy.Companion.ANIMATION_COLLISION_FRAMES
import com.github.adamyork.sparrow.wasm.data.player.Player
import com.github.adamyork.sparrow.wasm.data.FrameMetadata
import com.github.adamyork.sparrow.wasm.data.FrameMetadataState
import com.github.adamyork.sparrow.wasm.data.GameElement
import com.github.adamyork.sparrow.wasm.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.data.GameElementState

interface Enemy : GameElement {

    val type: EnemyType
    val originX: Int
    val originY: Int
    val enemyPosition: EnemyPosition
    val colliding: GameElementCollisionState
    val interacting: EnemyInteractionState

    fun getNextPosition(): EnemyPosition

    fun getNextEnemyState(player: Player): GameElementState

    fun getNextCollisionMetadataWithState(
        animatingFrames: HashMap<Int, FrameMetadata>,
        collisionFrames: HashMap<Int, FrameMetadata>,
    ): Pair<FrameMetadata, FrameMetadataState> {
        var metadata = animatingFrames[1] ?: throw AnimationFrameException(animatingFrames.toString(), 1)
        var metadataState = FrameMetadataState(this.colliding, this.interacting, state)
        if (colliding == GameElementCollisionState.COLLIDING) {
            if (frameMetadata.frame == ANIMATION_COLLISION_FRAMES) {
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
