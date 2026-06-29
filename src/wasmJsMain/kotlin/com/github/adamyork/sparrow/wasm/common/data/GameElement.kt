package com.github.adamyork.sparrow.wasm.common.data

import com.github.adamyork.sparrow.wasm.service.data.ImageAndBytes

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
interface GameElement {

    val x: Int
    val y: Int
    val height: Int
    val width: Int
    val state: GameElementState
    val frameMetadata: FrameMetadata
    val imageAndBytes: ImageAndBytes

    fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState>

    fun nestedDirection(): Direction

}
