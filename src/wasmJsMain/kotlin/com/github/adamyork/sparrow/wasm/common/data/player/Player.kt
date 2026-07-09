package com.github.adamyork.sparrow.wasm.common.data.player

import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import kotlinx.browser.window

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class Player(
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int,
    override var state: GameElementState,
    override var frameMetadata: FrameMetadata,
    override val imageAndBytes: ImageAndBytes,
    var vx: Double,
    var vy: Double,
    var jumping: PlayerJumpingState,
    var moving: PlayerMovingState,
    var direction: Direction,
    var colliding: GameElementCollisionState,
    var immunityTicks: Int = 0,
    val animationTargetFps: Double = 12.0,
    var animationTickCounter: Int = 0,
    var lastAnimationTickTimeMs: Double = 0.0,
    var animationTickBufferMs: Double = 0.0,
) : GameElement {

    companion object {
        //val LOGGER: Logger = LoggerFactory.getLogger(Player::class.java)
        const val ANIMATION_MOVING_FRAMES = 4
        const val ANIMATION_JUMPING_FRAMES = 8
        const val ANIMATION_COLLISION_FRAMES = 8
        const val IMMUNITY_TICKS_ON_HIT = 120
    }

    private val animationFrameIntervalMs: Double
        get() = 1000.0 / animationTargetFps.coerceAtLeast(1.0)

    var movingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var jumpingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        val metadataState = FrameMetadataState(
            GameElementCollisionState.FREE,
            EnemyInteractionState.ISOLATED,
            state
        )
        val nowMs = window.performance.now()
        if (lastAnimationTickTimeMs <= 0.0) {
            lastAnimationTickTimeMs = nowMs
            return Pair(frameMetadata, metadataState)
        }
        val elapsedMs = (nowMs - lastAnimationTickTimeMs).coerceAtLeast(0.0)
        lastAnimationTickTimeMs = nowMs
        animationTickBufferMs += elapsedMs
        animationTickCounter += 1
        if (colliding == GameElementCollisionState.COLLIDING) {
            val currentFrame = frameMetadata.frame.coerceIn(1, ANIMATION_COLLISION_FRAMES)
            if (animationTickBufferMs < animationFrameIntervalMs) {
                return Pair(collisionFrames[currentFrame] ?: collisionFrames[1]!!, metadataState)
            }
            animationTickBufferMs -= animationFrameIntervalMs
            val nextFrame = currentFrame + 1
            return if (nextFrame > ANIMATION_COLLISION_FRAMES) {
                Pair(movingFrames[1]!!, metadataState)
            } else {
                Pair(collisionFrames[nextFrame] ?: collisionFrames[1]!!, metadataState)
            }
        }
        if (animationTickBufferMs < animationFrameIntervalMs) {
            return Pair(frameMetadata, metadataState)
        }
        animationTickBufferMs -= animationFrameIntervalMs
        animationTickCounter = 0
        if (jumping == PlayerJumpingState.INITIAL || jumping == PlayerJumpingState.RISING) {
            val nextFrame = if (frameMetadata.frame >= ANIMATION_JUMPING_FRAMES) 1 else frameMetadata.frame + 1
            val metadata = jumpingFrames[nextFrame] ?: jumpingFrames[1]!!
            return Pair(metadata, metadataState)
        }
        if (moving == PlayerMovingState.MOVING) {
            val nextFrame = if (frameMetadata.frame >= ANIMATION_MOVING_FRAMES) 1 else frameMetadata.frame + 1
            val metadata = movingFrames[nextFrame] ?: movingFrames[1]!!
            return Pair(metadata, metadataState)
        }
        return Pair(movingFrames[1]!!, metadataState)
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

    fun getNextJumpState(
        nextY: Int,
        topBoundary: Int,
        bottomBoundary: Int,
        jumpDistance: Double
    ): PlayerJumpingState {
        val reachedCeiling = nextY <= topBoundary
        val reachedJumpApex = jumping == PlayerJumpingState.RISING && nextY <= (bottomBoundary - jumpDistance)
        val hitGround = nextY >= bottomBoundary
        val isDescending = nextY > y
        if (hitGround) return PlayerJumpingState.GROUNDED
        if (reachedCeiling || reachedJumpApex) return PlayerJumpingState.HEIGHT_REACHED
        return when (jumping) {
            PlayerJumpingState.INITIAL -> PlayerJumpingState.RISING
            PlayerJumpingState.HEIGHT_REACHED,
            PlayerJumpingState.GROUNDED,
            PlayerJumpingState.FALLING -> if (isDescending) PlayerJumpingState.FALLING else jumping
            PlayerJumpingState.RISING -> PlayerJumpingState.RISING
        }
    }

    fun getNextCollidingState(velocityX: Double, nextImmunityTicks: Int): GameElementCollisionState {
        val isCurrentlyColliding = colliding == GameElementCollisionState.COLLIDING
        return if (isCurrentlyColliding && velocityX == 0.0 && nextImmunityTicks <= 0) {
            GameElementCollisionState.FREE
        } else {
            colliding
        }
    }
}
