package com.github.adamyork.sparrow.wasm.common.data.player

import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.common.AnimationFrameException
import com.github.adamyork.sparrow.wasm.common.data.Cell
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadata
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadataState
import com.github.adamyork.sparrow.wasm.common.data.GameElement
import com.github.adamyork.sparrow.wasm.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.GameElementState
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class Player(
    override val x: Int,
    override val y: Int,
    override val width: Int,
    override val height: Int,
    override val state: GameElementState,
    override val frameMetadata: FrameMetadata,
    override val imageAndBytes: ImageAndBytes,
    var vx: Double,
    val vy: Double,
    val jumping: PlayerJumpingState,
    val moving: PlayerMovingState,
    val direction: Direction,
    val colliding: GameElementCollisionState,
) : GameElement {

    companion object {
        //val LOGGER: Logger = LoggerFactory.getLogger(Player::class.java)
        const val ANIMATION_MOVING_FRAMES = 4
        const val ANIMATION_JUMPING_FRAMES = 8
        const val ANIMATION_COLLISION_FRAMES = 8
    }

    var movingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var jumpingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        var metadata = movingFrames[1] ?: throw AnimationFrameException(movingFrames.toString(), 1)
        var metadataState = FrameMetadataState(
            GameElementCollisionState.FREE,
            EnemyInteractionState.ISOLATED,
            state
        )
        if (colliding == GameElementCollisionState.COLLIDING) {
            if (frameMetadata.frame >= ANIMATION_COLLISION_FRAMES) {
                metadataState = metadataState.copy(colliding = GameElementCollisionState.FREE)
                metadata = movingFrames[1] ?: throw AnimationFrameException(movingFrames.toString(), 1)
                return Pair(metadata, metadataState)
            } else {
                val nextFrame = frameMetadata.frame + 1
                metadata = collisionFrames[nextFrame] ?: throw AnimationFrameException(collisionFrames.toString(), 1)
                return Pair(metadata, metadataState)
            }
        }
        if (jumping == PlayerJumpingState.INITIAL || jumping == PlayerJumpingState.RISING || jumping == PlayerJumpingState.HEIGHT_REACHED) {
            if (frameMetadata.frame >= ANIMATION_JUMPING_FRAMES) {
                metadata = jumpingFrames[1] ?: throw AnimationFrameException(jumpingFrames.toString(), 1)
                return Pair(metadata, metadataState)
            } else {
                val nextFrame = frameMetadata.frame + 1
                metadata =
                    jumpingFrames[nextFrame] ?: throw AnimationFrameException(jumpingFrames.toString(), nextFrame)
                return Pair(metadata, metadataState)
            }
        }
        if (moving == PlayerMovingState.MOVING) {
            if (frameMetadata.frame >= ANIMATION_MOVING_FRAMES) {
                metadata = movingFrames[1] ?: throw AnimationFrameException(movingFrames.toString(), 1)
                return Pair(metadata, metadataState)
            } else {
                val nextFrame = frameMetadata.frame + 1
                metadata = movingFrames[nextFrame] ?: throw AnimationFrameException(movingFrames.toString(), nextFrame)
                return Pair(metadata, metadataState)
            }
        }
        return Pair(metadata, metadataState)
    }

    private fun generateAnimationFrameIndex() {
        movingFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))
        movingFrames[2] = FrameMetadata(2, Cell(1, 1, width, height))
        movingFrames[3] = FrameMetadata(3, Cell(1, 2, width, height))
        movingFrames[4] = FrameMetadata(4, Cell(1, 2, width, height))

        jumpingFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))
        jumpingFrames[2] = FrameMetadata(2, Cell(1, 2, width, height))
        jumpingFrames[3] = FrameMetadata(3, Cell(1, 3, width, height))
        jumpingFrames[4] = FrameMetadata(3, Cell(1, 3, width, height))
        jumpingFrames[5] = FrameMetadata(3, Cell(1, 3, width, height))
        jumpingFrames[6] = FrameMetadata(3, Cell(1, 3, width, height))
        jumpingFrames[7] = FrameMetadata(3, Cell(1, 3, width, height))
        jumpingFrames[8] = FrameMetadata(3, Cell(1, 3, width, height))

        collisionFrames[1] = FrameMetadata(1, Cell(1, 4, width, height))
        collisionFrames[2] = FrameMetadata(2, Cell(1, 4, width, height))
        collisionFrames[3] = FrameMetadata(3, Cell(1, 3, width, height))
        collisionFrames[4] = FrameMetadata(4, Cell(1, 3, width, height))
        collisionFrames[5] = FrameMetadata(5, Cell(1, 4, width, height))
        collisionFrames[6] = FrameMetadata(6, Cell(1, 4, width, height))
        collisionFrames[7] = FrameMetadata(7, Cell(1, 3, width, height))
        collisionFrames[8] = FrameMetadata(8, Cell(1, 3, width, height))
    }

    override fun nestedDirection(): Direction {
        return this.direction
    }
}
