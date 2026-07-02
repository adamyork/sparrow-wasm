package com.github.adamyork.sparrow.wasm.common.data.item

import com.github.adamyork.sparrow.wasm.common.AnimationFrameException
import com.github.adamyork.sparrow.wasm.common.data.Cell
import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadata
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadataState
import com.github.adamyork.sparrow.wasm.common.data.GameElementCollisionState
import com.github.adamyork.sparrow.wasm.common.data.GameElementState
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes
import kotlinx.browser.window

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class CollectibleItem(
    override val width: Int,
    override val height: Int,
    override val x: Int,
    override val y: Int,
    override val type: ItemType,
    override val state: GameElementState,
    override val imageAndBytes: ImageAndBytes,
    override val frameMetadata: FrameMetadata,
    override val id: Int,
    val animationTargetFps: Double = 12.0,
    var animationTickCounter: Int = 0,
    var lastAnimationTickTimeMs: Double = 0.0,
    var animationTickBufferMs: Double = 0.0,
) : Item {

    companion object {
        //val LOGGER: Logger = LoggerFactory.getLogger(CollectibleItem::class.java)
        const val ANIMATION_DEACTIVATING_FRAMES = 4
        const val ANIMATION_ACTIVE_FRAMES = 16
    }

    private val animationFrameIntervalMs: Double
        get() = 1000.0 / animationTargetFps.coerceAtLeast(1.0)

    var activeFrames: HashMap<Int, FrameMetadata> = HashMap()
    var deactivatingFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        var metadata = activeFrames[1] ?: throw AnimationFrameException(activeFrames.toString(), 1)
        var metadataState =
            FrameMetadataState(
                GameElementCollisionState.FREE,
                EnemyInteractionState.ISOLATED,
                state
            )
        if (!shouldAdvanceAnimationFrame()) {
            return Pair(frameMetadata, metadataState)
        }
        if (state == GameElementState.DEACTIVATING) {
            if (frameMetadata.frame == ANIMATION_DEACTIVATING_FRAMES) {
                //LOGGER.info("deactivating complete 0")
                metadataState =
                    FrameMetadataState(
                        GameElementCollisionState.FREE,
                        EnemyInteractionState.ISOLATED,
                        GameElementState.INACTIVE
                    )
                return Pair(metadata, metadataState)
            } else {
                val nextFrame = frameMetadata.frame + 1
                //LOGGER.info("deactivating frame $nextFrame")
                metadata = deactivatingFrames[nextFrame] ?: throw AnimationFrameException(
                    deactivatingFrames.toString(),
                    nextFrame
                )
                return Pair(metadata, metadataState)
            }
        }
        return getNextActiveMetadataWithState(activeFrames, ANIMATION_ACTIVE_FRAMES)
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
        return Direction.LEFT
    }

    override fun getFirstDeactivatingFrame(): FrameMetadata {
        return deactivatingFrames[1] ?: throw AnimationFrameException(deactivatingFrames.toString(), 1)
    }


    @Suppress("DuplicatedCode")
    private fun generateAnimationFrameIndex() {
        activeFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))
        activeFrames[2] = FrameMetadata(2, Cell(1, 1, width, height))
        activeFrames[3] = FrameMetadata(3, Cell(1, 1, width, height))
        activeFrames[4] = FrameMetadata(4, Cell(1, 1, width, height))
        activeFrames[5] = FrameMetadata(5, Cell(1, 2, width, height))
        activeFrames[6] = FrameMetadata(6, Cell(1, 2, width, height))
        activeFrames[7] = FrameMetadata(7, Cell(1, 2, width, height))
        activeFrames[8] = FrameMetadata(8, Cell(1, 2, width, height))
        activeFrames[9] = FrameMetadata(9, Cell(1, 3, width, height))
        activeFrames[10] = FrameMetadata(10, Cell(1, 3, width, height))
        activeFrames[11] = FrameMetadata(11, Cell(1, 3, width, height))
        activeFrames[12] = FrameMetadata(12, Cell(1, 3, width, height))
        activeFrames[13] = FrameMetadata(13, Cell(1, 4, width, height))
        activeFrames[14] = FrameMetadata(14, Cell(1, 4, width, height))
        activeFrames[15] = FrameMetadata(15, Cell(1, 4, width, height))
        activeFrames[16] = FrameMetadata(16, Cell(1, 4, width, height))

        deactivatingFrames[1] = FrameMetadata(1, Cell(1, 5, width, height))
        deactivatingFrames[2] = FrameMetadata(2, Cell(1, 6, width, height))
        deactivatingFrames[3] = FrameMetadata(3, Cell(1, 7, width, height))
        deactivatingFrames[4] = FrameMetadata(4, Cell(1, 8, width, height))
    }

}
