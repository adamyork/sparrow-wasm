package com.github.adamyork.sparrow.platform.common.data.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.github.adamyork.sparrow.platform.common.PlatformInterop
import com.github.adamyork.sparrow.platform.common.ThrottledAnimator
import com.github.adamyork.sparrow.platform.common.data.*
import com.github.adamyork.sparrow.platform.common.data.enemy.EnemyInteractionState
import com.github.adamyork.sparrow.platform.gui.UiController
import com.github.adamyork.sparrow.platform.service.RuntimeService
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
class Player(
    override var x: Int,
    override var y: Int,
    override val width: Int,
    override val height: Int,
    state: ElementState,
    override var frameMetadata: FrameMetadata,
    override val imageAndBytes: ImageAndBytes,
    var vx: Double,
    var vy: Double,
    jumping: PlayerJumpingState,
    var moving: PlayerMovingState,
    var direction: Direction,
    var colliding: GameElementCollisionState,
    override val platformInterop: PlatformInterop,
    var immunityTicks: Int = 0,
    override val animationTargetFps: Double = 12.0,
    override var animationTickCounter: Int = 0,
    override var lastAnimationTickTimeMs: Double = 0.0,
    override var animationTickBufferMs: Double = 0.0,
) : GameElement, ThrottledAnimator {

    private val logger = KotlinLogging.logger {}

    companion object {
        const val ANIMATION_MOVING_FRAMES = 4
        const val ANIMATION_JUMPING_FRAMES = 8
        const val ANIMATION_COLLISION_FRAMES = 8
        const val IMMUNITY_TICKS_ON_HIT = 120
        private val EMPTY_PLATFORM_INTEROP = object : PlatformInterop {
            override fun onReady(action: () -> Unit) = Unit

            override fun getWindowHeight(): Double = 0.0

            override fun getWindowWidth(): Double = 0.0

            override fun hidePlatformLoader() = Unit

            override fun getPlatformNowTime(): Double = 0.0

            override fun getBlobFromBytes(bytes: ByteArray): Any {
                throw UnsupportedOperationException("Blob interop is not available for empty player")
            }

            override fun createAudioBlobUri(blob: Any): String {
                throw UnsupportedOperationException("Audio URI interop is not available for empty player")
            }

            override fun isTouchDevice(): Boolean {
                throw UnsupportedOperationException("Audio URI interop is not available for empty player")
            }


            override fun <T> addEventListener(type: String, callback: (T) -> Unit) {
                throw UnsupportedOperationException("Audio URI interop is not available for empty player")
            }

            override fun <T> removeEventListener(type: String, callback: (T) -> Unit) {
                throw UnsupportedOperationException("Audio URI interop is not available for empty player")
            }

            override fun requestAnimationFrame(callback: (Double) -> Unit): Int {
                throw UnsupportedOperationException("Audio URI interop is not available for empty player")
            }

            override fun cancelAnimationFrame(handle: Int) {
                throw UnsupportedOperationException("Audio URI interop is not available for empty player")
            }

            @Composable
            override fun InsertInputHandlers(
                controller: UiController,
                runtimeService: RuntimeService
            ) {
                throw UnsupportedOperationException("Audio URI interop is not available for empty player")
            }
        }
        val emptyPlayer: Player = Player(
            x = 0,
            y = 0,
            width = 1,
            height = 1,
            state = ElementState.INACTIVE,
            frameMetadata = FrameMetadata(1, Cell(1, 1, 1, 1)),
            imageAndBytes = ImageAndBytes(byteArrayOf(), ImageBitmap(1, 1)),
            vx = 0.0,
            vy = 0.0,
            jumping = PlayerJumpingState.GROUNDED,
            moving = PlayerMovingState.STATIONARY,
            direction = Direction.RIGHT,
            colliding = GameElementCollisionState.FREE,
            platformInterop = EMPTY_PLATFORM_INTEROP,
            immunityTicks = 0,
            animationTargetFps = 12.0,
            animationTickCounter = 0,
            lastAnimationTickTimeMs = 0.0,
            animationTickBufferMs = 0.0
        )
    }

    var movingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var jumpingFrames: HashMap<Int, FrameMetadata> = HashMap()
    var collisionFrames: HashMap<Int, FrameMetadata> = HashMap()

    override var state: ElementState = state
        set(value) {
            logStateChange(field, value)
            field = value
        }

    var jumping: PlayerJumpingState = jumping
        set(value) {
            if (field != value) {
                logger.debug { "Player jumping changed: $field -> $value" }
            }
            field = value
        }

    init {
        generateAnimationFrameIndex()
    }

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        val metadataState = FrameMetadataState(
            GameElementCollisionState.FREE,
            EnemyInteractionState.ISOLATED,
            state
        )
        if (!shouldAdvanceAnimationFrame()) {
            if (colliding == GameElementCollisionState.COLLIDING) {
                val currentFrame = frameMetadata.frame.coerceIn(1, ANIMATION_COLLISION_FRAMES)
                return Pair(collisionFrames[currentFrame] ?: collisionFrames[1]!!, metadataState)
            }
            return Pair(frameMetadata, metadataState)
        }
        if (colliding == GameElementCollisionState.COLLIDING) {
            val currentFrame = frameMetadata.frame.coerceIn(1, ANIMATION_COLLISION_FRAMES)
            val nextFrame = currentFrame + 1
            return if (nextFrame > ANIMATION_COLLISION_FRAMES) {
                Pair(movingFrames[1]!!, metadataState)
            } else {
                Pair(collisionFrames[nextFrame] ?: collisionFrames[1]!!, metadataState)
            }
        }
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
