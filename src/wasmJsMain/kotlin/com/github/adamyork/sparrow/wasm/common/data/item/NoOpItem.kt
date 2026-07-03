package com.github.adamyork.sparrow.wasm.common.data.item

import com.github.adamyork.sparrow.wasm.common.data.Direction
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadata
import com.github.adamyork.sparrow.wasm.common.data.FrameMetadataState
import com.github.adamyork.sparrow.wasm.common.data.GameElementState
import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes

class NoOpItem : Item {
    override val type: ItemType
        get() = TODO("Not yet implemented")
    override val id: Int
        get() = TODO("Not yet implemented")

    override fun getFirstDeactivatingFrame(): FrameMetadata {
        TODO("Not yet implemented")
    }

    override val x: Int
        get() = TODO("Not yet implemented")
    override val y: Int
        get() = TODO("Not yet implemented")
    override val height: Int
        get() = TODO("Not yet implemented")
    override val width: Int
        get() = TODO("Not yet implemented")
    override val state: GameElementState
        get() = TODO("Not yet implemented")
    override val frameMetadata: FrameMetadata
        get() = TODO("Not yet implemented")
    override val imageAndBytes: ImageAndBytes
        get() = TODO("Not yet implemented")

    override fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState> {
        TODO("Not yet implemented")
    }

    override fun nestedDirection(): Direction {
        TODO("Not yet implemented")
    }
}