package com.github.adamyork.sparrow.wasm.data

import com.github.adamyork.sparrow.wasm.CustomImageWrapper

interface GameElement {

    val x: Int
    val y: Int
    val height: Int
    val width: Int
    val state: GameElementState
    val frameMetadata: FrameMetadata
    val customImageWrapper: CustomImageWrapper

    fun getNextFrameMetadataWithState(): Pair<FrameMetadata, FrameMetadataState>

    fun nestedDirection(): Direction

}
