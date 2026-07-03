package com.github.adamyork.sparrow.wasm.common.data.item

import com.github.adamyork.sparrow.wasm.common.AnimationFrameException
import com.github.adamyork.sparrow.wasm.common.ThrottleAnimator
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class FinishItem(
    override val width: Int,
    override val height: Int,
    override val x: Int,
    override val y: Int,
    override val type: ItemType,
    override val state: GameElementState,
    override val imageAndBytes: ImageAndBytes,
    override val frameMetadata: FrameMetadata,
    override val id: Int,
    override val animationTargetFps: Double = 12.0,
    override var animationTickCounter: Int = 0,
    override var lastAnimationTickTimeMs: Double = 0.0,
    override var animationTickBufferMs: Double = 0.0,
) : Item, ThrottleAnimator {

    companion object {
        //val LOGGER: Logger = LoggerFactory.getLogger(FinishItem::class.java)
        const val ANIMATION_DEACTIVATING_FRAMES = 1
        const val ANIMATION_ACTIVE_FRAMES = 1
    }

    var activeFrames: HashMap<Int, FrameMetadata> = HashMap()
    var deactivatingFrames: HashMap<Int, FrameMetadata> = HashMap()

    init {
        generateAnimationFrameIndex()
    }

    override fun getFirstDeactivatingFrame(): FrameMetadata {
        TODO("Not yet implemented")
    }

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        var metadata = activeFrames[1] ?: throw AnimationFrameException(activeFrames.toString(), 1)
        val metadataState =
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

    override fun nestedDirection(): Direction {
        return Direction.LEFT
    }

    private fun generateAnimationFrameIndex() {
        activeFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))
        deactivatingFrames[1] = FrameMetadata(1, Cell(1, 1, width, height))
    }

}
