package com.github.adamyork.sparrow.wasm.common.data

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.platform.common.data.Direction
import com.github.adamyork.sparrow.platform.common.data.ElementState
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface GameElement {

    var x: Int
    var y: Int
    val height: Int
    val width: Int
    var state: ElementState
    var frameMetadata: FrameMetadata
    val imageAndBytes: ImageAndBytes

    fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState>

    fun nestedDirection(): Direction

    fun cullingCheck(viewPort: ViewPort): Boolean {
        return (x + width > viewPort.x) &&
                (x < viewPort.x + viewPort.width) &&
                (y + height > viewPort.y) &&
                (y < viewPort.y + viewPort.height)
    }

    fun toRect() = Rect(
        x.toFloat(),
        y.toFloat(),
        (x + width).toFloat(),
        (y + height).toFloat()
    )

}
