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
data class BlockerEnemy(
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
        //val LOGGER: Logger = LoggerFactory.getLogger(BlockerEnemy::class.java)
        const val ANIMATION_COLLISION_FRAMES = 8
        const val MAX_X_MOVEMENT = 100
        const val MOVEMENT_X_DISTANCE = 4
    }

    private val animationFrameIntervalMs: Double
        get() = 1000.0 / animationTargetFps.coerceAtLeast(1.0)

    var animatingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun nestedDirection(): Direction {
        return this.enemyPosition.direction
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

    override fun getNextPosition(): EnemyPosition {
        return getNextPosition(1.0)
    }

    fun getNextPosition(deltaTimeCoefficient: Double): EnemyPosition {
        val leftBound = originX - MAX_X_MOVEMENT
        val rightBound = originX + MAX_X_MOVEMENT
        val scaledMovement = (MOVEMENT_X_DISTANCE * deltaTimeCoefficient.coerceAtLeast(0.0)) + movementCarryX
        val movementStep = floor(scaledMovement).toInt()
        movementCarryX = scaledMovement - movementStep

        if (movementStep <= 0) {
            return if (enemyPosition.direction == Direction.LEFT && enemyPosition.x <= leftBound) {
                EnemyPosition(enemyPosition.x, enemyPosition.y, Direction.RIGHT)
            } else if (enemyPosition.direction == Direction.RIGHT && enemyPosition.x >= rightBound) {
                EnemyPosition(enemyPosition.x, enemyPosition.y, Direction.LEFT)
            } else {
                EnemyPosition(enemyPosition.x, enemyPosition.y, enemyPosition.direction)
            }
        }

        if (enemyPosition.direction == Direction.LEFT) {
            if (enemyPosition.x > leftBound) {
                val nextX = (enemyPosition.x - movementStep).coerceAtLeast(leftBound)
                val nextDirection = if (nextX <= leftBound) Direction.RIGHT else Direction.LEFT
                return EnemyPosition(nextX, enemyPosition.y, nextDirection)
            }
            return EnemyPosition(enemyPosition.x, enemyPosition.y, Direction.RIGHT)
        }

        if (enemyPosition.x < rightBound) {
            val nextX = (enemyPosition.x + movementStep).coerceAtMost(rightBound)
            val nextDirection = if (nextX >= rightBound) Direction.LEFT else Direction.RIGHT
            return EnemyPosition(nextX, enemyPosition.y, nextDirection)
        }
        return EnemyPosition(enemyPosition.x, enemyPosition.y, Direction.LEFT)
    }

    override fun getNextEnemyState(player: Player): GameElementState {
        return this.state
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

}
