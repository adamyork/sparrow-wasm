package com.github.adamyork.sparrow.wasm.common.data.item

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.github.adamyork.sparrow.wasm.common.data.*
import com.github.adamyork.sparrow.wasm.common.data.enemy.EnemyInteractionState
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes

class NoOpItem : Item {
    override val type: ItemType = ItemType.FINISH
    override val id: Int = -1
    override var frameMetadata: FrameMetadata = FrameMetadata(0, Cell(0, 0, 0, 0))
    override var x: Int = 0
    override var y: Int = 0
    override val height: Int = 0
    override val width: Int = 0
    override var state: GameElementState = GameElementState.INACTIVE
    override val imageAndBytes: ImageAndBytes =
        ImageAndBytes(byteArrayOf(), EmptyImage.createEmptyImage().toComposeImageBitmap())

    override fun getFirstDeactivatingFrame(): FrameMetadata = frameMetadata
    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        return Pair(
            frameMetadata, FrameMetadataState(
                GameElementCollisionState.FREE, EnemyInteractionState.ISOLATED,
                GameElementState.INACTIVE
            )
        )
    }

    override fun nestedDirection(): Direction = Direction.RIGHT
}