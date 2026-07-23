package com.github.adamyork.sparrow.platform.common.data

import androidx.compose.ui.geometry.Rect
import com.github.adamyork.sparrow.platform.service.data.ImageAndBytes
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface GameElement {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    var x: Int
    var y: Int
    val height: Int
    val width: Int
    var state: ElementState
    var frameMetadata: FrameMetadata
    val imageAndBytes: ImageAndBytes

    fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState>

    fun nestedDirection(): Direction

    fun logStateChange(previousState: ElementState, nextState: ElementState) {
        if (previousState != nextState) {
            logger.debug {
                "GameElement state changed: $previousState -> $nextState at x=$x y=$y"
            }
        }
    }

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
