package com.github.adamyork.sparrow.platform.common.data.enemy

import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.AnimationFrameException
import com.github.adamyork.sparrow.platform.common.ThrottledAnimator
import com.github.adamyork.sparrow.platform.common.data.Cell
import com.github.adamyork.sparrow.platform.common.data.Direction
import com.github.adamyork.sparrow.platform.common.data.ElementState
import com.github.adamyork.sparrow.platform.common.data.FrameMetadata
import com.github.adamyork.sparrow.platform.common.data.FrameMetadataState
import com.github.adamyork.sparrow.platform.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.platform.common.data.player.Player
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class ShooterEnemy(
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int,
    state: ElementState,
    override var frameMetadata: FrameMetadata,
    override val imageAndBytes: ImageAndBytes,
    override val id: Int,
    override val type: EnemyType,
    override val originX: Int,
    override val originY: Int,
    override var enemyPosition: EnemyPosition,
    override var colliding: GameElementCollisionState,
    override var interacting: EnemyInteractionState,
    override val platformInterop: PlatformInterop,
    override val animationTargetFps: Double = 12.0,
    override var animationTickCounter: Int = 0,
    override var lastAnimationTickTimeMs: Double = 0.0,
    override var animationTickBufferMs: Double = 0.0,
) : Enemy, ThrottledAnimator {

    companion object {
        const val PLAYER_PROXIMITY_THRESHOLD = 200
        const val ANIMATION_INTERACTING_FRAMES = 8
    }

    var animatingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()
    var interactingFrames: HashMap<Int, FrameMetadata> = HashMap()

    override var state: ElementState = state
        set(value) {
            logStateChange(field, value)
            field = value
        }

    init {
        generateAnimationFrameIndex()
    }

    override fun getNextEnemyState(player: Player): ElementState {
        return this.state
    }

    @Suppress("DuplicatedCode")
    private fun generateAnimationFrameIndex() {
        animatingFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))

        interactingFrames[1] = FrameMetadata(1, Cell(1, 2, width, height))
        interactingFrames[2] = FrameMetadata(2, Cell(1, 2, width, height))
        interactingFrames[3] = FrameMetadata(3, Cell(1, 2, width, height))
        interactingFrames[4] = FrameMetadata(4, Cell(1, 2, width, height))
        interactingFrames[5] = FrameMetadata(5, Cell(1, 2, width, height))
        interactingFrames[6] = FrameMetadata(6, Cell(1, 2, width, height))
        interactingFrames[7] = FrameMetadata(7, Cell(1, 2, width, height))
        interactingFrames[8] = FrameMetadata(8, Cell(1, 2, width, height))

        collisionFrames[1] = FrameMetadata(1, Cell(1, 2, width, height))
        collisionFrames[2] = FrameMetadata(2, Cell(1, 2, width, height))
        collisionFrames[3] = FrameMetadata(3, Cell(1, 1, width, height))
        collisionFrames[4] = FrameMetadata(4, Cell(1, 1, width, height))
        collisionFrames[5] = FrameMetadata(5, Cell(1, 2, width, height))
        collisionFrames[6] = FrameMetadata(6, Cell(1, 2, width, height))
        collisionFrames[7] = FrameMetadata(7, Cell(1, 1, width, height))
        collisionFrames[8] = FrameMetadata(8, Cell(1, 1, width, height))
    }

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        if (!shouldAdvanceAnimationFrame()) {
            return Pair(frameMetadata, FrameMetadataState(this.colliding, this.interacting, state))
        }
        var metadata = animatingFrames[1] ?: throw AnimationFrameException(animatingFrames.toString(), 1)
        var metadataState = FrameMetadataState(this.colliding, this.interacting, state)
        if (this.interacting == EnemyInteractionState.INTERACTING) {
            if (frameMetadata.frame == ANIMATION_INTERACTING_FRAMES) {
                metadataState = metadataState.copy(interacting = EnemyInteractionState.ISOLATED)
                return Pair(metadata, metadataState)
            } else {
                val nextFrame = frameMetadata.frame + 1
                metadata = interactingFrames[nextFrame] ?: throw AnimationFrameException(
                    interactingFrames.toString(),
                    nextFrame
                )
                return Pair(metadata, metadataState)
            }
        }
        return this.getNextCollisionMetadataWithState(animatingFrames, collisionFrames)
    }

    override fun nestedDirection(): Direction {
        return this.enemyPosition.direction
    }

    override fun getNextPosition(): EnemyPosition {
        return EnemyPosition(
            this.x,
            this.y,
            this.nestedDirection()
        )
    }
}
