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
import kotlinx.browser.window

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
    val immunityTicks: Int = 0,
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
        var metadataState = FrameMetadataState(
            GameElementCollisionState.FREE,
            EnemyInteractionState.ISOLATED,
            state
        )

        // Keep simulation unbounded, but only advance animation frames at ~12 FPS.
        val nowMs = window.performance.now()
        if (lastAnimationTickTimeMs <= 0.0) {
            lastAnimationTickTimeMs = nowMs
            return Pair(frameMetadata, metadataState)
        }

        val elapsedMs = (nowMs - lastAnimationTickTimeMs).coerceAtLeast(0.0)
        lastAnimationTickTimeMs = nowMs
        animationTickBufferMs += elapsedMs
        animationTickCounter += 1

        // If we are colliding, we prioritize collision frames regardless of the normal buffer
        // to ensure the animation starts immediately on impact.
        if (colliding == GameElementCollisionState.COLLIDING) {
            // Reset to frame 1 if we were not previously on a collision frame
            val currentFrame = if (frameMetadata.frame > ANIMATION_COLLISION_FRAMES || frameMetadata.frame < 1) 1 else frameMetadata.frame

            if (animationTickBufferMs < animationFrameIntervalMs) {
                return Pair(collisionFrames[currentFrame] ?: collisionFrames[1]!!, metadataState)
            }

            // Advance frame
            animationTickBufferMs -= animationFrameIntervalMs
            val nextFrame = currentFrame + 1

            return if (nextFrame > ANIMATION_COLLISION_FRAMES) {
                // Animation finished: return to base frame and rely on state engine to update colliding status
                Pair(movingFrames[1]!!, metadataState)
            } else {
                Pair(collisionFrames[nextFrame] ?: collisionFrames[1]!!, metadataState)
            }
        }

        // Normal non-colliding flow
        if (animationTickBufferMs < animationFrameIntervalMs) {
            return Pair(frameMetadata, metadataState)
        }
        animationTickBufferMs -= animationFrameIntervalMs
        animationTickCounter = 0

        // Jump State
        if (jumping == PlayerJumpingState.INITIAL || jumping == PlayerJumpingState.RISING) {
            val nextFrame = if (frameMetadata.frame >= ANIMATION_JUMPING_FRAMES) 1 else frameMetadata.frame + 1
            val metadata = jumpingFrames[nextFrame] ?: jumpingFrames[1]!!
            return Pair(metadata, metadataState)
        }

        // Moving State
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
}
