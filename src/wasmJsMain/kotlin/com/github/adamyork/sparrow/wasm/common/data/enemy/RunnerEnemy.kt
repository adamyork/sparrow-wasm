package com.github.adamyork.sparrow.wasm.common.data.enemy

import com.github.adamyork.sparrow.wasm.common.data.Cell
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadata
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadataState
import com.github.adamyork.sparrow.wasm.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.GameElementState
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import com.github.adamyork.sparrow.wasm.common.data.player.Player
import kotlinx.browser.window
import kotlin.math.floor

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class RunnerEnemy(
    override val x: Int,
    override val y: Int,
    override val width: Int,
    override val height: Int,
    override val state: GameElementState,
    override val frameMetadata: FrameMetadata,
    override val imageAndBytes: ImageAndBytes,
    override val type: EnemyType,
    override val originX: Int,
    override val originY: Int,
    override val enemyPosition: EnemyPosition,
    override val colliding: GameElementCollisionState,
    override val interacting: EnemyInteractionState,
    val animationTargetFps: Double = 12.0,
    var animationTickCounter: Int = 0,
    var lastAnimationTickTimeMs: Double = 0.0,
    var animationTickBufferMs: Double = 0.0,
    var movementCarryX: Double = 0.0,
) : Enemy {

    companion object {
        //val LOGGER: Logger = LoggerFactory.getLogger(ShooterEnemy::class.java)
        const val MOVEMENT_X_DISTANCE = 10
        const val PLAYER_PROXIMITY_THRESHOLD = 200
    }

    private val animationFrameIntervalMs: Double
        get() = 1000.0 / animationTargetFps.coerceAtLeast(1.0)

    var animatingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun getNextEnemyState(player: Player): GameElementState {
        val withinXRange = player.x >= this.originX - PLAYER_PROXIMITY_THRESHOLD
        val withinYRange = player.y == this.originY - this.height
        val notAtEndOfPath = this.x > 0
        return if (withinXRange && withinYRange && notAtEndOfPath) {
            GameElementState.ACTIVE
        } else if (this.x == 0) {
            GameElementState.INACTIVE
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

    private fun shouldAdvanceAnimationFrame(): Boolean {
        val nowMs = window.performance.now()
        if (lastAnimationTickTimeMs <= 0.0) {
            lastAnimationTickTimeMs = nowMs
            return false
        }
        val elapsedMs = (nowMs - lastAnimationTickTimeMs).coerceAtLeast(0.0)
        lastAnimationTickTimeMs = nowMs
        animationTickBufferMs += elapsedMs
        animationTickCounter += 1
        if (animationTickBufferMs < animationFrameIntervalMs) {
            return false
        }
        animationTickBufferMs -= animationFrameIntervalMs
        animationTickCounter = 0
        return true
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
