package com.github.adamyork.sparrow.platform.common.data.item

import com.github.adamyork.sparrow.platform.common.AnimationFrameException
import com.github.adamyork.sparrow.platform.common.data.*
import com.github.adamyork.sparrow.platform.common.data.enemy.EnemyInteractionState

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface Item : GameElement {

    val type: ItemType
    val id: Int

    fun getFirstDeactivatingFrame(): FrameMetadata

    fun getNextActiveMetadataWithState(
        activeFrames: HashMap<Int, FrameMetadata>,
        numActiveFrames: Int
    ): Pair<FrameMetadata, FrameMetadataState> {
        var metadata = activeFrames[1] ?: throw AnimationFrameException(activeFrames.toString(), 1)
        val metadataState =
            FrameMetadataState(
                GameElementCollisionState.FREE,
                EnemyInteractionState.ISOLATED,
                state
            )
        if (state == ElementState.ACTIVE) {
            if (frameMetadata.frame == numActiveFrames) {
                return Pair(metadata, metadataState)
            } else {
                val nextFrame = frameMetadata.frame + 1
                metadata = activeFrames[nextFrame] ?: throw AnimationFrameException(activeFrames.toString(), nextFrame)
                return Pair(metadata, metadataState)
            }
        }
        return Pair(metadata, metadataState)
    }

}
