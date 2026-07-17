package com.github.adamyork.sparrow.wasm.common.data.enemy

import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.wasm.common.ThrottledAnimator
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import kotlin.math.floor

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class RunnerEnemy(
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int,
    override var state: ElementState,
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
    var movementCarryX: Double = 0.0,
) : Enemy, ThrottledAnimator {

    companion object {
        const val MOVEMENT_X_DISTANCE = 10
        const val PLAYER_PROXIMITY_THRESHOLD = 200
    }

    var animatingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun getNextEnemyState(player: Player): ElementState {
        val withinXRange = player.x >= this.originX - PLAYER_PROXIMITY_THRESHOLD
        val withinYRange = player.y == this.originY - this.height
        val notAtEndOfPath = this.x > 0
        return if (withinXRange && withinYRange && notAtEndOfPath) {
            ElementState.ACTIVE
        } else if (this.x == 0) {
            ElementState.INACTIVE
        } else {
            this.state
        }
    }

    @Suppress("DuplicatedCode")
    private fun generateAnimationFrameIndex() {
        animatingFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))

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
        return this.getNextCollisionMetadataWithState(animatingFrames, collisionFrames)
    }

    override fun nestedDirection(): Direction {
        return this.enemyPosition.direction
    }

    override fun getNextPosition(): EnemyPosition {
        return getNextPosition(1.0)
    }

    fun getNextPosition(deltaTimeCoefficient: Double): EnemyPosition {
        val scaledMovement = (MOVEMENT_X_DISTANCE * deltaTimeCoefficient.coerceAtLeast(0.0)) + movementCarryX
        val movementStep = floor(scaledMovement).toInt()
        movementCarryX = scaledMovement - movementStep

        if (movementStep <= 0) {
            return EnemyPosition(enemyPosition.x, enemyPosition.y, Direction.LEFT)
        }

        val nextX = (enemyPosition.x - movementStep).coerceAtLeast(0)
        return EnemyPosition(nextX, enemyPosition.y, Direction.LEFT)
    }

}
