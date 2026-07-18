package com.github.adamyork.sparrow.platform.engine.data

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.skia.Image

/**
 * Author: Adam York
 * Copyright (c) Adam York
 */
data class DrawResult(
    //TODO interop
    val foregroundImage: Image?,
    val farGroundBitmap: ImageBitmap?,
    val farGroundOffsetX: Float,
    val farGroundOffsetY: Float,
    val midGroundBitmap: ImageBitmap?,
    val midGroundOffsetX: Float,
    val midGroundOffsetY: Float,
    val collisionBitmap: ImageBitmap?,
    val collisionOffsetX: Float,
    val collisionOffsetY: Float,
    val foregroundOffsetX: Float,
    val foregroundOffsetY: Float,
    val nearFieldBitmap: ImageBitmap?,
    val nearFieldOffsetX: Float,
    val nearFieldOffsetY: Float,
) {
    companion object {
        val EMPTY_DRAW_RESULT = DrawResult(
            null,
            null,
            0f,
            0f,
            null,
            0f,
            0f,
            null,
            0f,
            0f,
            0f,
            0f,
            null,
            0f,
            0f
        )
    }
}
